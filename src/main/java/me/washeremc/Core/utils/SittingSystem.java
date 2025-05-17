package me.washeremc.Core.utils;

import me.washeremc.Washere;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class SittingSystem implements Listener {
    private final Washere plugin;
    private final HashMap<UUID, ArmorStand> stairSeats = new HashMap<>();
    private final HashMap<UUID, UUID> playerSeats = new HashMap<>();

    public SittingSystem(Washere plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (stairSeats.containsKey(player.getUniqueId()) || playerSeats.containsKey(player.getUniqueId())) return;

        if (block.getBlockData() instanceof Stairs) {
            event.setCancelled(true);
            sitOnStairs(player, block);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        Player player = event.getPlayer();

        if (stairSeats.containsKey(player.getUniqueId()) || playerSeats.containsKey(player.getUniqueId())) return;

        if (!isPlayerOnGround(targetPlayer)) return;

        sitOnPlayer(player, targetPlayer);
        event.setCancelled(true);
    }

    private boolean isPlayerOnGround(@NotNull Player player) {
        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() - 0.01);
        return loc.getBlock().getType().isSolid();
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        standUp(player);

        new HashMap<>(playerSeats).forEach((sittingPlayerUUID, targetPlayerUUID) -> {
            if (targetPlayerUUID.equals(player.getUniqueId())) {
                Player sittingPlayer = plugin.getServer().getPlayer(sittingPlayerUUID);
                if (sittingPlayer != null) {
                    standUp(sittingPlayer);
                }
            }
        });
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        standUp(event.getEntity());
    }

    private void sitOnStairs(Player player, @NotNull Block stairs) {
        Location seatLocation = stairs.getLocation().add(0.5, 0.3, 0.5);
        ArmorStand seat = createStairSeat(player, seatLocation);
        stairSeats.put(player.getUniqueId(), seat);
    }

    private void sitOnPlayer(Player sitter, @NotNull Player target) {
        if (target.getPassengers().contains(sitter)) return;

        target.getPassengers().forEach(entity -> {
            if (entity instanceof Player) {
                standUp((Player) entity);
            }
        });

        target.addPassenger(sitter);
        playerSeats.put(sitter.getUniqueId(), target.getUniqueId());

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerSneak(org.bukkit.event.player.PlayerToggleSneakEvent e) {
                if (e.getPlayer().getUniqueId().equals(sitter.getUniqueId()) ||
                        e.getPlayer().getUniqueId().equals(target.getUniqueId())) {
                    standUp(sitter);
                }
            }
        }, plugin);
    }

    private @NotNull ArmorStand createStairSeat(Player player, @NotNull Location location) {
        ArmorStand seat = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setSmall(true);
            armorStand.setMarker(true);
        });

        seat.addPassenger(player);

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerSneak(org.bukkit.event.player.PlayerToggleSneakEvent e) {
                if (e.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    standUp(player);
                }
            }
        }, plugin);
        return seat;
    }

    public void standUp(@NotNull Player player) {
        ArmorStand stairSeat = stairSeats.remove(player.getUniqueId());
        if (stairSeat != null) {
            player.leaveVehicle();
            stairSeat.remove();
            teleportToSafeLocation(player);
        }

        UUID targetUUID = playerSeats.remove(player.getUniqueId());
        if (targetUUID != null) {
            player.leaveVehicle();
            teleportToSafeLocation(player);
        }
    }
    private void teleportToSafeLocation(@NotNull Player player) {
        Location safeLoc = player.getLocation();
        safeLoc.setY(player.getLocation().getBlockY());
        player.teleport(safeLoc);
    }
}
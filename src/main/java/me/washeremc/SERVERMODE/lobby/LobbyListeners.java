package me.washeremc.SERVERMODE.lobby;

import me.washeremc.Core.Profile.Profile;
import me.washeremc.Core.utils.GuiItems;
import me.washeremc.Washere;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public class LobbyListeners implements Listener {

    private final Washere plugin;

    public LobbyListeners(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }


    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);
        setLobbyItems(player);

        player.setGameMode(GameMode.ADVENTURE);
        if (player.hasPermission(FlyCommand.FLY_PERMISSION)) {
            FlyCommand.setFlight(player, true);
        }

        if (plugin.getConfig().get("serverSpawn") != null) {
            Location spawn = (Location) plugin.getConfig().get("serverSpawn");
            assert spawn != null;
            player.teleport(spawn);
        }
    }

    @EventHandler
    public void onHeadRightClick(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.PLAYER_HEAD) {
            if (itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()) {
                if (itemInHand.getItemMeta().getDisplayName().equals("§eProfile")) {
                    Profile.openProfile(player);
                }
            }
        }
    }

    @EventHandler
    public void onCompassRightClick(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.COMPASS) {
            if (itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()) {
                if (itemInHand.getItemMeta().getDisplayName().equals("§eServers")) {
                    ServerTeleport.openServerTeleport(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerHit(@NotNull EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);

        }
    }

    @EventHandler
    public void OnInteract(@NotNull PlayerInteractEvent event) {
        if (event.getPlayer().hasPermission("staff.block")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof ArmorStand || e.getEntity() instanceof ItemFrame) {
            if (e.getDamager().hasPermission("staff.block")) {
                return;
            }
            e.setCancelled(true);
        }
        e.setCancelled(true);
    }


    @EventHandler
    public void onHangingBreakByEntityEvent(@NotNull HangingBreakByEntityEvent e) {
        if (e.getRemover().hasPermission("staff.block")) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        if (plugin.getConfig().get("serverSpawn") != null){
            e.deathMessage(null);
            Player p = e.getEntity();
            Location spawn = (Location)plugin.getConfig().get("serverSpawn");
            assert spawn != null;
            p.teleport(spawn);
        }
    }

    @EventHandler
    public void onItemDrop(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("staff.block")) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("staff.block")) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("staff.block")) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    @EventHandler
    public void onFoodChange(@NotNull FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }


    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageEvent e) {
        e.setCancelled(true);
    }


    @EventHandler
    public void onParkour(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location under = player.getLocation().clone();
        under.setY(under.getY() - 1);

        // Get Y-velocity from config
        double yVelocity = plugin.getConfig().getDouble("jump-velocity", 0.9); // Default = 0.9

        if (player.getLocation().getBlock().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                && under.getBlock().getType() == Material.EMERALD_BLOCK) {

            Vector direction = player.getLocation().getDirection().setY(0).normalize();
            direction.multiply(2.0);

            player.setVelocity(new Vector(direction.getX(), yVelocity, direction.getZ()));
        }

        // Check if the player fell below the configured Y-level
        double minY = plugin.getConfig().getDouble("fall-limit", -10.0); // Default = -10
        if (player.getLocation().getY() <= minY) {
            if (plugin.getConfig().get("serverSpawn") != null) {
                Location spawn = (Location) plugin.getConfig().get("serverSpawn");
                assert spawn != null;
                player.teleport(spawn);
            }
        }
    }

    private void setLobbyItems(@NotNull Player player) {
        if (!isLobby()) {
            return;
        }
        player.getInventory().clear();
        ItemStack playerHead = GuiItems.createPlayerHead(player, "§eProfile", Arrays.asList(
                "§7Click to open your profile menu!",
                "§eRight-click to view profile."
        ));
        player.getInventory().setItem(0, playerHead);


        ItemStack compass = GuiItems.createItem(Material.COMPASS, "§eServers", List.of(
                "§7Click to teleport between servers."
        ));
        player.getInventory().setItem(8, compass);
    }
}
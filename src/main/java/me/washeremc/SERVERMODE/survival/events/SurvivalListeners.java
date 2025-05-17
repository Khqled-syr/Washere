package me.washeremc.SERVERMODE.survival.events;


import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static me.washeremc.Core.utils.ChatUtils.colorize;


public class SurvivalListeners implements Listener {

    private final Washere plugin;

    public SurvivalListeners(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isSurvival() {
        return "survival".equalsIgnoreCase(plugin.getServerType());
    }

    @EventHandler
    public void OnPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isSurvival()) return;
        event.joinMessage(null);
        broadcastJoinOrLeaveMessage(event.getPlayer(), "join-message");

        if (!player.hasPlayedBefore()) {
            player.sendMessage("");
            player.performCommand("washere:help");
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!isSurvival()) return;

        event.quitMessage(null);
        broadcastJoinOrLeaveMessage(event.getPlayer(), "leave-message");
    }

    private void broadcastJoinOrLeaveMessage(Player player, String configKey) {
        String broadcastMessage = plugin.getConfig().getString(configKey);
        if (broadcastMessage == null) {
            plugin.getLogger().warning(configKey + " is not configured properly.");
            return;
        }
        broadcastMessage = PlaceholderAPI.setPlaceholders(player, broadcastMessage);
        broadcastMessage = broadcastMessage.replace("%player%", player.getName());
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcastMessage));
    }

    @EventHandler
    public void onPlayerBedEnter(@NotNull PlayerBedEnterEvent event) {
        if (!isSurvival()) return;
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Objects.requireNonNull(Bukkit.getWorld("world")).setTime(0);
                Objects.requireNonNull(Bukkit.getWorld("world")).setStorm(false);
                Objects.requireNonNull(Bukkit.getWorld("world")).setThundering(false);
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e" + event.getPlayer().getName() + "§7 has slept. Good morning everyone!"));
            }, 100L);
        }
    }

    @EventHandler
    public void onPlayerHit(@NotNull EntityDamageByEntityEvent event) {
        if (!isSurvival()) return;
        if (event.getDamager() instanceof Player damager) {
            Entity damaged = event.getEntity();

            if (damaged instanceof Player damagedPlayer) {

                double healthAfterDamage = damagedPlayer.getHealth() - event.getDamage();

                if (healthAfterDamage <= 0.5) {
                    event.setCancelled(true);
                    damaged.sendMessage(colorize("&c" + damager.getName() + " tried to kill you!"));
                    damager.sendMessage(colorize("&cYou can't hit them, they're gonna die LOL!"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (!isSurvival()) return;
        Player player = event.getEntity();
        String deathMessage = colorize("&c" + player.getName() + " died at X: " +
                player.getLocation().getBlockX() + " Y: " + player.getLocation().getBlockY() + " Z: " +
                player.getLocation().getBlockZ());

        EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent instanceof EntityDamageByEntityEvent entityDamageEvent) {
            Entity damager = entityDamageEvent.getDamager();
            if (damager instanceof Player) {
                deathMessage += " by " + ((Player) damager).displayName();
            } else if (damager instanceof LivingEntity) {
                deathMessage += " by a " + damager.getType().name().toLowerCase();
            }
        } else if (damageEvent != null) {
            deathMessage += " (" + damageEvent.getCause().name().toLowerCase() + ")";
        }
        event.deathMessage(Component.text(deathMessage));
    }
}
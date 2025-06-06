package me.washeremc.SERVERMODE.survival.events;

import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
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

        String joinMessage = plugin.getConfig().getString("join-message");
        if (joinMessage == null) {
            plugin.getLogger().warning("join-message is not configured properly.");
            return;
        }

        String processedMessage = PlaceholderAPI.setPlaceholders(player, joinMessage);
        event.joinMessage(ChatUtils.colorizeMini(processedMessage));

        if (!player.hasPlayedBefore()) {
            player.sendMessage("");
            player.performCommand("washere:help");
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!isSurvival()) return;

        String leaveMessage = plugin.getConfig().getString("leave-message");
        if (leaveMessage == null) {
            plugin.getLogger().warning("leave-message is not configured properly.");
            return;
        }

        String processedMessage = PlaceholderAPI.setPlaceholders(event.getPlayer(), leaveMessage);
        event.quitMessage(ChatUtils.colorizeMini(processedMessage));
    }

    @EventHandler
    public void onPlayerBedEnter(@NotNull PlayerBedEnterEvent event) {
        if (!isSurvival()) return;
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Objects.requireNonNull(Bukkit.getWorld("world")).setTime(0);
                Objects.requireNonNull(Bukkit.getWorld("world")).setStorm(false);
                Objects.requireNonNull(Bukkit.getWorld("world")).setThundering(false);

                String sleepMessage = "&e" + event.getPlayer().getName() + "&7 has slept. Good morning everyone!";
                Bukkit.broadcast(ChatUtils.colorizeMini(sleepMessage));
                plugin.getLogger().info(ChatUtils.stripColorLegacy(sleepMessage));
            }, 100L);
        }
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (!isSurvival()) return;
        Player player = event.getEntity();
        String deathMessage = "&c" + player.getName() + " died at X: " +
                player.getLocation().getBlockX() + " Y: " + player.getLocation().getBlockY() + " Z: " +
                player.getLocation().getBlockZ();

        EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent instanceof EntityDamageByEntityEvent entityDamageEvent) {
            Entity damager = entityDamageEvent.getDamager();
            if (damager instanceof Player) {
                deathMessage += " by " + ((Player) damager).getName();
            } else if (damager instanceof LivingEntity) {
                deathMessage += " by a " + damager.getType().name().toLowerCase();
            }
        } else if (damageEvent != null) {
            deathMessage += " (" + damageEvent.getCause().name().toLowerCase() + ")";
        }
        event.deathMessage(ChatUtils.colorizeMini(deathMessage));
        plugin.getLogger().info(ChatUtils.stripColorLegacy(deathMessage));
    }
}
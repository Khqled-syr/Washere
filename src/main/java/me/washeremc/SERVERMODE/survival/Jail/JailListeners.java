package me.washeremc.SERVERMODE.survival.Jail;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class JailListeners implements Listener {
    private final JavaPlugin plugin;
    private final JailManager jailManager;

    public JailListeners(JavaPlugin plugin, JailManager jailManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isJailed(player.getUniqueId()) && jailManager.getJailLocation() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.teleport(jailManager.getJailLocation());

                JailManager.JailData jailData = jailManager.getJailData(player.getUniqueId());
                long timeLeft = (jailData.releaseTime() - System.currentTimeMillis()) / 1000;

                player.sendMessage("§cYou are jailed for: " + jailData.reason());
                player.sendMessage("§cTime remaining: " + jailManager.formatTime(timeLeft));
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isJailed(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isJailed(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isJailed(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(@NotNull PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isJailed(player.getUniqueId())) {
            String command = event.getMessage().toLowerCase();
            if (!command.startsWith("/unjail")) {
                event.setCancelled(true);
                player.sendMessage("§cYou are jailed, you can't use commands!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isJailed(player.getUniqueId())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                    event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                if (jailManager.getJailLocation() != null &&
                        event.getTo().distance(jailManager.getJailLocation()) > 10) {
                    player.teleport(jailManager.getJailLocation());
                }
            }
        }
    }
}
package me.washeremc.Core.Listeners;


import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class ServerListeners implements Listener {

    private final Washere plugin;

    public ServerListeners(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        SettingsManager.savePlayerSettings(uuid);

        plugin.getScoreboard().removeSidebar(player);
        plugin.getScoreboard().removePlayerTeams(player);

    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getLogger().info("🔄 Loading settings for " + player.getName() + "...");

        try {
            plugin.getTabList().setTabList(player);
            plugin.getTabList().updatePlayerListNames();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update tablist for " + player.getName() + ": " + e.getMessage());
        }

        SettingsManager.loadPlayerSettingsAsync(uuid).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // Apply settings
                        applyPlayerSettings(player);

                        // Load and apply scoreboard if enabled
                        if (SettingsManager.isScoreboardEnabled(player)) {
                            plugin.getScoreboard().createSidebar(player);
                        }

                        plugin.getScoreboard().setPlayerTeams(player);
                        TagManager.loadPlayerTag(uuid);

                        plugin.getLogger().info("✅ Settings applied for " + player.getName());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to apply settings for " + player.getName(), e);
                    }
                })
        ).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Error loading settings: " + ex.getMessage(), ex);
            return null;
        });
    }

    private void applyPlayerSettings(Player player) {
        // Apply scoreboard
        if (SettingsManager.isScoreboardEnabled(player)) {
            plugin.getScoreboard().createSidebar(player);
        } else {
            plugin.getScoreboard().removeSidebar(player);
        }

        // Apply lobby-specific settings
        if (isLobby()) {
            // Player visibility
            boolean visible = SettingsManager.isPlayersVisible(player);
            SettingsManager.updatePlayerVisibility(player, visible, false);

            // Player time
            PlayerTime time = SettingsManager.getPlayerTime(player);
            long timeValue = switch (time) {
                case DAY -> 1000L;
                case NIGHT -> 13000L;
                case SUNSET -> 12000L;
            };
            player.setPlayerTime(timeValue, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreProcess(@NotNull PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase().split(" ")[0];
        if (command.equals("/plugins") || command.equals("/pl") || command.equals("/bukkit:pl") || command.equals("/bukkit:plugins")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (!player.hasPermission("washere.plugins")) {
                player.sendMessage(ChatUtils.colorizeMini("&cAll Plugins that we use on the server are custom."));
            } else {
                event.setCancelled(false);
            }
        }
    }
}
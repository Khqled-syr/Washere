package me.washeremc.Core.Listeners;


import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.database.DatabaseManager;
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

import java.util.UUID;
import java.util.logging.Level;

import static me.washeremc.Core.Tags.TagManager.SETTING_KEY;


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
        plugin.getPlayerTimeManager().stopTracking(player);

    }

    @EventHandler(priority = EventPriority.LOWEST) // Changed to LOWEST to run first
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getPlayerTimeManager().startTracking(player);

        // Force synchronous tag load for a join message
        try {
            String tagId = DatabaseManager.loadSetting(uuid, SETTING_KEY, "").get();
            if (tagId != null && !tagId.isEmpty()) {
                TagManager.setPlayerTag(uuid, tagId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load tag for " + player.getName() + ": " + e.getMessage());
        }

        // Delay other settings loading slightly
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("🔄 Loading settings for " + player.getName() + "...");

            try {
                plugin.getTabList().setTabList(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update tablist for " + player.getName() + ": " + e.getMessage());
            }

            SettingsManager.loadPlayerSettingsAsync(uuid).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            applyPlayerSettings(player);
                            if (SettingsManager.isScoreboardEnabled(player)) {
                                plugin.getScoreboard().createSidebar(player);
                            }
                            plugin.getScoreboard().setPlayerTeams(player);
                            plugin.getTabList().updatePlayerListNames();
                            plugin.getLogger().info("✅ Settings applied for " + player.getName());
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, "Failed to apply settings for " + player.getName(), e);
                        }
                    })
            ).exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "Error loading settings: " + ex.getMessage(), ex);
                return null;
            });
        }, 5L); // Short delay to ensure the tag is loaded first
    }

    private void applyPlayerSettings(@NotNull Player player) {
        TagManager.refreshPlayerTag(player.getUniqueId());

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
        String rawCommand = event.getMessage().toLowerCase().split(" ")[0];
        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;

        if (plugin.getConfig().contains("blocked-commands")) {
            java.util.List<String> blockedCommands = plugin.getConfig().getStringList("blocked-commands");

            if (blockedCommands.contains(command)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                if (!player.hasPermission("washere.plugins")) {
                    player.sendMessage(ChatUtils.colorizeMini("&cYou are not allowed to use this command!"));
                } else {
                    event.setCancelled(false);
                }
            }
        }
    }
}
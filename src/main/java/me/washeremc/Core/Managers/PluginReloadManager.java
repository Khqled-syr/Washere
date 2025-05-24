package me.washeremc.Core.Managers;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Core.utils.ScoreBoard;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PluginReloadManager {
    private final Washere plugin;

    public PluginReloadManager(@NotNull Washere plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    public void reloadCustomConfig() {
        try {
            plugin.getLogger().info(ChatUtils.colorize("&eInitiating plugin reload..."));
            
            // Reload config
            reloadConfiguration();
            
            // Reload features in parallel
            CompletableFuture<Void> featuresFuture = CompletableFuture.runAsync(this::initializeFeatures);
            CompletableFuture<Void> serverTypeFuture = CompletableFuture.runAsync(this::updateServerType);
            CompletableFuture<Void> tagsFuture = CompletableFuture.runAsync(this::reloadTags);

            // Wait for all operations to complete
            CompletableFuture.allOf(featuresFuture, serverTypeFuture, tagsFuture).join();

            plugin.getLogger().info("Plugin reload completed successfully!");
            logReloadStatus();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during plugin reload", e);
            throw new RuntimeException("Failed to reload plugin", e);
        }
    }

    private void reloadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
    }

    public void initializeFeatures() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                ScoreBoard scoreBoard = plugin.getScoreboard();
                if (scoreBoard != null) {
                    scoreBoard.resetSidebars();
                }

                scoreBoard = new ScoreBoard(plugin);
                plugin.setScoreboard(scoreBoard);

                updatePlayerScoreboards(scoreBoard);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize features", e);
            }
        });
    }

    private void updatePlayerScoreboards(@NotNull ScoreBoard scoreBoard) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                if (SettingsManager.isScoreboardEnabled(player)) {
                    scoreBoard.createSidebar(player);
                } else {
                    scoreBoard.removeSidebar(player);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, 
                    "Failed to update scoreboard for player " + player.getName(), e);
            }
        }
    }

    public void reloadTags() {
        plugin.getLogger().info("Reloading tags...");
        try {
            TagManager.reload();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        TagManager.refreshPlayerTag(player.getUniqueId());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                            "Failed to reload tags for player " + player.getName(), e);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload tags", e);
        }
    }

    /**
     * Updates the server type based on configuration.
     */
    public void updateServerType() {
        String newServerType = plugin.getConfig().getString("server-type", "NONE");
        String currentType = plugin.getServerType();

        if (!newServerType.equalsIgnoreCase(currentType)) {
            plugin.getLogger().info(String.format("Server type changed: %s -> %s", 
                currentType, newServerType));
            plugin.setServerType(newServerType);
        }
    }

    private void logReloadStatus() {
        plugin.getLogger().info("=== Reload Status ===");
        plugin.getLogger().info("Server Type: " + plugin.getServerType());
        plugin.getLogger().info("Scoreboard: Active");
        plugin.getLogger().info("Tags: Reloaded");
        plugin.getLogger().info("==================");
    }
}
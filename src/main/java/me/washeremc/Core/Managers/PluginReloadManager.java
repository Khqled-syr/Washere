package me.washeremc.Core.Managers;


import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.utils.ScoreBoard;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

public class PluginReloadManager {

    private final Washere plugin;

    public PluginReloadManager(Washere plugin) {
        this.plugin = plugin;
    }

    public void reloadCustomConfig() {
        plugin.getLogger().info("Reloading config...");
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        initializeFeatures();
        updateServerType();
        reloadTags();

        plugin.getLogger().info("Config reloaded successfully.");
        plugin.getLogger().info("Current server type: " + plugin.getServerType());
        plugin.getLogger().info("Scoreboard reloaded successfully.");
        plugin.getLogger().info("servers status reloaded successfully.");
    }

    public void initializeFeatures() {
        ScoreBoard scoreBoard = plugin.getScoreboard();

        if (scoreBoard != null) {
            scoreBoard.resetSidebars();
        }
         scoreBoard = new ScoreBoard(plugin);
        plugin.setScoreboard(scoreBoard);


        for (Player player : Bukkit.getOnlinePlayers()) {
            if (SettingsManager.isScoreboardEnabled(player)) {
                scoreBoard.createSidebar(player);
            } else {
                scoreBoard.removeSidebar(player);
            }
        }
    }

    public void reloadTags() {
        plugin.getLogger().info("Reloading tags...");
        TagManager.loadTagsConfig();
        TagManager.loadTags();
        for (Player player : Bukkit.getOnlinePlayers()) {
            TagManager.loadPlayerTag(player.getUniqueId());
        }
        plugin.getLogger().info("Tags reloaded successfully.");
    }

    public void updateServerType(){
        String newServerType = plugin.getConfig().getString("server-type", "NONE");
        if (!newServerType.equalsIgnoreCase(plugin.getServerType())) {
            plugin.getLogger().info("Server type changed from " + plugin.getServerType() + " to " + newServerType);
            plugin.setServerType(newServerType);
            plugin.getLogger().info("Current server type: " + newServerType);
        } else {
            plugin.getLogger().info("Server type is already set to " + newServerType);
        }
    }
}

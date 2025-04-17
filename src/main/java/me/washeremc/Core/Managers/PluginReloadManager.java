package me.washeremc.Core.Managers;


import me.washeremc.Core.Settings.SettingsManager;
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
}

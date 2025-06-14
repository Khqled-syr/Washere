package me.washeremc.Registration;

import me.washeremc.Core.Managers.PluginReloadManager;
import me.washeremc.Core.PlayerTime.PlayTimeTrackerExpansion;
import me.washeremc.Core.PlayerTime.PlayTimeTracker;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.Tags.Tag;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Core.utils.ScoreBoard;
import me.washeremc.Core.utils.TabList;
import me.washeremc.SERVERMODE.lobby.LobbyListeners;
import me.washeremc.SERVERMODE.lobby.NPCUtils;
import me.washeremc.SERVERMODE.survival.Jail.JailManager;
import me.washeremc.SERVERMODE.survival.TPA.TpaManager;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PluginServices {

    private final Washere plugin;
    public PluginServices(Washere plugin) {
        this.plugin = plugin;
    }

    public void onStartup() {
        logStartupBanner();
        initializeConfig();
        initializeDatabase();
        initializeSettings();
        setupServerMode();
        registerPluginReloadManager();
        setupManagers();
        registerAllComponents();
        processExistingPlayers(plugin.getScoreboard());
    }

    public void onShutdown() {
        logShutdownMessage();
        cancelScheduledTasks();
        saveTagsOnShutdown();
        closeDatabase();
        plugin.getPlayerTimeManager().shutdown();
        if (plugin.getScoreboard() != null) {
            plugin.getScoreboard().resetSidebars();
        }
        logSuccessfulShutdown();
    }

    private void logStartupBanner() {
        String banner = """
            __          __       _    _              \s
            \\ \\        / /      | |  | |             \s
             \\ \\  /\\  / /_ _ ___| |__| | ___ _ __ ___\s
              \\ \\/  \\/ / _` / __|  __  |/ _ \\ '__/ _ \\
               \\  /\\  / (_| \\__ \\ |  | |  __/ | |  __/
                \\/  \\/ \\__,_|___/_|  |_|\\___|_|  \\___|
                                                       \s
              \
                        §b§lMade by Levaii
            """;
        Bukkit.getConsoleSender().sendMessage(banner);
    }

    private void initializeConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    private void initializeDatabase() {
        try {
            DatabaseManager.initialize(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("Database initialization failed: " + e.getMessage());
        }
    }

    private void initializeSettings() {
        try {
            SettingsManager.initialize(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("Settings initialization failed: " + e.getMessage());
        }
    }

    private void registerAllComponents() {
        new CommandManager(plugin).registerCommands();
        new ListenerManager(plugin).RegisterListeners();
        new UtilManager(plugin).RegisterUtils();
        checkForPlaceholderAPI();
    }

    private void checkForPlaceholderAPI() {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null && placeholderAPI.isEnabled()) {
            plugin.getLogger().info("Hooked into PlaceholderAPI.\n");
        } else {
            plugin.getLogger().warning("PlaceholderAPI not found!");
        }
    }

    public void processExistingPlayers(ScoreBoard scoreboard) {
        if (scoreboard == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!SettingsManager.isScoreboardEnabled(player)) {
                scoreboard.removeSidebar(player);
            }
        }
    }

    public void setupManagers() {
        try {
            plugin.setNpcUtils(new NPCUtils(plugin));
            plugin.getNpcUtils().loadNPCs();

            plugin.setScoreboard(new ScoreBoard(plugin));
            plugin.setTabList(new TabList(plugin));

            plugin.setTpaManager(new TpaManager(plugin));

            JailManager jailManager = new JailManager(plugin);
            jailManager.initialize();
            plugin.setJailManager(jailManager);

            try {
                PlayTimeTracker playTimeTracker = new PlayTimeTracker(plugin);
                plugin.setPlayerTimeManager(playTimeTracker);
                new PlayTimeTrackerExpansion(plugin, playTimeTracker).register();
                plugin.getLogger().info("PlayerTimeManager initialized successfully");
            } catch (Exception e) {
                plugin.getLogger().info("Failed to initialize PlayerTimeManager: " + e.getMessage());
                plugin.getLogger().info(e.getMessage());
            }


        } catch (Exception e) {
            plugin.getLogger().severe("Error while setting up managers: " + e.getMessage());
            plugin.getLogger().severe("Error" + e.getMessage());
        }
    }

    public void registerPluginReloadManager() {
        PluginReloadManager reloadManager = new PluginReloadManager(plugin);
        reloadManager.initializeFeatures();
        plugin.setPluginReloadManager(reloadManager);
    }

    public void setupServerMode() {
        @NotNull String type = plugin.getConfig().getString("server-type", "none").toLowerCase();
        plugin.setServerType(type);

        switch (type) {
            case "lobby" -> {
                plugin.getLogger().info("Lobby mode enabled.");
                Bukkit.getPluginManager().registerEvents(new LobbyListeners(plugin), plugin);
            }
            case "survival" -> {

                plugin.getLogger().info("Survival mode enabled.");
                plugin.getLogger().info("Loading survival mode features...");
            }
            default -> {
                plugin.getLogger().warning("Unknown server-type: " + type);
                plugin.getLogger().warning("Valid options: 'lobby', 'survival'");
            }
        }
    }

    private void logShutdownMessage() {
        plugin.getLogger().info(ChatUtils.colorize("\n&cWasHere plugin has been disabled.\n"));
    }

    private void logSuccessfulShutdown() {
        plugin.getLogger().info(ChatUtils.colorize("&cWasHere plugin disabled successfully."));
    }

    private void cancelScheduledTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
        plugin.getJailManager().shutdown();
        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().stop();
        }
    }

    private void closeDatabase() {
        try {
            DatabaseManager.closeConnection();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
        }
    }

    private void saveTagsOnShutdown() {
        TagManager.saveAllTags();
        //TagManager.reload();
    }
}

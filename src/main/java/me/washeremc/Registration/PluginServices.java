package me.washeremc.Registration;


import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Core.utils.DiscordLogger;
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

    public void logStartupMessage() {
        printBanner();
    }

    private void printBanner() {
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

    public void initializeConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public void initializeDatabase() {
        try {
            DatabaseManager.initialize(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public void initializeSettings() {
        try {
            SettingsManager.initialize(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize settings: " + e.getMessage());
        }
    }

    public void registerAllComponents() {
        new CommandManager(plugin).registerCommands();
        new ListenerManager(plugin).RegisterListeners();
        new UtilManager(plugin).RegisterUtils();
        checkForPlaceholderAPI();
    }

    private void checkForPlaceholderAPI() {
        Plugin placeholderAPIPlugin = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled()) {
            plugin.getLogger().info("PlaceholderAPI found! Hooking into it...");
        } else {
            plugin.getLogger().warning("PlaceholderAPI not found!");
        }
    }


    public void processExistingPlayers(ScoreBoard scoreboard) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!SettingsManager.isScoreboardEnabled(player)) {
                scoreboard.removeSidebar(player);
            }
        }
    }

    public void RegisterManagers(){
        plugin.npcUtils = new NPCUtils(plugin);
        plugin.npcUtils.loadNPCs();

        plugin.scoreboard =  new ScoreBoard(plugin);
        plugin.tabList = new TabList(plugin);
        plugin.tpaManager =  new TpaManager(plugin);
        plugin.jailManager = new JailManager(plugin);
        plugin.jailManager.initialize();
        DiscordLogger.initialize(plugin);
        DiscordLogger.logPluginUsage();
    }

    public void setupServerMode(@NotNull String serverType) {
        switch (serverType.toLowerCase()) {
            case "lobby":
                plugin.getLogger().info("Server is in Lobby mode. Enabling Lobby Features...");
                plugin.getServer().getPluginManager().registerEvents(new LobbyListeners(plugin), plugin);
                break;
            case "survival":
                plugin.getLogger().info("Server is in Survival mode. Enabling Survival Features...");
                break;
            default:
                plugin.getLogger().warning("Unknown server type: " + serverType + ". Please check your config.");
                plugin.getLogger().warning("Supported types: 'lobby', 'survival'");
                break;
        }
    }
    public void logShutdownMessage() {
        plugin.getLogger().info("\n§cWasHere plugin has been disabled.\n");
    }

    public void cancelScheduledTasks() {
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }

    public void closeDatabase() {
        try {
            DatabaseManager.closeConnection();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to close database connection! " + e.getMessage());
        }
    }
    public void logSuccessfulShutdown() {
        plugin.getLogger().info("§cWasHere plugin disabled successfully.");
    }
}

package me.washeremc.Registration;


import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Core.utils.ScoreBoard;
import me.washeremc.SERVERMODE.lobby.LobbyListeners;
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
        //plugin.getLogger().info("WasHere plugin has been enabled\n");
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
        DatabaseManager.initialize(plugin);
    }

    public void initializeSettings() {
        SettingsManager.initialize(plugin);
    }

    public void registerAllComponents() {
        CommandManager commandManager = new CommandManager(plugin);
        commandManager.registerCommands();

        ListenerManager listenersManager = new ListenerManager(plugin);
        listenersManager.RegisterListeners();

        UtilManager utilsManager = new UtilManager(plugin);
        utilsManager.RegisterUtils();

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
        Bukkit.getOnlinePlayers().forEach(player -> processPlayerScoreboard(player, scoreboard));
    }

    private void processPlayerScoreboard(Player player, ScoreBoard scoreboard) {
        if (!SettingsManager.isScoreboardEnabled(player)) {
            scoreboard.removeSidebar(player);
        }
    }

    public void setupServerMode(@NotNull String serverType) {
        if (serverType.equalsIgnoreCase("lobby")) {
            plugin.getLogger().info("Server is in Lobby mode. Enabling Lobby Features...");
            plugin.getServer().getPluginManager().registerEvents(new LobbyListeners(plugin), plugin);
        } else if (serverType.equalsIgnoreCase("survival")) {
            plugin.getLogger().info("Server is in Survival mode. Enabling Survival Features...");
            // Survival-specific initialization is now handled in ListenerManager
            // This ensures that survival features are only enabled when in survival mode
        } else {
            plugin.getLogger().warning("Unknown server type: " + serverType + ". Please check your config.");
            plugin.getLogger().warning("Supported types: 'lobby', 'survival'");
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

package me.washeremc;


import me.washeremc.Core.Managers.PluginReloadManager;
import me.washeremc.Core.utils.ScoreBoard;
import me.washeremc.Core.utils.TabList;
import me.washeremc.Registration.PluginServices;
import me.washeremc.SERVERMODE.lobby.NPCUtils;
import me.washeremc.SERVERMODE.survival.Jail.JailManager;
import me.washeremc.SERVERMODE.survival.TPA.TpaManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Washere extends JavaPlugin implements Listener {

    private PluginServices pluginServices;
    private TpaManager tpaManager;
    private ScoreBoard scoreboard;
    private PluginReloadManager pluginReloadManager;
    private TabList tabList;
    private String serverType;
    private NPCUtils npcUtils;
    private JailManager jailManager;

    @Override
    public void onEnable() {
        jailManager = new JailManager(this);
        // Create plugin services first
        pluginServices = new PluginServices(this);

        // Core initialization
        pluginServices.logStartupMessage();
        pluginServices.initializeConfig();
        pluginServices.initializeDatabase();
        pluginServices.initializeSettings();

        // Load server configuration
        serverType = getConfig().getString("server-type", "NONE");

        // Initialize managers
        pluginReloadManager = new PluginReloadManager(this);
        pluginReloadManager.initializeFeatures();

        tpaManager = new TpaManager(this);

        // Initialize components
        npcUtils = new NPCUtils(this);
        npcUtils.loadNPCs();
        scoreboard = new ScoreBoard(this);
        tabList = new TabList(this);

        // Register everything
        pluginServices.registerAllComponents();

        // Post-initialization tasks
        pluginServices.processExistingPlayers(scoreboard);
        pluginServices.setupServerMode(serverType);
    }

    @Override
    public void onDisable() {
        pluginServices.logShutdownMessage();
        pluginServices.cancelScheduledTasks();
        pluginServices.closeDatabase();

        // Clean up resources
        if (scoreboard != null) {
            scoreboard.resetSidebars();
        }

        pluginServices.logSuccessfulShutdown();
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public PluginReloadManager getPluginReloadManager() {
        return pluginReloadManager;
    }

    public String getServerType() {
        return serverType;
    }

    public ScoreBoard getScoreboard() {
        return scoreboard;
    }

    public TabList getTabList() {
        return tabList;
    }

    public NPCUtils getNpcUtils() {
        return npcUtils;
    }

    public JailManager getJailManager() {
        return jailManager;
    }

    public void setScoreboard(ScoreBoard scoreboard) {
        this.scoreboard = scoreboard;
    }
}

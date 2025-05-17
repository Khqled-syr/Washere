package me.washeremc;

import me.washeremc.Core.Managers.PluginReloadManager;
import me.washeremc.Core.Tags.TagManager;
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
    public TpaManager tpaManager;
    public ScoreBoard scoreboard;
    private PluginReloadManager pluginReloadManager;
    public TabList tabList;
    private String serverType;
    public NPCUtils npcUtils;
    public JailManager jailManager;
    public TagManager tagManager;

    @Override
    public void onEnable() {
        pluginServices = new PluginServices(this);

        pluginServices.logStartupMessage();
        pluginServices.initializeConfig();
        pluginServices.initializeDatabase();
        pluginServices.initializeSettings();

        serverType = getConfig().getString("server-type", "NONE");

        pluginReloadManager = new PluginReloadManager(this);
        pluginReloadManager.initializeFeatures();

        pluginServices.RegisterManagers();
        pluginServices.registerAllComponents();
        pluginServices.processExistingPlayers(scoreboard);
        pluginServices.setupServerMode(serverType);
    }

    @Override
    public void onDisable() {
        pluginServices.logShutdownMessage();
        pluginServices.cancelScheduledTasks();
        pluginServices.closeDatabase();

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
    public void setServerType(String serverType) {
        this.serverType = serverType;
    }
    public TagManager getTagManager() {
        return tagManager;
    }
}
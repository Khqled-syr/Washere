package me.washeremc;

import me.washeremc.Core.PlayerTime.PlayerTimeManager;
import me.washeremc.Registration.PluginServices;
import me.washeremc.Core.Managers.PluginReloadManager;
import me.washeremc.Core.utils.ScoreBoard;
import me.washeremc.Core.utils.TabList;
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
    private PlayerTimeManager playerTimeManager;

    @Override
    public void onEnable() {
        pluginServices = new PluginServices(this);
        pluginServices.onStartup();
    }

    @Override
    public void onDisable() {
        pluginServices.onShutdown();
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public ScoreBoard getScoreboard() {
        return scoreboard;
    }

    public PluginReloadManager getPluginReloadManager() {
        return pluginReloadManager;
    }

    public TabList getTabList() {
        return tabList;
    }

    public String getServerType() {
        return serverType;
    }

    public NPCUtils getNpcUtils() {
        return npcUtils;
    }

    public JailManager getJailManager() {
        return jailManager;
    }

    public PlayerTimeManager getPlayerTimeManager() {
        return playerTimeManager;
    }

    public void setTpaManager(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    public void setScoreboard(ScoreBoard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public void setPluginReloadManager(PluginReloadManager pluginReloadManager) {
        this.pluginReloadManager = pluginReloadManager;
    }

    public void setTabList(TabList tabList) {
        this.tabList = tabList;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public void setNpcUtils(NPCUtils npcUtils) {
        this.npcUtils = npcUtils;
    }

    public void setJailManager(JailManager jailManager) {
        this.jailManager = jailManager;
    }

    public void setPlayerTimeManager(PlayerTimeManager playerTimeManager) {
        this.playerTimeManager = playerTimeManager;
    }
}

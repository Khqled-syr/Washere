package me.washeremc.Registration;


import me.washeremc.Core.Profile.Profile;
import me.washeremc.Core.Settings.PlayerSetting.PvpListener;
import me.washeremc.Core.Settings.SettingsMenu;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.proxy.PluginMessage;
import me.washeremc.Core.proxy.ServerPing;
import me.washeremc.SERVERMODE.lobby.ServerTeleport;
import me.washeremc.SERVERMODE.survival.Home.HomeManager;
import me.washeremc.SERVERMODE.survival.Warp.WarpManager;
import me.washeremc.SERVERMODE.survival.Warp.WarpTabCompleter;
import me.washeremc.SERVERMODE.survival.utils.ActionBarManager;
import me.washeremc.SERVERMODE.survival.utils.BackpackUtils;
import me.washeremc.Washere;

import java.util.Objects;

public class UtilManager {
    private final Washere plugin;
    WarpTabCompleter tabCompleter = new WarpTabCompleter();


    public UtilManager(Washere plugin) {
        this.plugin = plugin;
    }

    public void RegisterUtils() {
        WarpManager.initialize(plugin);
        HomeManager.initialize(plugin);
        BackpackUtils.initialize(plugin);
        Profile.initialize(plugin);
        PluginMessage.initialize(plugin);
        ServerPing.initialize(plugin);
        ServerTeleport.initialize(plugin);
        TagManager.initialize(plugin);
        ServerPing.initialize(plugin);
        SettingsMenu.setPlugin(plugin);

        // Tab Completer for warps
        Objects.requireNonNull(plugin.getCommand("warp")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(plugin.getCommand("delwarp")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(plugin.getCommand("setwarp")).setTabCompleter(tabCompleter);
        //new ActionBarTask(plugin).runTaskTimer(plugin, 0L, 5L);

        ActionBarManager actionBarManager = new ActionBarManager(plugin);
        actionBarManager.start();
        plugin.setActionBarManager(actionBarManager);

        PvpListener pvpListener = new PvpListener(plugin,actionBarManager);
        plugin.getServer().getPluginManager().registerEvents(pvpListener, plugin);


    }
}

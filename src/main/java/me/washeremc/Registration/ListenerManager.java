package me.washeremc.Registration;


import me.washeremc.Core.Listeners.ChatListener;
import me.washeremc.Core.Listeners.ServerListeners;
import me.washeremc.Core.Settings.PlayerSetting.PvpListener;
import me.washeremc.Core.Settings.SettingsMenu;
import me.washeremc.Core.Settings.SettingsMenuListener;
import me.washeremc.SERVERMODE.survival.Jail.JailListeners;
import me.washeremc.SERVERMODE.survival.events.DonateListener;
import me.washeremc.SERVERMODE.survival.events.RecipeInventoryListener;
import me.washeremc.SERVERMODE.survival.events.SurvivalListeners;
import me.washeremc.SERVERMODE.survival.utils.AFKManager;
import me.washeremc.SERVERMODE.survival.utils.ActionBarTask;
import me.washeremc.SERVERMODE.survival.Warp.WarpTabCompleter;
import me.washeremc.Washere;

import java.util.Objects;

public class ListenerManager {

    private final Washere plugin;
    WarpTabCompleter tabCompleter = new WarpTabCompleter();

    public ListenerManager(Washere plugin) {
        this.plugin = plugin;
    }

    public void RegisterListeners(){
        plugin.getServer().getPluginManager().registerEvents(new ServerListeners(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SettingsMenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PvpListener(plugin), plugin);
        SettingsMenu.setPlugin(plugin);

        if (plugin.getNpcUtils() != null) {
            plugin.getServer().getPluginManager().registerEvents(plugin.getNpcUtils(), plugin);
        }

        if ("survival".equalsIgnoreCase(plugin.getServerType())) {
            plugin.getServer().getPluginManager().registerEvents(new SurvivalListeners(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new DonateListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new RecipeInventoryListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new AFKManager(plugin), plugin);

            if (plugin.getJailManager() != null) {
                plugin.getServer().getPluginManager().registerEvents(new JailListeners(plugin, plugin.getJailManager()), plugin);
            }

            Objects.requireNonNull(plugin.getCommand("warp")).setTabCompleter(tabCompleter);
            Objects.requireNonNull(plugin.getCommand("delwarp")).setTabCompleter(tabCompleter);
            Objects.requireNonNull(plugin.getCommand("setwarp")).setTabCompleter(tabCompleter);

            new ActionBarTask(plugin).runTaskTimer(plugin, 0L, 20L);
        }
    }
}

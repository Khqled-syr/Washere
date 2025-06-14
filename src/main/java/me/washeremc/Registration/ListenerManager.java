package me.washeremc.Registration;


import me.washeremc.Core.Listeners.ChatListener;
import me.washeremc.Core.Listeners.ServerListeners;
import me.washeremc.Core.Settings.SettingsMenuListener;
import me.washeremc.Core.Tags.TagListener;
import me.washeremc.Core.proxy.PluginMessage;
import me.washeremc.Core.utils.CommandTabFilter;
import me.washeremc.Core.utils.SittingSystem;
import me.washeremc.SERVERMODE.survival.Jail.JailListeners;
import me.washeremc.SERVERMODE.survival.events.RecipeInventoryListener;
import me.washeremc.SERVERMODE.survival.events.SurvivalListeners;
import me.washeremc.SERVERMODE.survival.utils.AFKManager;
import me.washeremc.Washere;

public class ListenerManager {

    private final Washere plugin;

    public ListenerManager(Washere plugin) {
        this.plugin = plugin;
    }

    public void RegisterListeners(){
        plugin.getServer().getPluginManager().registerEvents(new ServerListeners(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SettingsMenuListener(), plugin);



        plugin.getServer().getPluginManager().registerEvents(new TagListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SittingSystem(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CommandTabFilter(plugin), plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", new PluginMessage(plugin));


        if (plugin.getNpcUtils() != null) {
            plugin.getServer().getPluginManager().registerEvents(plugin.getNpcUtils(), plugin);
        }

        if ("survival".equalsIgnoreCase(plugin.getServerType())) {
            plugin.getServer().getPluginManager().registerEvents(new SurvivalListeners(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new RecipeInventoryListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new AFKManager(plugin), plugin);

            if (plugin.getJailManager() != null) {
                plugin.getServer().getPluginManager().registerEvents(new JailListeners(plugin, plugin.getJailManager()), plugin);
            }
        }
    }
}
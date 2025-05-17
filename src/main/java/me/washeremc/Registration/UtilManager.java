package me.washeremc.Registration;


import me.washeremc.Core.Profile.Profile;
import me.washeremc.Core.Tags.TagListener;
import me.washeremc.Core.Tags.TagManager;
import me.washeremc.Core.proxy.PluginMessage;
import me.washeremc.Core.proxy.ServerPing;
import me.washeremc.Core.utils.*;
import me.washeremc.SERVERMODE.lobby.ServerTeleport;
import me.washeremc.SERVERMODE.survival.Home.HomeManager;
import me.washeremc.SERVERMODE.survival.Warp.WarpManager;
import me.washeremc.SERVERMODE.survival.utils.BackpackUtils;
import me.washeremc.Washere;

public class UtilManager {
    private final Washere plugin;


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

        plugin.getServer().getPluginManager().registerEvents(new TagListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SittingSystem(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CommandTabFilter(plugin), plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", new PluginMessage(plugin));

        ServerPing.initialize(plugin);
    }

}

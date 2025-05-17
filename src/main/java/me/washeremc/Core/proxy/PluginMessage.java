package me.washeremc.Core.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.washeremc.SERVERMODE.lobby.ServerTeleport;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PluginMessage implements PluginMessageListener {

    private static Washere plugin;
    private static final Map<String, Boolean> serverStatus = new HashMap<>();
    private static final Map<String, String> serverIPs = new HashMap<>();
    private static final Map<String, Integer> serverPorts = new HashMap<>();

    public PluginMessage(Washere plugin) {
        PluginMessage.plugin = plugin;
        loadServersFromConfig();
    }

    public static void initialize(Washere pluginInstance) {
        if (pluginInstance == null) throw new IllegalArgumentException("[PluginMessage] Plugin instance cannot be null!");
        plugin = pluginInstance;
        loadServersFromConfig();
        startServerStatusUpdater();
    }

    public static void loadServersFromConfig() {
        ConfigurationSection serversSection = plugin.getConfig().getConfigurationSection("servers");
        if (serversSection == null) return;

        for (String server : serversSection.getKeys(false)) {
            String ip = serversSection.getString(server + ".ip");
            int port = serversSection.getInt(server + ".port");
            if (ip != null && port > 0) {
                serverIPs.put(server, ip);
                serverPorts.put(server, port);
                serverStatus.put(server, false);
            }
        }
    }
    public static void requestServerStatus(Player ignoredPlayer) {
        for (String server : serverIPs.keySet()) {
            ServerPing.pingServer(serverIPs.get(server), serverPorts.get(server), server);
        }
    }

    public static void updateServerStatus(String server, boolean isOnline) {
        serverStatus.put(server, isOnline);
        Bukkit.getScheduler().runTask(plugin, ServerTeleport::updateAllGUIs);
    }

    public static boolean isServerOnline(String server) {
        return serverStatus.getOrDefault(server, false);
    }

    public static void connect(@NotNull Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private static void startServerStatusUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) return;
                requestServerStatus(Bukkit.getOnlinePlayers().iterator().next());
            }
        }.runTaskTimer(plugin, 0L, 100L); // Every 5 seconds
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
    }
}

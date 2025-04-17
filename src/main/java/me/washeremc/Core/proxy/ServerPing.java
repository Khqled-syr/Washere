package me.washeremc.Core.proxy;

import me.washeremc.Washere;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServerPing {

    private static Washere plugin;

    public static void initialize(Washere pluginInstance) {
        if (pluginInstance == null) {
            throw new IllegalArgumentException("[ServerPing] Plugin instance cannot be null!");
        }
        plugin = pluginInstance;
    }

    public static void pingServer(String ip, int port, String serverName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean isOnline = isServerOnline(ip, port);

                // Return to the main thread for Bukkit API interaction
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PluginMessage.updateServerStatus(serverName, isOnline);
                    }
                }.runTask(plugin); // ← main thread
            }
        }.runTaskAsynchronously(plugin); // ← background thread
    }

    private static boolean isServerOnline(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1000); // 1-second timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

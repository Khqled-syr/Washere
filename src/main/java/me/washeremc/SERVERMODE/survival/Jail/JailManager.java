package me.washeremc.SERVERMODE.survival.Jail;

import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JailManager {
    private final Washere plugin;
    private Location jailLocation;
    private Location releaseLocation;
    private final Map<UUID, JailData> jailedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();

    public JailManager(Washere plugin) {
        this.plugin = plugin;
        loadJailData();

        Bukkit.getScheduler().runTaskTimer(plugin, this::checkJailTimes, 20L, 20L * 10);
    }

    public void loadJailData() {
        FileConfiguration config = plugin.getConfig();

        if (config.contains("jail.location")) {
            ConfigurationSection locSection = config.getConfigurationSection("jail.location");
            String worldName = Objects.requireNonNull(locSection).getString("world");
            double x = locSection.getDouble("x");
            double y = locSection.getDouble("y");
            double z = locSection.getDouble("z");
            float yaw = (float) locSection.getDouble("yaw");
            float pitch = (float) locSection.getDouble("pitch");

            jailLocation = new Location(Bukkit.getWorld(Objects.requireNonNull(worldName)), x, y, z, yaw, pitch);
        }

        if (config.contains("jail.release-location")) {
            ConfigurationSection locSection = config.getConfigurationSection("jail.release-location");
            String worldName = Objects.requireNonNull(locSection).getString("world");
            double x = locSection.getDouble("x");
            double y = locSection.getDouble("y");
            double z = locSection.getDouble("z");
            float yaw = (float) locSection.getDouble("yaw");
            float pitch = (float) locSection.getDouble("pitch");

            releaseLocation = new Location(Bukkit.getWorld(Objects.requireNonNull(worldName)), x, y, z, yaw, pitch);
        } else {
            releaseLocation = Bukkit.getWorlds().getFirst().getSpawnLocation();
        }

        if (config.contains("jail.players")) {
            ConfigurationSection playersSection = config.getConfigurationSection("jail.players");
            for (String uuidString : Objects.requireNonNull(playersSection).getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);

                long releaseTime = Objects.requireNonNull(playerSection).getLong("release-time");
                String reason = playerSection.getString("reason");

                if (releaseTime > System.currentTimeMillis()) {
                    jailedPlayers.put(uuid, new JailData(releaseTime, reason));

                    if (playerSection.contains("previous-location")) {
                        ConfigurationSection prevLocSection = playerSection.getConfigurationSection("previous-location");
                        if (prevLocSection != null) {
                            String worldName = prevLocSection.getString("world");
                            double x = prevLocSection.getDouble("x");
                            double y = prevLocSection.getDouble("y");
                            double z = prevLocSection.getDouble("z");
                            float yaw = (float) prevLocSection.getDouble("yaw");
                            float pitch = (float) prevLocSection.getDouble("pitch");

                            previousLocations.put(uuid, new Location(Bukkit.getWorld(Objects.requireNonNull(worldName)), x, y, z, yaw, pitch));
                        }
                    }
                }
            }
        }
    }

    public void saveJailData() {
        FileConfiguration config = plugin.getConfig();

        if (jailLocation != null) {
            ConfigurationSection locSection = config.createSection("jail.location");
            locSection.set("world", jailLocation.getWorld().getName());
            locSection.set("x", jailLocation.getX());
            locSection.set("y", jailLocation.getY());
            locSection.set("z", jailLocation.getZ());
            locSection.set("yaw", jailLocation.getYaw());
            locSection.set("pitch", jailLocation.getPitch());
        }

        if (releaseLocation != null) {
            ConfigurationSection locSection = config.createSection("jail.release-location");
            locSection.set("world", releaseLocation.getWorld().getName());
            locSection.set("x", releaseLocation.getX());
            locSection.set("y", releaseLocation.getY());
            locSection.set("z", releaseLocation.getZ());
            locSection.set("yaw", releaseLocation.getYaw());
            locSection.set("pitch", releaseLocation.getPitch());
        }

        config.set("jail.players", null); // Clear existing data
        ConfigurationSection playersSection = config.createSection("jail.players");

        for (Map.Entry<UUID, JailData> entry : jailedPlayers.entrySet()) {
            ConfigurationSection playerSection = playersSection.createSection(entry.getKey().toString());
            playerSection.set("release-time", entry.getValue().releaseTime());
            playerSection.set("reason", entry.getValue().reason());

            Location prevLoc = previousLocations.get(entry.getKey());
            if (prevLoc != null) {
                ConfigurationSection prevLocSection = playerSection.createSection("previous-location");
                prevLocSection.set("world", prevLoc.getWorld().getName());
                prevLocSection.set("x", prevLoc.getX());
                prevLocSection.set("y", prevLoc.getY());
                prevLocSection.set("z", prevLoc.getZ());
                prevLocSection.set("yaw", prevLoc.getYaw());
                prevLocSection.set("pitch", prevLoc.getPitch());
            }
        }

        plugin.saveConfig();
    }

    private void checkJailTimes() {
        long currentTime = System.currentTimeMillis();

        jailedPlayers.entrySet().removeIf(entry -> {
            if (currentTime >= entry.getValue().releaseTime()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);

                if (player != null && player.isOnline()) {
                    player.sendMessage("§aYou have been released from jail!");

                    teleportToReleaseLocation(player);
                }

                previousLocations.remove(uuid);
                return true;
            }
            return false;
        });
    }

    private void teleportToReleaseLocation(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        Location prevLoc = previousLocations.get(uuid);
        if (prevLoc != null && prevLoc.getWorld() != null) {
            player.teleport(prevLoc);
        } else
            player.teleport(Objects.requireNonNullElseGet(releaseLocation, () -> Bukkit.getWorlds().getFirst().getSpawnLocation()));

        previousLocations.remove(uuid);
    }

    public boolean isJailed(UUID uuid) {
        return jailedPlayers.containsKey(uuid);
    }

    public JailData getJailData(UUID uuid) {
        return jailedPlayers.get(uuid);
    }

    public boolean setJailLocation(Location location) {
        if (location == null) return false;
        this.jailLocation = location.clone();
        saveJailData();
        return true;
    }

    public Location getJailLocation() {
        return jailLocation;
    }

    public boolean jailPlayer(UUID uuid, long durationSeconds, String reason) {
        if (jailLocation == null) return false;

        Player player = Bukkit.getPlayer(uuid);
        long releaseTime = System.currentTimeMillis() + (durationSeconds * 1000);
        jailedPlayers.put(uuid, new JailData(releaseTime, reason));

        if (player != null && player.isOnline()) {
            previousLocations.put(uuid, player.getLocation().clone());

            player.teleport(jailLocation);
            player.sendMessage("§cYou have been jailed for: " + reason);
            player.sendMessage("§cTime: " + formatTime(durationSeconds));
        }

        saveJailData();
        return true;
    }

    public boolean unjailPlayer(UUID uuid) {
        if (!jailedPlayers.containsKey(uuid)) return false;

        jailedPlayers.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            player.sendMessage("§aYou have been released from jail!");

            teleportToReleaseLocation(player);
        }

        saveJailData();
        return true;
    }

    public String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0 seconds";
        }

        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder time = new StringBuilder();
        if (days > 0) {
            time.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
        }
        if (hours > 0) {
            time.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
        }
        if (minutes > 0) {
            time.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        }
        if (seconds > 0) {
            time.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }

        return time.toString().trim();
    }

    public record JailData(long releaseTime, String reason) {

    }
}
package me.washeremc.SERVERMODE.survival.Warp;

import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("ALL")
public class WarpManager {
    private static final Map<UUID, Map<String, Location>> warps = new HashMap<>();
    private static File warpsFile;
    private static FileConfiguration warpsConfig;
    private static Washere plugin;
    private static final Logger logger = Bukkit.getLogger();
    private static boolean initialized = false;

    public static void initialize(@NotNull Washere pluginInstance) {
        plugin = pluginInstance;

        // ✅ Only initialize if we're in survival mode
        if (!isSurvivalMode()) {
            pluginInstance.getLogger().info("Warp system not initialized - not in survival mode.");
            return;
        }

        warpsFile = new File(pluginInstance.getDataFolder(), "warps.yml");

        if (!warpsFile.exists()) {
            warpsFile.getParentFile().mkdirs();
            try {
                warpsFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Could not create warps.yml file");
                e.printStackTrace();
            }
        }

        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        loadWarps();
        initialized = true;
    }

    private static boolean isSurvivalMode() {
        return "survival".equalsIgnoreCase(plugin.getServerType());
    }

    public static void setWarp(UUID playerUUID, String warpName, Location location) {
        if (!initialized) return;

        warps.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(warpName, location);
        saveWarps();
    }

    public static Location getWarp(UUID playerUUID, String warpName) {
        if (!initialized) return null;

        return warps.getOrDefault(playerUUID, new HashMap<>()).get(warpName);
    }

    public static @NotNull Set<String> getWarps(UUID playerUUID) {
        if (!initialized) return Collections.emptySet();

        return warps.getOrDefault(playerUUID, new HashMap<>()).keySet();
    }

    public static boolean warpExists(UUID playerUUID, String warpName) {
        if (!initialized) return false;

        return warps.getOrDefault(playerUUID, new HashMap<>()).containsKey(warpName);
    }

    public static void loadWarps() {
        if (!initialized) return;

        for (String uuidString : warpsConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(uuidString);
            ConfigurationSection warpsSection = warpsConfig.getConfigurationSection(uuidString);
            if (warpsSection == null) {
                logger.severe("Warp section for " + uuidString + " is null.");
                continue;
            }

            for (String warpName : warpsSection.getKeys(false)) {
                ConfigurationSection warpConfigSection = warpsSection.getConfigurationSection(warpName);
                if (warpConfigSection == null) {
                    logger.severe("Warp config section for " + warpName + " in " + uuidString + " is null.");
                    continue;
                }
                Map<String, Object> warpConfig = warpConfigSection.getValues(false);
                Location warpLocation = getLocationFromConfig(warpConfig);
                if (warpLocation != null) {
                    warps.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(warpName, warpLocation);
                } else {
                    logger.severe("Failed to load warp " + warpName + " for player " + uuidString);
                }
            }
        }
    }

    public static @Nullable Location getLocationFromConfig(@NotNull Map<String, Object> config) {
        String worldName = (String) config.get("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.severe("World " + worldName + " is not loaded or does not exist.");
            return null;
        }
        double x = (double) config.get("x");
        double y = (double) config.get("y");
        double z = (double) config.get("z");
        float pitch = ((Double) config.get("pitch")).floatValue();
        float yaw = ((Double) config.get("yaw")).floatValue();
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static boolean deleteWarp(UUID playerUUID, String warpName) {
        if (!initialized) return false;

        if (warps.containsKey(playerUUID) && warps.get(playerUUID).containsKey(warpName)) {
            warps.get(playerUUID).remove(warpName);
            if (warps.get(playerUUID).isEmpty()) {
                warps.remove(playerUUID);
            }
            saveWarps();
            return true;
        }
        return false;
    }

    private static void saveWarps() {
        if (!initialized) return;

        warpsConfig = new YamlConfiguration();
        for (UUID playerUUID : warps.keySet()) {
            for (Map.Entry<String, Location> entry : warps.get(playerUUID).entrySet()) {
                String path = playerUUID.toString() + "." + entry.getKey();
                Location location = entry.getValue();
                warpsConfig.set(path + ".world", location.getWorld().getName());
                warpsConfig.set(path + ".x", location.getX());
                warpsConfig.set(path + ".y", location.getY());
                warpsConfig.set(path + ".z", location.getZ());
                warpsConfig.set(path + ".yaw", location.getYaw());
                warpsConfig.set(path + ".pitch", location.getPitch());
            }
        }
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            logger.severe("Failed to save warps.yml");
            e.printStackTrace();
        }
    }
}

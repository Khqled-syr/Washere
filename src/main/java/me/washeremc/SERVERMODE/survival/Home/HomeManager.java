package me.washeremc.SERVERMODE.survival.Home;

import me.washeremc.Washere;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class HomeManager {
    private static final Map<UUID, Location> homes = new HashMap<>();
    private static File homesFile;
    private static FileConfiguration homesConfig;
    private static JavaPlugin plugin;
    private static boolean initialized = false;

    public static void initialize(@NotNull JavaPlugin pluginInstance) {
        plugin = pluginInstance;

        if (!isSurvivalMode()) {
            pluginInstance.getLogger().info("Home system not initialized - not in survival mode.");
            return;
        }

        homesFile = new File(pluginInstance.getDataFolder(), "homes.yml");

        if (!homesFile.exists()) {
            File parentDir = homesFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                pluginInstance.getLogger().severe("Failed to create parent directories for homes.yml");
                return;
            }
            try {
                if (!homesFile.createNewFile()) {
                    pluginInstance.getLogger().severe("Failed to create homes.yml (file already exists or unknown issue)");
                }
            } catch (IOException e) {
                pluginInstance.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create homes.yml", e);
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);

        initialized = true;
        pluginInstance.getLogger().info("Home system initialized.");
        loadHomes();
    }

    private static boolean isSurvivalMode() {
        if (plugin instanceof Washere) {
            return "survival".equalsIgnoreCase(((Washere) plugin).getServerType());
        }
        return true;
    }

    public static void setHome(UUID playerUUID, Location location) {
        if (!initialized) return;
        homes.put(playerUUID, location);
        saveHomes();
    }

    public static @Nullable Location getHome(UUID playerUUID) {
        if (!initialized) return null;
        return homes.get(playerUUID);
    }

    public static void loadHomes() {
        if (!initialized || homesConfig == null) return;

        for (String uuidString : homesConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(uuidString);
            Location homeLocation = (Location) homesConfig.get(uuidString);
            homes.put(playerUUID, homeLocation);
        }
        plugin.getLogger().info("Loaded " + homes.size() + " homes");
    }

    private static void saveHomes() {
        if (!initialized || homesFile == null) return;
        homesConfig = new YamlConfiguration();
        try {
            for (UUID playerUUID : homes.keySet()) {
                homesConfig.set(playerUUID.toString(), homes.get(playerUUID));
            }
            homesConfig.save(homesFile);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save homes.yml", e);
            } else {
                JavaPlugin.getPlugin(Washere.class).getLogger().log(Level.SEVERE, "Failed to save homes.yml", e);
            }
        }
    }
}
package me.washeremc.SERVERMODE.survival.utils;

import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BackpackUtils implements Listener {

    private static final Map<UUID, Inventory> backpacks = new HashMap<>();
    private static File backpackFile;
    private static FileConfiguration backpackConfig;
    private static Washere plugin;
    private static boolean initialized = false;

    public static void initialize(@NotNull Washere pluginInstance) {
        plugin = pluginInstance;

        // Check if this is a survival server
        if (!isSurvivalMode()) {
            pluginInstance.getLogger().info("Backpack system not initialized - not in survival mode.");
            return;
        }

        backpackFile = new File(pluginInstance.getDataFolder(), "backpacks.yml");

        if (!backpackFile.exists()) {
            File parentDir = backpackFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                pluginInstance.getLogger().severe("Failed to create parent directories for backpacks.yml");
                return;
            }

            try {
                if (!backpackFile.createNewFile()) {
                    pluginInstance.getLogger().severe("Failed to create backpacks.yml (already exists or unknown error)");
                }
            } catch (IOException e) {
                pluginInstance.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create backpack file", e);
            }
        }

        backpackConfig = YamlConfiguration.loadConfiguration(backpackFile);
        loadBackpacks();
        Bukkit.getPluginManager().registerEvents(new BackpackUtils(), pluginInstance);
        initialized = true;
    }

    private static boolean isSurvivalMode() {
        // Assuming the plugin has a method to check server type like in the previous example
        return "survival".equalsIgnoreCase(plugin.getServerType());
    }

    public static void setBackpack(UUID playerUUID, Inventory inventory) {
        if (!initialized) return;

        backpacks.put(playerUUID, inventory);
        saveBackpacks();
    }

    public static Inventory getBackpack(UUID playerUUID) {
        if (!initialized) {
            return Bukkit.createInventory(null, 54, Component.text("Backpack"));
        }

        return backpacks.getOrDefault(playerUUID, Bukkit.createInventory(null, 9, Component.text("Backpack")));
    }

    private static void loadBackpacks() {
        if (!initialized) return;

        for (String uuidString : backpackConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(uuidString);
            Inventory inventory = Bukkit.createInventory(null, 54, Component.text("Backpack"));
            for (int i = 0; i < 54; i++) {
                inventory.setItem(i, backpackConfig.getItemStack(uuidString + "." + i));
            }
            backpacks.put(playerUUID, inventory);
        }
    }

    private static void saveBackpacks() {
        if (!initialized || backpackFile == null) return;

        backpackConfig = new YamlConfiguration();
        try {
            for (UUID playerUUID : backpacks.keySet()) {
                Inventory inventory = backpacks.get(playerUUID);
                for (int i = 0; i < inventory.getSize(); i++) {
                    backpackConfig.set(playerUUID.toString() + "." + i, inventory.getItem(i));
                }
            }
            backpackConfig.save(backpackFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save backpacks", e);
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!initialized) return;

        if (event.getView().title().equals(Component.text("Backpack"))) {
            UUID playerUUID = event.getPlayer().getUniqueId();
            setBackpack(playerUUID, event.getInventory());
        }
    }
}
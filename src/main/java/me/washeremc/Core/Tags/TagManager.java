package me.washeremc.Core.Tags;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TagManager {
    private static Washere plugin;
    private static final Map<String, Tag> tags = new HashMap<>();
    private static final Map<UUID, String> playerTags = new HashMap<>();
    private static FileConfiguration tagsConfig;
    public static final String SETTING_KEY = "selectedTag";

    public static void initialize(Washere pluginInstance) {
        plugin = pluginInstance;
        try {
            plugin.getLogger().info("Initializing Tags system...");
            loadTagsConfig();
            loadTags();

            // Wait for the database to be ready before loading tags
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                preloadAllPlayerTags();
                plugin.getLogger().info("Tags system initialized successfully.");
            }, 20L); // 1 second delay

            new TagPlaceholderExpansion(plugin).register();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Tags system: " + e.getMessage());
        }
    }


    public static void preloadAllPlayerTags() {
        // Cancel any existing tasks to avoid duplicates
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // First, clear any existing cache to ensure fresh data
                playerTags.clear();

                // Load all tags from database
                Map<UUID, String> allTags = SettingsManager.getAllPlayerTags();

                for (Map.Entry<UUID, String> entry : allTags.entrySet()) {
                    UUID uuid = entry.getKey();
                    String tagId = entry.getValue();

                    if (tagId != null && !tagId.isEmpty() && tags.containsKey(tagId)) {
                        playerTags.put(uuid, tagId);
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        refreshPlayerTag(player.getUniqueId());
                        plugin.getTabList().updatePlayerListNames();
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error preloading player tags: " + e.getMessage());
            }
        });
    }

    public static void loadTagsConfig() {
        File tagsFile = new File(plugin.getDataFolder(), "tags.yml");
        if (!tagsFile.exists()) {
            plugin.saveResource("tags.yml", false);
        }
        tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);
    }

    public static void loadTags() {
        tags.clear();
        ConfigurationSection tagsSection = tagsConfig.getConfigurationSection("tags");
        if (tagsSection != null) {
            for (String tagId : tagsSection.getKeys(false)) {
                ConfigurationSection tagSection = tagsSection.getConfigurationSection(tagId);
                if (tagSection != null) {
                    String displayName = ChatUtils.colorize(tagSection.getString("display-name", tagId));
                    String prefix = ChatUtils.colorize(tagSection.getString("prefix", ""));
                    String suffix = ChatUtils.colorize(tagSection.getString("suffix", ""));
                    String permission = tagSection.getString("permission", "washere.tag." + tagId);
                    String material = tagSection.getString("material", "NAME_TAG");
                    int slot = tagSection.getInt("slot", 0);

                    Tag tag = new Tag(tagId, displayName, prefix, suffix, permission, material, slot);
                    tags.put(tagId, tag);
                }
            }
        }
        plugin.getLogger().info("Loaded " + tags.size() + " tags");
    }

    @Contract(" -> new")
    public static @NotNull List<Tag> getAllTags() {
        return new ArrayList<>(tags.values());
    }

    public static @NotNull List<Tag> getAvailableTags(Player player) {
        List<Tag> availableTags = new ArrayList<>();
        for (Tag tag : tags.values()) {
            if (player.hasPermission(tag.permission())) {
                availableTags.add(tag);
            }
        }
        return availableTags;
    }

    public static @Nullable Tag getPlayerTag(UUID uuid) {
        String tagId = playerTags.get(uuid);

        if (tagId == null) {
            tagId = SettingsManager.getSettingValue(uuid, SETTING_KEY, "");
            playerTags.put(uuid, tagId);
        }
        if (tagId.isEmpty() || !tags.containsKey(tagId)) {
            return null;
        }

        return tags.get(tagId);
    }

    public static void refreshPlayerTag(UUID uuid) {
        // We need to force a database check here
        playerTags.remove(uuid);

        // Get fresh data from a database
        String tagId = SettingsManager.getSettingValue(uuid, SETTING_KEY, "");
        if (!tagId.isEmpty() && tags.containsKey(tagId)) {
            playerTags.put(uuid, tagId);
            plugin.getLogger().info("Refreshed tag " + tagId + " for player " + uuid);
        } else {
            playerTags.put(uuid, "");
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTabList().updatePlayerListNames());
        }
    }

    public static void saveAllTags() {
        for (Map.Entry<UUID, String> entry : playerTags.entrySet()) {
            try {
                DatabaseManager.saveSettingSync(entry.getKey(), SETTING_KEY, entry.getValue());
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save tag for " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    public static void setPlayerTag(UUID uuid, String tagId) {
        CompletableFuture.runAsync(() -> {
            try {
                if (tagId == null || tagId.isEmpty() || !tags.containsKey(tagId)) {
                    playerTags.put(uuid, "");
                    DatabaseManager.saveSettingSync(uuid, SETTING_KEY, ""); // Force synchronous save
                } else {
                    playerTags.put(uuid, tagId);
                    DatabaseManager.saveSettingSync(uuid, SETTING_KEY, tagId); // Force synchronous save
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        plugin.getTabList().updatePlayerListNames();
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save tag for " + uuid + ": " + e.getMessage());
            }
        });
    }


    public static void reload() {
        Map<UUID, String> currentTags = new HashMap<>(playerTags);
        playerTags.clear();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<UUID, String> loadedTags = SettingsManager.getAllPlayerTags();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    try {
                        // Force synchronous load for online players
                        String tagId = DatabaseManager.loadSetting(uuid, SETTING_KEY, "").get();
                        if (tagId != null && !tagId.isEmpty() && tags.containsKey(tagId)) {
                            playerTags.put(uuid, tagId);
                            plugin.getLogger().info("Restored tag " + tagId + " for online player " + player.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to restore tag for online player " + player.getName() + ": " + e.getMessage());
                        // Restore previous tag if loading fails
                        if (currentTags.containsKey(uuid)) {
                            playerTags.put(uuid, currentTags.get(uuid));
                        }
                    }
                }

                loadedTags.forEach((uuid, tagId) -> {
                    if (!Bukkit.getPlayer(uuid).isOnline() && tags.containsKey(tagId)) {
                        playerTags.put(uuid, tagId);
                        plugin.getLogger().info("Restored tag " + tagId + " for offline player " + uuid);
                    }
                });

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getTabList().updatePlayerListNames();
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error reloading tags: " + e.getMessage());
                // Restore all previous tags if something goes wrong
                playerTags.putAll(currentTags);
            }
        });
    }

    public static void loadPlayerTag(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String tagId = DatabaseManager.loadSetting(uuid, SETTING_KEY, "").get(); // Force synchronous load

                if (tagId != null && !tagId.isEmpty() && tags.containsKey(tagId)) {
                    playerTags.put(uuid, tagId);
                    plugin.getLogger().info("Loaded tag " + tagId + " for player " + uuid);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            plugin.getTabList().updatePlayerListNames();
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load tag for " + uuid + ": " + e.getMessage());
            }
        });
    }
}
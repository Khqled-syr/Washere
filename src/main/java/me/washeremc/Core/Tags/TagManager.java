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
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TagManager {
    private static Washere plugin;
    private static final Map<String, Tag> tags = new HashMap<>();
    private static final Map<UUID, String> playerTags = new HashMap<>();
    private static FileConfiguration tagsConfig;
    private static final String SETTING_KEY = "selectedTag";

    public static void initialize(Washere pluginInstance) {
        plugin = pluginInstance;
        try{
            plugin.getLogger().info("Tags system initialized.");
            loadTagsConfig();
            loadTags();
            preloadAllPlayerTags();
            new TagPlaceholderExpansion(plugin).register();
        }catch (Exception e){
            plugin.getLogger().severe("Failed to initialize Tags system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void preloadAllPlayerTags() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                loadPlayerTag(player.getUniqueId());
            }
                SettingsManager.getAllPlayerTags().forEach((uuid, tagId) -> {
                    if (tags.containsKey(tagId)) {
                        playerTags.put(uuid, tagId);
                    }
                });
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

    public static Tag getTag(String id) {
        return tags.get(id);
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
        playerTags.remove(uuid);

        String tagId = SettingsManager.getSettingValue(uuid, SETTING_KEY, "");
        if (!tagId.isEmpty() && tags.containsKey(tagId)) {
            playerTags.put(uuid, tagId);
        } else {
            playerTags.put(uuid, "");
        }
    }

    public static CompletableFuture<Void> setPlayerTag(UUID uuid, String tagId) {
        playerTags.remove(uuid);

        if (tagId == null || tagId.isEmpty()) {
            playerTags.put(uuid, "");
            return SettingsManager.savePlayerTag(uuid, "").thenCompose(v ->
                    DatabaseManager.saveSetting(uuid, SETTING_KEY, ""));
        } else if (tags.containsKey(tagId)) {
            playerTags.put(uuid, tagId);
            return SettingsManager.savePlayerTag(uuid, tagId).thenCompose(v ->
                    DatabaseManager.saveSetting(uuid, SETTING_KEY, tagId));
        }
        return CompletableFuture.completedFuture(null);
    }

    public static @NotNull String formatPlayerName(@NotNull Player player) {
        Tag tag = getPlayerTag(player.getUniqueId());
        if (tag != null) {
            return tag.prefix() + player.getName() + tag.suffix();
        }
        return player.getName();
    }

    public static void reload() {
        loadTagsConfig();
        loadTags();
        playerTags.clear();
    }

    public static void loadPlayerTag(UUID uuid) {
        String tagId = SettingsManager.getSettingValue(uuid, SETTING_KEY, "");
        if (!tagId.isEmpty() && tags.containsKey(tagId)) {
            playerTags.put(uuid, tagId);
        }
    }
}
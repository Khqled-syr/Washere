package me.washeremc.Core.Settings;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static me.washeremc.Core.Tags.TagManager.SETTING_KEY;

public final class SettingsManager {
    private static final Map<UUID, Map<String, Object>> playerSettings = new ConcurrentHashMap<>();
    private static final int PVP_TOGGLE_COOLDOWN = 60;
    private static final String PVP_TOGGLE_KEY = "pvp_toggle";
    private static Washere plugin;

    private SettingsManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    public static void initialize(@NotNull Washere pluginInstance) {
        Objects.requireNonNull(pluginInstance, "Plugin instance cannot be null");
        plugin = pluginInstance;
    }


    public static <T> @Nullable T getSettingValue(@NotNull Player player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        return getSettingValue(player.getUniqueId(), key);
    }


    public static <T> @Nullable T getSettingValue(@NotNull UUID uuid, @NotNull String key) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        return (T) playerSettings
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> SettingRegistry.getSetting(key).getDefaultValue());
    }


    @Contract("_, _, _ -> !null")
    public static <T> T getSettingValue(@NotNull Player player, @NotNull String key, @NotNull T defaultValue) {
        Objects.requireNonNull(player, "Player cannot be null");
        return getSettingValue(player.getUniqueId(), key, defaultValue);
    }


    @Contract("_, _, _ -> !null")
    public static <T> T getSettingValue(@NotNull UUID uuid, @NotNull String key, @NotNull T defaultValue) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(defaultValue, "Default value cannot be null");

        return (T) playerSettings
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .getOrDefault(key, defaultValue);
    }


    @Contract("_ -> new")
    public static @NotNull CompletableFuture<Void> loadPlayerSettingsAsync(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return CompletableFuture.runAsync(() -> loadPlayerSettings(uuid))
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load player settings", throwable);
                    return null;
                });
    }


    public static <T> boolean toggleSetting(@NotNull Player player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        UUID uuid = player.getUniqueId();
        Setting<T> setting = SettingRegistry.getSetting(key);

        if (isPvpToggle(key) && !handlePvpCooldown(player, uuid)) {
            return false;
        }

        T currentValue = getSettingValue(player, key);
        T newValue = setting.toggle(currentValue);

        updateSetting(uuid, key, newValue);
        scheduleSettingEffect(player, key, newValue);

        return true;
    }


    private static boolean handlePvpCooldown(Player player, UUID uuid) {
        if (CooldownManager.isOnCooldown(uuid, PVP_TOGGLE_KEY)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, PVP_TOGGLE_KEY);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + " &cseconds before toggling PVP again!"));
            return false;
        }
        CooldownManager.setCooldown(uuid, PVP_TOGGLE_KEY, PVP_TOGGLE_COOLDOWN);
        return true;
    }


    private static <T> void updateSetting(UUID uuid, String key, T newValue) {
        playerSettings.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(key, newValue);
        DatabaseManager.saveSetting(uuid, key, newValue);
    }



    private static <T> void scheduleSettingEffect(Player player, String key, T newValue) {
        Bukkit.getScheduler().runTask(plugin, () -> applySettingEffect(player, key, newValue));
    }

    private static <T> void applySettingEffect(Player player, String key, T value) {
        try {
            switch (key) {

                //GENERAL
                case "scoreboard" -> toggleScoreboard(player, (Boolean) value);
                case "messaging" -> notifyToggle(player, "Messaging", (Boolean) value);
                case "pinging" -> notifyToggle(player, "Pinging", (Boolean) value);

                //LOBBY
                case "players_visibility" -> updatePlayerVisibility(player, (Boolean) value, true);
                case "player_time" -> applyPlayerTime(player, (PlayerTime) value);

                //SURVIVAL
                case "tpa" -> notifyToggle(player, "TPA", (Boolean) value);
                case "actionbar" -> notifyToggle(player, "Action Bar", (Boolean) value);
                case "pvp" -> notifyPvpStatus(player, (Boolean) value);
                default -> plugin.getLogger().warning("Unknown setting key: " + key);
            }
        } catch (ClassCastException e) {
            plugin.getLogger().log(Level.SEVERE, "Invalid value type for setting " + key, e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error applying setting " + key, e);
        }
    }

    private static void notifyToggle(@NotNull Player player, String settingName, boolean enabled) {
        player.sendMessage(ChatUtils.colorizeMini(enabled ?
                "&a" + settingName + " has been enabled." :
                "&c" + settingName + " has been disabled."));
    }

    private static void notifyPvpStatus(Player player, boolean enabled) {
        if (enabled) {
            player.sendMessage(ChatUtils.colorizeMini("&aYou have enabled PVP. You can now attack and be attacked by other players."));
        } else {
            player.sendMessage(ChatUtils.colorizeMini("&cYou have disabled PVP. You cannot attack or be attacked by other players."));
        }
    }

    private static void toggleScoreboard(Player player, boolean enabled) {
        if (enabled) {
            plugin.getScoreboard().createSidebar(player);
            player.sendMessage(ChatUtils.colorizeMini("&a" + "Scoreboard" + " has been enabled."));
        } else {
            plugin.getScoreboard().removeSidebar(player);
            player.sendMessage(ChatUtils.colorizeMini("&c" + "Scoreboard" + " has been disabled."));
        }
    }

    public static void updatePlayerVisibility(Player player, boolean visible, boolean isToggle) {
        if (player == null) {
            plugin.getLogger().warning("Player is null, cannot update visibility.");
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (visible) {
                player.showPlayer(plugin, online);
            } else {
                player.hidePlayer(plugin, online);
            }
        }

        if (isToggle) {
            player.sendMessage(ChatUtils.colorizeMini("&ePlayers are now " + (visible ? "&avisible" : "&cinvisible") + "&e."));
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                boolean onlinePlayerSettings = isPlayersVisible(online);
                if (onlinePlayerSettings) {
                    online.showPlayer(plugin, player);
                } else {
                    online.hidePlayer(plugin, player);
                }
            }
        }
    }

    private static void applyPlayerTime(@NotNull Player player, @NotNull PlayerTime time) {
        player.setPlayerTime(switch (time) {
            case DAY -> 1000L;
            case NIGHT -> 13000L;
            case SUNSET -> 12000L;
        }, false);
        player.sendMessage(ChatUtils.colorize("&aPlayer time has been set to " + time.name().toLowerCase() + "."));
    }

    public static void loadPlayerSettings(UUID uuid) {
        Map<String, Object> settings = new ConcurrentHashMap<>();
        Map<String, CompletableFuture<?>> futures = new HashMap<>();

        for (Setting<?> setting : SettingRegistry.getSettings()) {
            String key = setting.getKey();
            Object defaultValue = setting.getDefaultValue();

            futures.put(key, DatabaseManager.loadSetting(uuid, key, defaultValue)
                    .thenAccept(value -> {
                        if (key.equals("selectedTag")) {
                            settings.put(key, value != null ? value : "");
                        } else {
                            settings.put(key, value != null ? value : defaultValue);
                        }
                    })
                    .exceptionally(e -> {
                        plugin.getLogger().log(Level.SEVERE, "Error loading setting: " + key, e);
                        settings.put(key, key.equals("selectedTag") ? "" : defaultValue);
                        return null;
                    }));
        }

        try {
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error waiting for settings to load", e);
        }
        playerSettings.put(uuid, settings);
    }

    public static void savePlayerSettings(UUID uuid) {
        Map<String, Object> settings = playerSettings.get(uuid);
        if (settings == null || settings.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            CompletableFuture<?>[] futures = settings.entrySet().stream()
                .map(entry -> DatabaseManager.saveSetting(uuid, entry.getKey(), entry.getValue()))
                .toArray(CompletableFuture[]::new);

            try {
                CompletableFuture.allOf(futures).get();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving settings for player " + uuid, e);
            }
        });
    }

    public static @NotNull CompletableFuture<Void> savePlayerTag(UUID uuid, String tagId) {
        playerSettings.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put("selectedTag", tagId != null ? tagId : "");

        return DatabaseManager.saveSetting(uuid, "selectedTag", tagId != null ? tagId : "");
    }

    public static @NotNull Map<UUID, String> getAllPlayerTags() {
        Map<UUID, String> result = new HashMap<>();

        if (DatabaseManager.useMySQL) {
            try (Connection conn = DatabaseManager.dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid, " + SETTING_KEY + " FROM player_settings")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String tagId = rs.getString(SETTING_KEY);
                        if (tagId != null && !tagId.isEmpty()) {
                            result.put(uuid, tagId);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch player tags from MySQL", e);
            }
        } else {
            if (DatabaseManager.settingsConfig == null) {
                plugin.getLogger().severe("Settings configuration is not initialized.");
                return result;
            }

            ConfigurationSection playersSection = DatabaseManager.settingsConfig.getConfigurationSection("players");
            if (playersSection != null) {
                for (String uuidStr : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        String tagId = DatabaseManager.settingsConfig.getString("players." + uuidStr + "." + SETTING_KEY, "");
                        if (!tagId.isEmpty()) {
                            result.put(uuid, tagId);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in settings file: " + uuidStr);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + result.size() + " player tags from database");
        return result;
    }


    public static boolean isMessagingEnabled(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        return getSettingValue(player, "messaging", true);
    }

    public static boolean isScoreboardEnabled(Player player) {
        return getSettingValue(player, "scoreboard", true);
    }

    public static boolean isPlayersVisible(Player player) {
        return getSettingValue(player, "players_visibility", true);
    }

    public static PlayerTime getPlayerTime(Player player) {
        return getSettingValue(player, "player_time", PlayerTime.DAY);
    }

    public static boolean isActionbarEnabled(Player player) {
        return getSettingValue(player, "actionbar", true);
    }

    public static boolean isPingingEnabled(Player player) {
        return getSettingValue(player, "pinging", true);
    }

    public static boolean isTpaEnabled(Player player) {
        return getSettingValue(player, "tpa", true);
    }

    private static boolean isPvpToggle(String key) {
        return "pvp".equals(key);
    }

    public static boolean isPvpEnabled(Player player) {
        return getSettingValue(player, "pvp", false);
    }
}

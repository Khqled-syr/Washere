package me.washeremc.Core.Settings;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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


    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + " &cseconds before toggling PVP again!"));
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
                case "scoreboard" -> toggleScoreboard(player, (Boolean) value);
                case "players_visibility" -> updatePlayerVisibility(player, (Boolean) value);
                case "player_time" -> applyPlayerTime(player, (PlayerTime) value);
                case "pvp" -> notifyPvpStatus(player, (Boolean) value);
                default -> plugin.getLogger().warning("Unknown setting key: " + key);
            }
        } catch (ClassCastException e) {
            plugin.getLogger().log(Level.SEVERE, "Invalid value type for setting " + key, e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error applying setting " + key, e);
        }
    }


    private static void notifyPvpStatus(Player player, boolean enabled) {
        if (enabled) {
            player.sendMessage(ChatUtils.colorize("&aYou have enabled PVP. You can now attack and be attacked by other players."));
        } else {
            player.sendMessage(ChatUtils.colorize("&cYou have disabled PVP. You cannot attack or be attacked by other players."));
        }
    }

    private static void toggleScoreboard(Player player, boolean enabled) {
        if (enabled) plugin.getScoreboard().createSidebar(player);
        else plugin.getScoreboard().removeSidebar(player);
    }

    private static void updatePlayerVisibility(Player player, boolean visible) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (visible) player.showPlayer(plugin, online);
            else player.hidePlayer(plugin, online);
        }
    }

    private static void applyPlayerTime(@NotNull Player player, @NotNull PlayerTime time) {
        player.setPlayerTime(switch (time) {
            case DAY -> 1000L;
            case NIGHT -> 13000L;
            case SUNSET -> 12000L;
        }, false);
    }

    public static void loadPlayerSettings(UUID uuid) {
        Map<String, Object> settings = new ConcurrentHashMap<>();
        Map<String, CompletableFuture<?>> futures = new HashMap<>();

        for (Setting<?> setting : SettingRegistry.getSettings()) {
            String key = setting.getKey();
            Object defaultValue = setting.getDefaultValue();

            futures.put(key, DatabaseManager.loadSetting(uuid, key, defaultValue)
                .thenAccept(value -> settings.put(key, value))
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Error loading setting: " + key, e);
                    settings.put(key, defaultValue);
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

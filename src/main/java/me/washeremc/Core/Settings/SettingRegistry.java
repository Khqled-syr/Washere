package me.washeremc.Core.Settings;


import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SettingRegistry {
    private static final Map<String, Setting<?>> settings = new HashMap<>();

    static {
        registerSetting(new Setting<>("scoreboard", true, "Scoreboard", value -> !value));
        registerSetting(new Setting<>("messaging", true, "Messaging", value -> !value));
        registerSetting(new Setting<>("pinging", true, "Pinging", value -> !value));
        registerSetting(new Setting<>("tpa", true, "TPA", value -> !value));
        registerSetting(new Setting<>("actionbar", true, "Action Bar", value -> !value));
        registerSetting(new Setting<>("players_visibility", true, "Players Visibility", value -> !value));
        registerSetting(new Setting<>("pvp", false, "PVP", value -> !value));
        registerSetting(new Setting<>("selectedTag", "", "Selected Tag", value -> value));

        registerSetting(new Setting<>("player_time", PlayerTime.DAY, "Player Time", value -> {
            if (value == PlayerTime.DAY) return PlayerTime.NIGHT;
            if (value == PlayerTime.NIGHT) return PlayerTime.SUNSET;
            return PlayerTime.DAY;
        }));
    }

    public static <T> void registerSetting(Setting<T> setting) {
        settings.put(setting.getKey(), setting);
    }

    @SuppressWarnings("unchecked")
    public static <T> Setting<T> getSetting(String key) {
        return (Setting<T>) settings.get(key);
    }

    @Contract(pure = true)
    public static @NotNull Collection<Setting<?>> getSettings() {
        return settings.values();
    }
}
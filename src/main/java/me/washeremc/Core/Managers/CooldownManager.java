package me.washeremc.Core.Managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public static void setCooldown(UUID uuid, String key, int seconds) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, System.currentTimeMillis() + (seconds * 1000L));
    }

    public static boolean isOnCooldown(UUID uuid, String key) {
        return getRemainingTime(uuid, key) > 0;
    }

    public static long getRemainingTime(UUID uuid, String key) {
        return Math.max(0, (cooldowns.getOrDefault(uuid, new HashMap<>()).getOrDefault(key, 0L) - System.currentTimeMillis()) / 1000);
    }
}
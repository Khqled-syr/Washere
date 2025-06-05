
package me.washeremc.Core.PlayerTime;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.washeremc.Washere;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayTimeTrackerExpansion extends PlaceholderExpansion {
    private final Washere plugin;
    private final PlayTimeTracker playTimeTracker;
    private final Map<String, String> placeholderCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCacheUpdate = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 30000L;

    public PlayTimeTrackerExpansion(Washere plugin, PlayTimeTracker playTimeTracker) {
        this.plugin = plugin;
        this.playTimeTracker = playTimeTracker;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playertime";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        String cacheKey = uuid + "_" + params.toLowerCase();
        long currentTime = System.currentTimeMillis();

        Long lastUpdate = lastCacheUpdate.get(uuid);
        if (lastUpdate != null && (currentTime - lastUpdate) < CACHE_DURATION) {
            String cached = placeholderCache.get(cacheKey);
            if (cached != null) return cached;
        }

        String result = switch (params.toLowerCase()) {
            case "hours" -> String.valueOf(playTimeTracker.getPlayerTime(uuid));
            case "hours_formatted" -> formatTime(playTimeTracker.getPlayerTime(uuid));
            case "full_format" -> formatTimeFull(playTimeTracker.getPlayerTime(uuid), playTimeTracker.getPlayerMinutes(uuid));
            default -> null;
        };

        if (result != null) {
            placeholderCache.put(cacheKey, result);
            lastCacheUpdate.put(uuid, currentTime);
        }

        return result;
    }

    @Contract(pure = true)
    private @NotNull String formatTime(long hours) {
        if (hours < 24) {
            return hours + " hours";
        }
        long days = hours / 24;
        long remainingHours = hours % 24;
        return days + " days" + (remainingHours > 0 ? ", " + remainingHours + " hours" : "");
    }

    @Contract(pure = true)
    private @NotNull String formatTimeFull(long hours, long minutes) {
        if (hours < 1) {
            return minutes + " minutes";
        } else if (hours < 24) {
            return hours + " hours, " + minutes + " minutes";
        }
        long days = hours / 24;
        long remainingHours = hours % 24;
        return days + " days, " + remainingHours + " hours, " + minutes + " minutes";
    }

    public void clearPlayerCache(UUID uuid) {
        lastCacheUpdate.remove(uuid);
        placeholderCache.entrySet().removeIf(entry -> entry.getKey().startsWith(uuid.toString()));
    }
}
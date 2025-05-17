package me.washeremc.SERVERMODE.survival.TPA;

import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final Washere plugin;

    private static final long REQUEST_EXPIRATION_TIME = 30000; // 30 seconds
    private static final long COOLDOWN_DURATION = 30000; // 30 seconds
    private static final long CLEANUP_INTERVAL = 60000; // 60 seconds

    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> expirationTasks = new ConcurrentHashMap<>();

    public TpaManager(Washere plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        }.runTaskTimer(plugin, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }


    public UUID getRequest(UUID targetId) {
        return tpaRequests.get(targetId);
    }

    public boolean hasRequest(UUID targetId) {
        return tpaRequests.containsKey(targetId);
    }

    public boolean canAcceptRequest(UUID targetId) {
        if (!hasRequest(targetId)) return false;

        Long requestTime = requestTimestamps.get(targetId);
        if (requestTime == null) return false;

        return System.currentTimeMillis() - requestTime < REQUEST_EXPIRATION_TIME;
    }

    public void addRequest(UUID senderId, UUID targetId) {
        Objects.requireNonNull(senderId, "senderId cannot be null");
        Objects.requireNonNull(targetId, "targetId cannot be null");

        synchronized (this) {
            if (hasRequest(targetId)) {
                throw new IllegalStateException("Target already has an active request");
            }

            tpaRequests.put(targetId, senderId);
            requestTimestamps.put(targetId, System.currentTimeMillis());

            BukkitRunnable expirationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (hasRequest(targetId)) {
                        sendExpirationMessages(senderId, targetId);
                        removeRequest(targetId);
                    }
                }
            };

            expirationTasks.put(targetId, expirationTask);
            expirationTask.runTaskLater(plugin, REQUEST_EXPIRATION_TIME / 50L); // Convert to ticks
        }
    }


    private void sendExpirationMessages(UUID senderId, UUID targetId) {
        Player senderPlayer = Bukkit.getPlayer(senderId);
        Player targetPlayer = Bukkit.getPlayer(targetId);

        if (senderPlayer != null && targetPlayer != null) {
            senderPlayer.sendMessage(ChatUtils.colorize("&cYour teleport request to " + targetPlayer.getName() + " has expired."));
            targetPlayer.sendMessage(ChatUtils.colorize("&cThe teleport request from " + senderPlayer.getName() + " has expired."));
        } else if (senderPlayer != null) {
            senderPlayer.sendMessage(ChatUtils.colorize("&cYour teleport request has expired."));
        } else if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatUtils.colorize("&cA teleport request to you has expired."));
        }
    }


    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        requestCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() >= COOLDOWN_DURATION);
    }

    public BukkitRunnable getExpirationTask(UUID targetId) {
        return expirationTasks.get(targetId);
    }

    public void removeRequest(UUID targetId) {
        tpaRequests.remove(targetId);
        requestTimestamps.remove(targetId);
        BukkitRunnable task = expirationTasks.remove(targetId);
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isOnCooldown(UUID senderId) {
        if (senderId == null) return false;
        return requestCooldowns.containsKey(senderId) &&
                System.currentTimeMillis() - requestCooldowns.get(senderId) < COOLDOWN_DURATION;
    }

    public void setCooldown(UUID senderId) {
        requestCooldowns.put(senderId, System.currentTimeMillis());
    }

    public long getCooldownRemaining(UUID senderId) {
        if (isOnCooldown(senderId)) {
            return (COOLDOWN_DURATION - (System.currentTimeMillis() - requestCooldowns.get(senderId))) / 1000;
        }
        return 0;
    }
}
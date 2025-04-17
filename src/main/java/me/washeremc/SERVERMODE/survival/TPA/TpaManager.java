package me.washeremc.SERVERMODE.survival.TPA;


import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {

    private final Washere plugin;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, Long> requestTimestamps = new HashMap<>();
    private final Map<UUID, Long> requestCooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> expirationTasks = new HashMap<>();

    private static final long REQUEST_EXPIRATION_TIME = 30000;

    public TpaManager(Washere plugin) {
        this.plugin = plugin;
    }

    public UUID getRequest(UUID targetId) {
        return tpaRequests.get(targetId);
    }

    public boolean hasRequest(UUID targetId) {
        return tpaRequests.containsKey(targetId);
    }


    public void addRequest(UUID senderId, UUID targetId) {
        tpaRequests.put(targetId, senderId);
        requestTimestamps.put(targetId, System.currentTimeMillis());

        BukkitRunnable expirationTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long requestTime = requestTimestamps.getOrDefault(targetId, currentTime);

                if (currentTime - requestTime >= REQUEST_EXPIRATION_TIME) {
                    plugin.getLogger().info("Expiration task triggered for request: " + targetId);

                    sendExpirationMessages(senderId, targetId);
                    removeRequest(targetId);
                }
            }
        };

        expirationTasks.put(targetId, expirationTask);
        expirationTask.runTaskLater(plugin, REQUEST_EXPIRATION_TIME / 50L);
    }

    private void sendExpirationMessages(UUID senderId, UUID targetId) {
        Player senderPlayer = Bukkit.getPlayer(senderId);
        Player targetPlayer = Bukkit.getPlayer(targetId);

        if (senderPlayer != null) {
            assert targetPlayer != null;
            senderPlayer.sendMessage(ChatUtils.colorize("&cYour teleport request to " + targetPlayer.getName() + " has expired."));
        }

        if (targetPlayer != null) {
            assert senderPlayer != null;
            targetPlayer.sendMessage(ChatUtils.colorize("&cThe teleport request from " + senderPlayer.getName() + " has expired."));
        }
    }

    public BukkitRunnable getExpirationTask(UUID targetId) {
        return expirationTasks.get(targetId);
    }

    public void removeRequest(UUID targetId) {
        tpaRequests.remove(targetId);
        requestTimestamps.remove(targetId);

        BukkitRunnable expirationTask = expirationTasks.remove(targetId);
        if (expirationTask != null) {
            expirationTask.cancel();  // Ensure this task is removed
        }
    }

    public boolean isOnCooldown(UUID senderId) {
        return requestCooldowns.containsKey(senderId) &&
                System.currentTimeMillis() - requestCooldowns.get(senderId) < 30000; // 30 seconds cooldown
    }

    public void setCooldown(UUID senderId) {
        requestCooldowns.put(senderId, System.currentTimeMillis());
    }

    public long getCooldownRemaining(UUID senderId) {
        if (isOnCooldown(senderId)) {
            return (30000 - (System.currentTimeMillis() - requestCooldowns.get(senderId))) / 1000;
        }
        return 0;
    }
}
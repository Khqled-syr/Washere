package me.washeremc.SERVERMODE.survival.utils;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarTask extends BukkitRunnable {
    private final Washere plugin;
    private static final double MAX_TARGET_DISTANCE = 30.0;
    private final Map<UUID, Long> temporaryMessages = new ConcurrentHashMap<>();

    public ActionBarTask(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public void setTemporaryMessage(@NotNull Player player, long durationMs) {
        temporaryMessages.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
    }

    @Override
    public void run() {
        if (isLobby()) return;

        long currentTime = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (!SettingsManager.isActionbarEnabled(player)) {
                ActionBarUtils.hideActionBar(player);
                continue;
            }

            Long expireTime = temporaryMessages.get(uuid);
            if (expireTime != null) {
                if (currentTime < expireTime) {
                    continue;
                } else {
                    temporaryMessages.remove(uuid);
                }
            }

            Entity target = player.getTargetEntity((int) MAX_TARGET_DISTANCE);
            if (target instanceof Player targetPlayer) {
                String message = ChatUtils.colorize(String.format("%s &8| &c%.1f❤",
                    targetPlayer.getName(),
                    Math.round(targetPlayer.getHealth() * 2) / 2.0));
                ActionBarUtils.sendActionBar(player, message);
                continue;
            }

            Location loc = player.getLocation();
            String coords = String.format("X: %d, Y: %d, Z: %d",
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());

            ActionBarUtils.sendActionBar(player, coords);
        }

        temporaryMessages.entrySet().removeIf(entry -> currentTime >= entry.getValue());
    }
}

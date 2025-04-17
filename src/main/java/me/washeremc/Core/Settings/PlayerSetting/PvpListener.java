package me.washeremc.Core.Settings.PlayerSetting;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvpListener implements Listener {
    private final @NotNull Washere plugin;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 2000L; // 2 seconds cooldown
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    public PvpListener(@NotNull Washere plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!"survival".equalsIgnoreCase(plugin.getServerType())) {
            return;
        }

        boolean attackerPvpEnabled = SettingsManager.isPvpEnabled(attacker);
        boolean victimPvpEnabled = SettingsManager.isPvpEnabled(victim);

        if (!attackerPvpEnabled || !victimPvpEnabled) {
            event.setCancelled(true);
            
            if (canSendMessage(attacker.getUniqueId())) {
                if (!attackerPvpEnabled) {
                    sendActionBarMessage(attacker, ChatUtils.colorize("&cYou have PVP disabled! Enable it in /settings"));
                } else {
                    sendActionBarMessage(attacker, ChatUtils.colorize("&c" + victim.getName() + " has PVP disabled!"));
                }
            }

            if (!victimPvpEnabled && attackerPvpEnabled && canSendMessage(victim.getUniqueId())) {
                sendActionBarMessage(victim, ChatUtils.colorize("&c" + attacker.getName() + " tried to attack you! (PVP is disabled)"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        BukkitTask task = activeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        lastMessageTime.remove(playerId);
    }

    private boolean canSendMessage(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(playerId, 0L);
        
        if (currentTime - lastTime >= MESSAGE_COOLDOWN) {
            lastMessageTime.put(playerId, currentTime);
            return true;
        }
        return false;
    }

    private void sendActionBarMessage(@NotNull Player player, String message) {
        // Cancel any existing task for this player
        BukkitTask existingTask = activeTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Send initial message
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));

        // Schedule repeated messages
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }, 20L, 20L);

        // Schedule task cancellation
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            activeTasks.remove(player.getUniqueId());
        }, 60L); // 3 seconds (60 ticks)

        activeTasks.put(player.getUniqueId(), task);
    }
}
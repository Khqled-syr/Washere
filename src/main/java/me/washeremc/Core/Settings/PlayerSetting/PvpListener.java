package me.washeremc.Core.Settings.PlayerSetting;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import me.washeremc.SERVERMODE.survival.utils.ActionBarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
    private final ActionBarManager actionBarManager; // Add this
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 2000L;
    private static final long PVP_MESSAGE_DURATION = 4000L; // 4 seconds

    public PvpListener(@NotNull Washere plugin, ActionBarManager actionBarManager) {
        this.plugin = plugin;
        this.actionBarManager = actionBarManager;
    }

    @EventHandler
    public void onPlayerDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) return;

        if (!"survival".equalsIgnoreCase(plugin.getServerType())) return;

        boolean attackerPvp = SettingsManager.isPvpEnabled(attacker);
        boolean victimPvp = SettingsManager.isPvpEnabled(victim);

        if (!attackerPvp || !victimPvp) {
            event.setCancelled(true);
            if (canSendMessage(attacker.getUniqueId())) {
                String message = !attackerPvp
                        ? "&cYou have PVP disabled! enable it from your settings."
                        : "&c" + victim.getName() + " has PVP disabled!";
                sendActionBarMessage(attacker, ChatUtils.colorize(message));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cancelTask(playerId);
        lastMessageTime.remove(playerId);
    }

    private boolean canSendMessage(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerId);
        if (lastTime == null || currentTime - lastTime >= MESSAGE_COOLDOWN) {
            lastMessageTime.put(playerId, currentTime);
            return true;
        }
        return false;
    }

    private void sendActionBarMessage(@NotNull Player player, String message) {
        cancelTask(player.getUniqueId());

        actionBarManager.setTemporaryMessage(player, PVP_MESSAGE_DURATION);

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        player.sendActionBar(component);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> player.sendActionBar(component), 20L, 20L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            activeTasks.remove(player.getUniqueId());
        }, 80L);

        activeTasks.put(player.getUniqueId(), task);
    }

    private void cancelTask(UUID playerId) {
        BukkitTask task = activeTasks.remove(playerId);
        if (task != null) task.cancel();
    }
}
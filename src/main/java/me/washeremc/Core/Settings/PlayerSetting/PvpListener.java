package me.washeremc.Core.Settings.PlayerSetting;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
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
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 2000L;

    public PvpListener(@NotNull Washere plugin) {
        this.plugin = plugin;
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
                        ? "&cYou have PVP disabled! Enable it in /settings"
                        : "&c" + victim.getName() + " has PVP disabled!";
                sendActionBarMessage(attacker, ChatUtils.colorize(message));
            }
            if (!victimPvp && attackerPvp && canSendMessage(victim.getUniqueId())) {
                sendActionBarMessage(victim, ChatUtils.colorize("&c" + attacker.getName() + " tried to attack you! (PVP is disabled)"));
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
        return currentTime - lastMessageTime.getOrDefault(playerId, 0L) >= MESSAGE_COOLDOWN
                && lastMessageTime.put(playerId, currentTime) == null;
    }

    private void sendActionBarMessage(@NotNull Player player, String message) {
        cancelTask(player.getUniqueId());

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        player.sendActionBar(component);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> player.sendActionBar(component), 20L, 20L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            activeTasks.remove(player.getUniqueId());
        }, 60L);

        activeTasks.put(player.getUniqueId(), task);
    }

    private void cancelTask(UUID playerId) {
        BukkitTask task = activeTasks.remove(playerId);
        if (task != null) task.cancel();
    }
}
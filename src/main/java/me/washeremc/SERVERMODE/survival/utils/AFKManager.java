package me.washeremc.SERVERMODE.survival.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager implements Listener {
    private final Washere plugin;
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Boolean> isAFK = new HashMap<>();
    private static final long AFK_TIME = 60_000; // 1 minute (was incorrectly set to 6 minutes)
    private final String name = "%washere_tag_prefix%%luckperms_prefix%%player_displayname%%washere_tag_suffix%";

    private boolean isSurvival() {
        return "survival".equalsIgnoreCase(plugin.getServerType());
    }

    public AFKManager(Washere plugin) {
        this.plugin = plugin;
        if(!isSurvival()) {
            plugin.getLogger().info("AFK system not initialized - not in survival mode.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        }
        startAFKChecker();
        plugin.getLogger().info("AFK system initialized.");
    }

    private void resetAFK(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (isAFK.getOrDefault(uuid, false)) {
            isAFK.put(uuid, false);
            Bukkit.broadcast(ChatUtils.colorizeMini(PlaceholderAPI.setPlaceholders(player, name) + " &7is no longer AFK."));
        }
    }

    private void startAFKChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (!lastActivity.containsKey(uuid)) {
                        lastActivity.put(uuid, now);
                        continue;
                    }

                    long lastActive = lastActivity.get(uuid);

                    if (!isAFK.getOrDefault(uuid, false) && (now - lastActive) >= AFK_TIME) {
                        isAFK.put(uuid, true);
                        Bukkit.broadcast(ChatUtils.colorizeMini(PlaceholderAPI.setPlaceholders(player, name) + " &7is now AFK."));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 5);
    }

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if(isSurvival() &&
                (event.getFrom().getX() != event.getTo().getX() ||
                        event.getFrom().getY() != event.getTo().getY() ||
                        event.getFrom().getZ() != event.getTo().getZ())) {
            resetAFK(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerChat(@NotNull AsyncChatEvent event) {
        if (isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
        if (isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        if (isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
        if (isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isSurvival()) {
            resetAFK(player);
        }
    }
    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (isSurvival()) {
            UUID uuid = event.getPlayer().getUniqueId();
            lastActivity.remove(uuid);
            isAFK.remove(uuid);
        }
    }
    @EventHandler
    public void onPlayerKick(@NotNull PlayerKickEvent event) {
        if (isSurvival()) {
            UUID uuid = event.getPlayer().getUniqueId();
            lastActivity.remove(uuid);
            isAFK.remove(uuid);
        }
    }
    @EventHandler
    public void onPlayerLogin(@NotNull PlayerLoginEvent event) {
        if (isSurvival()) {
            UUID uuid = event.getPlayer().getUniqueId();
            lastActivity.put(uuid, System.currentTimeMillis());
            isAFK.put(uuid, false);
        }
    }
    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (isSurvival()) {
            UUID uuid = event.getPlayer().getUniqueId();
            lastActivity.put(uuid, System.currentTimeMillis());
            isAFK.put(uuid, false);
        }
    }
}
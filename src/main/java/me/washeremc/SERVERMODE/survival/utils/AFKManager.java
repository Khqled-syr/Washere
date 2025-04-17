package me.washeremc.SERVERMODE.survival.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private static final long AFK_TIME = 360_000; // 1 minute

    private boolean isSurvival() {
        return "survival".equalsIgnoreCase(plugin.getServerType());
    }

    public AFKManager(Washere plugin) {
        this.plugin = plugin;

        if(!isSurvival()) {
            return;
        }
        startAFKChecker();
    }

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if(isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if(isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        lastActivity.remove(event.getPlayer().getUniqueId());
        isAFK.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(@NotNull AsyncChatEvent event) {
        if(isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if(isSurvival()) {
            resetAFK(event.getPlayer());
        }
    }

    private void resetAFK(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (isAFK.getOrDefault(uuid, false)) {
            isAFK.put(uuid, false);
            Bukkit.broadcast(Component.text("§7" + player.getName() + " is no longer AFK."));
        }
    }

    private void startAFKChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    long lastActive = lastActivity.getOrDefault(uuid, now);

                    if (!isAFK.getOrDefault(uuid, false) && (now - lastActive) >= AFK_TIME) {
                        isAFK.put(uuid, true);
                        Bukkit.broadcast(Component.text("§7" + player.getName() + " is now AFK."));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 5); // Check every 5 seconds
    }
}

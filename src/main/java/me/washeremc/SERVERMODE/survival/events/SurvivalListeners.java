package me.washeremc.SERVERMODE.survival.events;


import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.libs.kyori.adventure.text.Component;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Washere;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static me.washeremc.Core.utils.ChatUtils.colorize;


public class SurvivalListeners implements Listener {

    private final Washere plugin;

    public SurvivalListeners(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @EventHandler
    public void OnPlayerJoin(@NotNull PlayerJoinEvent event) {

        Player player = event.getPlayer();
        plugin.getScoreboard().setPlayerTeams(Objects.requireNonNull(player.getPlayer()));

        try {
            plugin.getTabList().setTabList(player);
            plugin.getTabList().updatePlayerListNames();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update tablist for " + player.getName() + ": " + e.getMessage());
        }
        if (SettingsManager.isScoreboardEnabled(player)) {
            plugin.getScoreboard().createSidebar(player);
        } else {
            plugin.getScoreboard().removeSidebar(player);
        }

        if (isLobby()) return;
        event.joinMessage(null);
        String broadcastMessage = plugin.getConfig().getString("join-message");
        assert broadcastMessage != null;
        broadcastMessage = PlaceholderAPI.setPlaceholders(player, broadcastMessage);

        broadcastMessage = broadcastMessage.replace("%player%", player.getName());
        //player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcastMessage));

        if (!player.hasPlayedBefore()) {
                player.sendMessage("");
                player.performCommand("washere:help");
            }
        }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (isLobby()) return;

        Player player = event.getPlayer();
        event.quitMessage(null);
        String broadcastMessage = plugin.getConfig().getString("leave-message");
        assert broadcastMessage != null;
        broadcastMessage = PlaceholderAPI.setPlaceholders(player, broadcastMessage);

        broadcastMessage = broadcastMessage.replace("%player%", player.getName());
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize((broadcastMessage)));

    }

    @EventHandler
    public void onPlayerBedEnter(@NotNull PlayerBedEnterEvent event) {
        if (isLobby()) return;
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Objects.requireNonNull(Bukkit.getWorld("world")).setTime(0);
                Objects.requireNonNull(Bukkit.getWorld("world")).setStorm(false);
                Objects.requireNonNull(Bukkit.getWorld("world")).setThundering(false);
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e" + event.getPlayer().getName() + "§7 has slept. Good morning everyone!"));
            }, 100L);
        }
    }

    @EventHandler
    public void onPlayerHit(@NotNull EntityDamageByEntityEvent event) {
        if (isLobby()) return;
        if (event.getDamager() instanceof Player damager) {
            Entity damaged = event.getEntity();

            if (damaged instanceof Player damagedPlayer) {

                double healthAfterDamage = damagedPlayer.getHealth() - event.getDamage();

                if (healthAfterDamage <= 0.5) {
                    event.setCancelled(true);
                    damaged.sendMessage(colorize("&c" + damager.getName() + " tried to kill you!"));
                    damager.sendMessage(colorize("&cYou can't hit them, they're gonna die LOL!"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (isLobby()) return;
        Player player = event.getEntity();
        String deathMessage = colorize("&c" + player.getName() + " died at X: " +
                player.getLocation().getBlockX() + " Y: " + player.getLocation().getBlockY() + " Z: " +
                player.getLocation().getBlockZ());

        EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent instanceof EntityDamageByEntityEvent entityDamageEvent) {
            Entity damager = entityDamageEvent.getDamager();
            if (damager instanceof Player) {
                deathMessage += " by " + ((Player) damager).displayName();
            } else if (damager instanceof LivingEntity) {
                deathMessage += " by a " + damager.getType().name().toLowerCase();
            }
        } else {
            assert damageEvent != null;
            deathMessage += " (" + damageEvent.getCause().name().toLowerCase() + ")";
        }
        event.deathMessage((net.kyori.adventure.text.Component) Component.text(deathMessage));

    }
}

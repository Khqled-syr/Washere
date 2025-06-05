package me.washeremc.Core.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TabList {

    private final Washere plugin;
    private int taskId = -1;
    private final Map<UUID, Component> lastHeaders = new HashMap<>();
    private final Map<UUID, Component> lastFooters = new HashMap<>();

    public TabList(Washere plugin) {
        this.plugin = plugin;
        startDynamicTabUpdater();
    }

    public void setTabList(@NotNull Player player) {
        String headerText = PlaceholderAPI.setPlaceholders(player, Objects.requireNonNull(plugin.getConfig().getString("tablist.header")));
        String footerText = PlaceholderAPI.setPlaceholders(player, Objects.requireNonNull(plugin.getConfig().getString("tablist.footer")));

        Component header = LegacyComponentSerializer.legacyAmpersand().deserialize(ChatUtils.colorize(headerText));
        Component footer = LegacyComponentSerializer.legacyAmpersand().deserialize(ChatUtils.colorize(footerText));

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    public void updatePlayerListNames() {
        String format = plugin.getConfig().getString("tablist.player-list-name-format", "%luckperms_prefix% %player_name%");

        Bukkit.getOnlinePlayers().forEach(player -> {
            String formattedName = PlaceholderAPI.setPlaceholders(player, format);
            Component displayName = LegacyComponentSerializer.legacyAmpersand().deserialize(ChatUtils.colorize(formattedName));
            player.playerListName(displayName);
        });
    }

    private void updatePlayerNameTag(@NotNull Player player) {
        String format = plugin.getConfig().getString("nametag.format", "&7%luckperms_prefix%%player_name%");
        String nameTag = ChatUtils.colorize(PlaceholderAPI.setPlaceholders(player, format));

        player.setDisplayName(nameTag);
        player.setCustomName(nameTag);
        player.setCustomNameVisible(plugin.getConfig().getBoolean("nametag.visible", true));
    }

    public boolean isUpdaterRunning() {
        return taskId != -1 && Bukkit.getScheduler().isCurrentlyRunning(taskId);
    }

    public void startDynamicTabUpdater() {
        stopDynamicTabUpdater(); // Always clean up first

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        UUID uuid = player.getUniqueId();
                        String headerText = PlaceholderAPI.setPlaceholders(
                                player,
                                Objects.requireNonNull(plugin.getConfig().getString("tablist.header"))
                        );
                        String footerText = PlaceholderAPI.setPlaceholders(
                                player,
                                Objects.requireNonNull(plugin.getConfig().getString("tablist.footer"))
                        );

                        Component header = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(ChatUtils.colorize(headerText));
                        Component footer = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(ChatUtils.colorize(footerText));

                        // Only update if content changed
                        if (!header.equals(lastHeaders.get(uuid)) ||
                                !footer.equals(lastFooters.get(uuid))) {
                            player.sendPlayerListHeaderAndFooter(header, footer);
                            lastHeaders.put(uuid, header);
                            lastFooters.put(uuid, footer);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error updating tablist for " + player.getName());
                    }
                }
                updatePlayerListNames();
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    public void stopDynamicTabUpdater() {
        if (taskId != -1) {
            try {
                Bukkit.getScheduler().cancelTask(taskId);
            } catch (Exception ignored) {}
            taskId = -1;
        }
        // Clean up caches
        lastHeaders.clear();
        lastFooters.clear();
    }

    public void cleanupPlayer(UUID uuid) {
        lastHeaders.remove(uuid);
        lastFooters.remove(uuid);
    }
}
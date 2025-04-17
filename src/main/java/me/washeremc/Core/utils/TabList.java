package me.washeremc.Core.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TabList {

    private final Washere plugin;
    private int taskId = -1;

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

    private void startDynamicTabUpdater() {
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    stopDynamicTabUpdater();
                    return;
                }
                Bukkit.getOnlinePlayers().forEach(TabList.this::setTabList);
            }
        }.runTaskTimer(plugin, 0L, 100L).getTaskId();
    }

    public void stopDynamicTabUpdater() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
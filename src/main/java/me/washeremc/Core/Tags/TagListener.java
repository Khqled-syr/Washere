package me.washeremc.Core.Tags;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Washere;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TagListener implements Listener {
    private final Washere plugin;

    public TagListener(Washere plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        TagManager.loadPlayerTag(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Tag currentTag = TagManager.getPlayerTag(uuid);
        String tagId = currentTag != null ? currentTag.id() : "";

        SettingsManager.getSettingValue(uuid, "selectedTag", "");
        SettingsManager.savePlayerTag(uuid, tagId).join();

        SettingsManager.savePlayerSettings(uuid);

    }
}
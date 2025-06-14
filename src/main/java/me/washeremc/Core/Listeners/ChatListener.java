package me.washeremc.Core.Listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import me.washeremc.SERVERMODE.survival.Jail.JailManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("ALL")
public class ChatListener implements Listener {

    private final Washere plugin;
    // OPTIMIZED: Cache components to reduce processing
    private final Map<String, Component> componentCache = new HashMap<>();
    private long lastCacheClean = System.currentTimeMillis();
    private static final long CACHE_CLEAN_INTERVAL = 300000; // 5 minutes

    public ChatListener(Washere plugin) {
        this.plugin = plugin;
    }


    public void clearPlayerCache(UUID uuid) {
        componentCache.remove("rank_" + uuid.toString());
        componentCache.keySet().removeIf(key -> key.startsWith("name_" + uuid.toString()));
    }

    @EventHandler
    public void onPlayerChat(@NotNull AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String message = ((TextComponent) event.message()).content();
        UUID uuid = sender.getUniqueId();
        String cooldownKey = "chat";

        clearPlayerCache(uuid);

        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            sender.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore chatting again!"));
            event.setCancelled(true);
            return;
        }

        // OPTIMIZED: Clean cache periodically to prevent memory leaks
        cleanCacheIfNeeded();

        FileConfiguration config = plugin.getConfig();
        String chatFormat = config.getString("chat.format", "&7%luckperms_prefix%%player_name%: &f%message%");

        JailManager jailManager = plugin.getJailManager();
        if (jailManager != null && jailManager.isJailed(uuid)) {
            chatFormat = "&8[Prisoned] " + chatFormat;
        }

        String rankInfoKey = "rank_" + uuid.toString();
        Component hoverComponent = componentCache.get(rankInfoKey);
        if (hoverComponent == null) {
            String rankInfo = PlaceholderAPI.setPlaceholders(sender, "&7Rank: %luckperms_primary_group_name%");
            hoverComponent = ChatUtils.colorizeMini(rankInfo);
            componentCache.put(rankInfoKey, hoverComponent);
        }

        String[] parts = chatFormat.split(":");
        String nameFormat = parts[0];
        String messageFormat = parts.length > 1 ? parts[1] : " &f%message%";

        String nameKey = "name_" + uuid.toString() + "_" + nameFormat.hashCode();
        Component nameComponent = componentCache.get(nameKey);
        if (nameComponent == null) {
            String namePartProcessed = PlaceholderAPI.setPlaceholders(sender, nameFormat);
            nameComponent = ChatUtils.colorizeMini(namePartProcessed)
                    .hoverEvent(HoverEvent.showText(hoverComponent));
            componentCache.put(nameKey, nameComponent);
        }

        String messagePartProcessed = PlaceholderAPI.setPlaceholders(sender, messageFormat).replace("%message%", message);
        Component messageComponent = ChatUtils.colorizeMini(messagePartProcessed);

        Component fullMessage = nameComponent.append(Component.text(":")).append(messageComponent);
        CooldownManager.setCooldown(uuid, cooldownKey, 2);

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (message.contains(recipient.getName()) && !recipient.equals(sender)) {
                if (SettingsManager.isPingingEnabled(recipient)) {
                    String highlightedMessage = messagePartProcessed.replace(recipient.getName(),
                            ChatUtils.colorize("&b&n" + recipient.getName() + "&r"));
                    Component highlightedComponent = ChatUtils.colorizeMini(highlightedMessage);
                    Component personalizedMessage = nameComponent.append(Component.text(":")).append(highlightedComponent);

                    recipient.sendMessage(personalizedMessage);
                    recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else {
                    recipient.sendMessage(fullMessage);
                }
            } else {
                recipient.sendMessage(fullMessage);
            }
        }
        event.setCancelled(true);
    }

    private void cleanCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheClean > CACHE_CLEAN_INTERVAL) {
            componentCache.clear();
            lastCacheClean = currentTime;
        }
    }
}
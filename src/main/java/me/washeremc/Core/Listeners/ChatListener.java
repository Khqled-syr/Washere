package me.washeremc.Core.Listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
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

import java.util.UUID;

@SuppressWarnings("ALL")
public class ChatListener implements Listener {

    private final Washere plugin;

    public ChatListener(Washere plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(@NotNull AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String message = ((TextComponent) event.message()).content();
        UUID uuid = sender.getUniqueId();
        String cooldownKey = "chat";

        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            sender.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore chatting again!"));
            event.setCancelled(true);
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String chatFormat = config.getString("chat.format", "&7%luckperms_prefix%%player_name%: &f%message%");

        String rankInfo = PlaceholderAPI.setPlaceholders(sender, "&7Rank: %luckperms_primary_group_name%");

        Component hoverComponent = ChatUtils.colorizeMini(rankInfo);

        String[] parts = chatFormat.split(":");
        String nameFormat = parts[0];
        String messageFormat = parts.length > 1 ? parts[1] : " &f%message%";

        String namePartProcessed = PlaceholderAPI.setPlaceholders(sender, nameFormat);
        Component nameComponent = ChatUtils.colorizeMini(namePartProcessed)
                .hoverEvent(HoverEvent.showText(hoverComponent));

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
}
package me.washeremc.Core.Listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("ALL")
public class ChatListener implements Listener {

    private final Washere plugin;

    public ChatListener(Washere plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(@NotNull AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        UUID uuid = sender.getUniqueId();
        String cooldownKey = "chat";

        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            sender.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore chatting again!"));
            event.setCancelled(true);
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String chatFormat = config.getString("chat.format", "&7%luckperms_prefix% %player_name%: &f%message%");
        chatFormat = PlaceholderAPI.setPlaceholders(sender, chatFormat);
        String formattedMessage = ChatUtils.colorize(chatFormat.replace("%message%", message));

        CooldownManager.setCooldown(uuid, cooldownKey, 2);

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (message.contains(recipient.getName()) && !recipient.equals(sender)) {
                if (SettingsManager.isPingingEnabled(recipient)) {
                    String highlightedMessage = message.replace(recipient.getName(), "§b§n" + recipient.getName() + "§r");
                    String personalizedMessage = ChatUtils.colorize(chatFormat.replace("%message%", highlightedMessage));

                    recipient.sendMessage(personalizedMessage);
                    recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else {
                    recipient.sendMessage(formattedMessage);
                }
            } else {
                recipient.sendMessage(formattedMessage);
            }
        }
        event.setCancelled(true);
    }
}
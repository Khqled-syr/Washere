package me.washeremc.Core.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class WhoisCommand implements CommandExecutor {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command!"));
            return true;
        }

        if (!player.hasPermission("washere.admin")) {
            player.sendMessage(ChatUtils.colorize("&cYou do not have permission to use this command!"));
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "whois";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length != 1) {
            player.sendMessage(ChatUtils.colorize("&cUsage: /whois <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || (!target.isOnline() && target.getFirstPlayed() == 0)) {
            player.sendMessage(ChatUtils.colorize("&cPlayer not found!"));
            return true;
        }

        player.sendMessage(ChatUtils.colorize("&8&m----------------------------------------"));
        player.sendMessage(ChatUtils.colorize("&e&lPlayer Information for " + target.getName()));
        player.sendMessage(ChatUtils.colorize("&8&m----------------------------------------"));

        sendCopyableInfo(player, "Name", target.getName() != null ? target.getName() : "Unknown");
        sendCopyableInfo(player, "UUID", target.getUniqueId().toString());

        if (target.getFirstPlayed() > 0) {
            sendCopyableInfo(player, "First Joined", dateFormat.format(new Date(target.getFirstPlayed())));
        }

        if (target.getLastLogin() > 0) {
            sendCopyableInfo(player, "Last Seen", dateFormat.format(Date.from(Instant.ofEpochMilli(target.getLastLogin()))));
        }

        player.sendMessage(ChatUtils.colorize("&6Online Status: " + (target.isOnline() ? "&aOnline" : "&cOffline")));

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                String ip = onlineTarget.getAddress() != null ? onlineTarget.getAddress().getAddress().getHostAddress() : "Unknown";
                sendCopyableInfo(player, "IP Address", ip);

                double maxHealth = onlineTarget.getAttribute(Attribute.MAX_HEALTH) != null ?
                        Objects.requireNonNull(onlineTarget.getAttribute(Attribute.MAX_HEALTH)).getValue() : 20.0;

                player.sendMessage(ChatUtils.colorize("&6Health: &f" + Math.round(onlineTarget.getHealth()) + "/" + Math.round(maxHealth)));
                player.sendMessage(ChatUtils.colorize("&6Food Level: &f" + onlineTarget.getFoodLevel() + "/20"));
                player.sendMessage(ChatUtils.colorize("&6XP Level: &f" + onlineTarget.getLevel()));
                sendCopyableInfo(player, "Location",
                        onlineTarget.getWorld().getName() + " (" +
                                Math.round(onlineTarget.getLocation().getX()) + ", " +
                                Math.round(onlineTarget.getLocation().getY()) + ", " +
                                Math.round(onlineTarget.getLocation().getZ()) + ")");

                player.sendMessage(ChatUtils.colorize("&6Gamemode: &f" + onlineTarget.getGameMode()));
                player.sendMessage(ChatUtils.colorize("&6OP Status: " + (onlineTarget.isOp() ? "&aYes" : "&cNo")));
                player.sendMessage(ChatUtils.colorize("&6Flying: " + (onlineTarget.isFlying() ? "&aYes" : "&cNo")));
            }
        }

        player.sendMessage(ChatUtils.colorize("&6Banned: " + (target.isBanned() ? "&cYes" : "&aNo")));
        player.sendMessage(ChatUtils.colorize("&6Whitelisted: " + (target.isWhitelisted() ? "&aYes" : "&cNo")));

        player.sendMessage(ChatUtils.colorize("&8&m----------------------------------------"));
        return true;
    }

    private void sendCopyableInfo(@NotNull Player player, String label, String value) {
        Component labelComponent = Component.text(label + ": ", NamedTextColor.GOLD);
        Component valueComponent = Component.text(value, NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy!", NamedTextColor.YELLOW)))
                .clickEvent(ClickEvent.copyToClipboard(value));

        Component copyButton = Component.text(" [Copy]", NamedTextColor.GRAY, TextDecoration.ITALIC)
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy!", NamedTextColor.YELLOW)))
                .clickEvent(ClickEvent.copyToClipboard(value));

        player.sendMessage(labelComponent.append(valueComponent).append(copyButton));
    }
}

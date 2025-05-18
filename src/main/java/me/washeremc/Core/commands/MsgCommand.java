package me.washeremc.Core.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MsgCommand implements CommandExecutor {

    private final Map<Player, Player> lastMessenger = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command."));
            return true;
        }

        if (!SettingsManager.isMessagingEnabled(player)) {
            player.sendMessage(ChatUtils.colorizeMini("&cMessaging is disabled. Enable it in your settings."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "msg";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length < 2) {
            player.sendMessage(ChatUtils.colorizeMini("&cUsage: /msg <player> <message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatUtils.colorizeMini("&cPlayer not found or not online."));
            return true;
        }

        if (!SettingsManager.isMessagingEnabled(target)) {
            player.sendMessage(ChatUtils.colorizeMini("&cThe player has disabled messaging."));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatUtils.colorizeMini("&cYou cannot message yourself."));
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        target.sendMessage(ChatUtils.colorizeMini( "&e" + player.getName() + " &7-> &6you&7: &b" + message));
        player.sendMessage(ChatUtils.colorizeMini("&6You &7-> &e" + target.getName() + "&7: &b" + message));

        lastMessenger.put(target, player);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return true;
    }

    public Player getLastMessenger(Player player) {
        return lastMessenger.get(player);
    }
    public void setLastMessenger(Player player, Player messenger) {
        lastMessenger.put(player, messenger);
    }
}
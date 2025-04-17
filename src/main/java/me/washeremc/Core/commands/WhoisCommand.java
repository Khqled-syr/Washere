package me.washeremc.Core.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WhoisCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command!"));
            return true;
        }

        // 🔥 Cooldown check
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            player.sendMessage(ChatUtils.colorize("&cPlayer not found!"));
            return true;
        }

        player.sendMessage(ChatUtils.colorize("&eWhois Info:"));
        player.sendMessage(ChatUtils.colorize("&6Player: &f" + target.getName()));
        player.sendMessage(ChatUtils.colorize("&6UUID: &f" + target.getUniqueId()));

        return true;
    }
}

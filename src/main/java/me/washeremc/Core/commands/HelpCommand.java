package me.washeremc.Core.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "help";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        player.sendMessage(ChatUtils.colorizeMini(
                """
                        &b╔════════════════════════════╗
                        &6&l         Help Menu        \s
                        &b╚════════════════════════════╝
                        &a/msg &7- Send a private message.
                        &a/reply &7- Reply to the last private message.
                        &a/settings &7- Open your personal settings menu.
                        &a/profile &7- View your player profile.
                        &a/tags &7- View and manage your tags.
                        &b═════════════════════════════
                        &eTip: Use &a/settings &eto customize your experience!"""
        ));

        return true;
    }
}
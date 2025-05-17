package me.washeremc.Core.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HelpCommand implements CommandExecutor {

    public HelpCommand() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        UUID uuid = player.getUniqueId();
        String cooldownKey = "help";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        TextComponent header = Component.text()
                .append(Component.text("╔════════════════════════════╗", NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("         Help Menu         ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("╚════════════════════════════╝", NamedTextColor.AQUA))
                .append(Component.newline())
                .build();

        TextComponent body = Component.text()
                .append(Component.text("/backpack", NamedTextColor.GREEN))
                .append(Component.text(" - Access your backpack.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/home", NamedTextColor.GREEN))
                .append(Component.text(" - Teleport to your home.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/sethome", NamedTextColor.GREEN))
                .append(Component.text(" - Set your current location as home.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/setwarp", NamedTextColor.GREEN))
                .append(Component.text(" - Create a public warp point.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/warp", NamedTextColor.GREEN))
                .append(Component.text(" - Teleport to a public warp.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/delwarp", NamedTextColor.GREEN))
                .append(Component.text(" - Delete a public warp.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/msg", NamedTextColor.GREEN))
                .append(Component.text(" - Send a private message.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/reply", NamedTextColor.GREEN))
                .append(Component.text(" - Reply to the last private message.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/tpa", NamedTextColor.GREEN))
                .append(Component.text(" - Send a teleport request to another player.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/donate", NamedTextColor.GREEN))
                .append(Component.text(" - Support the server with a donation.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/settings", NamedTextColor.GREEN))
                .append(Component.text(" - Open your personal settings menu.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("/profile", NamedTextColor.GREEN))
                .append(Component.text(" - View your player profile.", NamedTextColor.GRAY))
                .append(Component.newline())
                .build();

        TextComponent footer = Component.text()
                .append(Component.text("═════════════════════════════", NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Tip: Use ", NamedTextColor.YELLOW))
                .append(Component.text("/settings", NamedTextColor.GREEN))
                .append(Component.text(" to customize your experience!", NamedTextColor.YELLOW))
                .build();

        TextComponent helpMenu = Component.text()
                .append(header)
                .append(body)
                .append(footer)
                .build();

        player.sendMessage(helpMenu);
        return true;
    }
}

package me.washeremc.SERVERMODE.survival.TPA;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TpaCommand implements CommandExecutor {

    private final Washere plugin;

    public TpaCommand(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command."));
            return true;
        }

        if (isLobby()) {
            senderPlayer.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        UUID uuid = senderPlayer.getUniqueId();
        String cooldownKey = "tpa";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            sender.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length != 1) {
            sender.sendMessage(ChatUtils.colorizeMini("&cUsage: /tpa <player>"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);

        if (targetPlayer == null) {
            sender.sendMessage(ChatUtils.colorizeMini("&cPlayer not found!"));
            return true;
        }

        if (!SettingsManager.isTpaEnabled(senderPlayer)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cYou have disabled TPA, enable it from the settings!"));
            return true;
        }

        if (!SettingsManager.isTpaEnabled(targetPlayer)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThat player has disabled TPA."));
            return true;
        }

        if (senderPlayer.equals(targetPlayer)) {
            senderPlayer.sendMessage(ChatUtils.colorizeMini("&cYou cannot send a teleport request to yourself!"));
            return true;
        }

        UUID senderId = senderPlayer.getUniqueId();
        UUID targetId = targetPlayer.getUniqueId();

        if (plugin.getTpaManager().isOnCooldown(senderId)) {
            long remainingTime = plugin.getTpaManager().getCooldownRemaining(senderId);
            sender.sendMessage(ChatUtils.colorizeMini("&cPlease wait " + remainingTime + " seconds before sending another request!"));
            return true;
        }

        plugin.getTpaManager().addRequest(senderId, targetId);
        plugin.getTpaManager().setCooldown(senderId);

        Component message = Component.text(senderPlayer.getName() + " has requested to teleport to you. ", NamedTextColor.YELLOW)
                .append(Component.text("[Accept]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/tpaccept"))
                        .hoverEvent(HoverEvent.showText(Component.text("Accept the request", NamedTextColor.GRAY))));

        targetPlayer.sendMessage(message);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        senderPlayer.sendMessage(ChatUtils.colorizeMini("&7Teleport request sent to &e" + targetPlayer.getName()));
        return true;
    }
}
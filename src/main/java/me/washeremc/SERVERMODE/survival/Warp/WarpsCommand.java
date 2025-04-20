package me.washeremc.SERVERMODE.survival.Warp;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public record WarpsCommand(Washere plugin) implements CommandExecutor {


    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command."));
            return true;
        }

        if (isLobby()) {
            player.sendMessage(ChatUtils.colorize("&cThis command is not available in this server."));
            return true;
        }


        if (!WarpManager.isWarpsEnabled()) {
            player.sendMessage(ChatUtils.colorize("&cWarps are currently disabled."));
            return true;
        }

        // 🔥 Cooldown check
        UUID uuid = player.getUniqueId();
        String cooldownKey = "warps";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);



        sendWarpList(player);
        return true;
    }


    private void sendWarpList(@NotNull Player player) {
        Set<String> privateWarps = WarpManager.getWarps(player.getUniqueId());
        Set<String> publicWarps = WarpManager.getPublicWarps();

        player.sendMessage(ChatUtils.colorize("&6=== Available Warps ==="));

        if (!privateWarps.isEmpty()) {
            player.sendMessage(ChatUtils.colorize("&aYour warps: &f" + String.join(", ", privateWarps)));
        }

        if (!publicWarps.isEmpty()) {
            player.sendMessage(ChatUtils.colorize("&aPublic warps: &f" + String.join(", ", publicWarps)));
        }

        if (privateWarps.isEmpty() && publicWarps.isEmpty()) {
            player.sendMessage(ChatUtils.colorize("&cNo warps available."));
        }
    }
}
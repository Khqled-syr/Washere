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

        // 🔥 Cooldown check
        UUID uuid = player.getUniqueId();
        String cooldownKey = "warps";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);


        UUID playerUUID = player.getUniqueId();
        Set<String> warps = WarpManager.getWarps(playerUUID);

        if (warps.isEmpty()) {
            player.sendMessage(ChatUtils.colorize("&cYou have no warps set."));
        } else {
            StringBuilder warpList = new StringBuilder("&aYour warps: ");
            for (String warp : warps) {
                warpList.append(warp).append(", ");
            }
            warpList.setLength(warpList.length() - 2);
            player.sendMessage(ChatUtils.colorize(warpList.toString()));
        }
        return true;
    }
}
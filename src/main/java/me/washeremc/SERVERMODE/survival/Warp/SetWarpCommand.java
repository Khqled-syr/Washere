package me.washeremc.SERVERMODE.survival.Warp;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SetWarpCommand(Washere plugin) implements CommandExecutor {

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
        String cooldownKey = "setwarp";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);


        if (args.length != 1) {
            player.sendMessage(ChatUtils.colorize("&cUsage: /setwarp [name]"));
            return false;
        }

        String warpName = args[0];
        UUID playerUUID = player.getUniqueId();

        if (WarpManager.warpExists(playerUUID, warpName)) {
            player.sendMessage(ChatUtils.colorize("&cYou already have a warp with this name."));
            return true;
        }

        Location location = player.getLocation();
        WarpManager.setWarp(playerUUID, warpName, location);
        player.sendMessage(ChatUtils.colorize("&aWarp " + warpName + " set!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        return true;
    }
}
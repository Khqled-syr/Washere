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

        if (!WarpManager.isWarpsEnabled()) {
            player.sendMessage(ChatUtils.colorize("&cWarps are currently disabled."));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("public")) {
            if (!WarpManager.isPublicWarpsEnabled()) {
                player.sendMessage(ChatUtils.colorize("&cPublic warps are currently disabled."));
                return true;
            }

            if (!player.hasPermission("washere.warp.public")) {
                player.sendMessage(ChatUtils.colorize("&cYou don't have permission to create public warps."));
                return true;
            }

            WarpManager.setPublicWarp(args[1], player.getLocation());
            player.sendMessage(ChatUtils.colorize("&aPublic warp &f" + args[1] + " &ahas been set!"));
            return true;
        }

        UUID uuid = player.getUniqueId();

        int currentWarps = WarpManager.getWarps(uuid).size();
        if (currentWarps >= WarpManager.getMaxWarpsPerPlayer() && !player.hasPermission("washere.warp.bypass")) {
            player.sendMessage(ChatUtils.colorize("&cYou have reached your maximum warp limit!"));
            return true;
        }

        String cooldownKey = "setwarp";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey) && !player.hasPermission("washere.warp.bypass")) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length != 1) {
            player.sendMessage(ChatUtils.colorize("&cUsage: /setwarp [name]"));
            if (player.hasPermission("washere.warp.public")) {
                player.sendMessage(ChatUtils.colorize("&eUse &f/setwarp public <name> &eto create a public warp"));
            }
            return false;
        }

        String warpName = args[0];

        if (WarpManager.warpExists(uuid, warpName)) {
            player.sendMessage(ChatUtils.colorize("&cYou already have a warp with this name."));
            return true;
        }

        Location location = player.getLocation();
        WarpManager.setWarp(uuid, warpName, location);
        player.sendMessage(ChatUtils.colorize("&aWarp " + warpName + " set!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        return true;
    }
}
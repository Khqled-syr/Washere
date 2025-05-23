package me.washeremc.SERVERMODE.survival.Warp;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class WarpCommand implements CommandExecutor {
    private final Washere plugin;

    public WarpCommand(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command."));
            return true;
        }

        if (isLobby()) {
            player.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        if (!WarpManager.isWarpsEnabled()) {
            player.sendMessage(ChatUtils.colorizeMini("&cWarps are currently disabled."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (args.length == 0) {
            sendWarpList(player);
            return true;
        }

        String warpName = args[0];
        Location location = WarpManager.getWarp(uuid, warpName);

        if (location == null) {
            location = WarpManager.getPublicWarp(warpName);
        }

        if (location == null) {
            player.sendMessage(ChatUtils.colorize("&cWarp &f" + warpName + " &cnot found!"));
            sendWarpList(player);
            return true;
        }

        String cooldownKey = "warp";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey) && !player.hasPermission("washere.warp.bypass")) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }

        player.teleport(location);
        CooldownManager.setCooldown(uuid, cooldownKey, 3);
        player.sendMessage(ChatUtils.colorizeMini("&aTeleported to warp &f" + warpName + "&a!"));

        return true;
    }

    private void sendWarpList(@NotNull Player player) {
        Set<String> privateWarps = WarpManager.getWarps(player.getUniqueId());
        Set<String> publicWarps = WarpManager.getPublicWarps();

        player.sendMessage(ChatUtils.colorizeMini("&6=== Available Warps ==="));

        if (!privateWarps.isEmpty()) {
            player.sendMessage(ChatUtils.colorizeMini("&aYour warps: &f" + String.join(", ", privateWarps)));
        }

        if (!publicWarps.isEmpty()) {
            player.sendMessage(ChatUtils.colorizeMini("&aPublic warps: &f" + String.join(", ", publicWarps)));
        }

        if (privateWarps.isEmpty() && publicWarps.isEmpty()) {
            player.sendMessage(ChatUtils.colorizeMini("&cNo warps available."));
        }
    }
}


package me.washeremc.SERVERMODE.survival.Warp;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DelWarpCommand implements CommandExecutor {

    private final Washere plugin;

    public DelWarpCommand(Washere plugin) {
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

        if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("public")) {
                if (player.hasPermission("washere.warp.public")) {
                    player.sendMessage(ChatUtils.colorizeMini("&cUsage: /delwarp [public] <name>"));
                    return true;
                }
            }

            if (!WarpManager.isPublicWarpsEnabled()) {
                player.sendMessage(ChatUtils.colorizeMini("&cPublic warps are currently disabled."));
                return true;
            }

            if (!player.hasPermission("washere.warp.public")) {
                player.sendMessage(ChatUtils.colorizeMini("&cYou don't have permission to delete public warps."));
                return true;
            }

            String warpName = args[1];
            if (WarpManager.deletePublicWarp(warpName)) {
                player.sendMessage(ChatUtils.colorizeMini("&aPublic warp &f" + warpName + " &ahas been deleted!"));
            } else {
                player.sendMessage(ChatUtils.colorizeMini("&cPublic warp &f" + warpName + " &cnot found!"));
            }
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "delwarp";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length != 1) {
            player.sendMessage(ChatUtils.colorizeMini("&cUsage: /delwarp [name]"));
            return false;
        }

        String warpName = args[0];
        UUID playerUUID = player.getUniqueId();
        if (WarpManager.deleteWarp(playerUUID, warpName)) {
            player.sendMessage(ChatUtils.colorizeMini("&aWarp " + warpName + " deleted!"));
        } else {
            player.sendMessage(ChatUtils.colorizeMini("&cWarp " + warpName + " not found!"));
        }
        return true;
    }
}
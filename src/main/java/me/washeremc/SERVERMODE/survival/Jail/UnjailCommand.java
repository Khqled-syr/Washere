package me.washeremc.SERVERMODE.survival.Jail;


import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {
    private final JailManager jailManager;

    private final Washere plugin;
    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public UnjailCommand(JailManager jailManager, Washere plugin) {
        this.jailManager = jailManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("washere.unjail")) {
            sender.sendMessage(ChatUtils.colorizeMini("&cYou don't have permission to use this command!"));
            return true;
        }

        if (isLobby()) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatUtils.colorizeMini("&cUsage: /unjail <player>"));
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target != null) {
            if (jailManager.unjailPlayer(target.getUniqueId())) {
                sender.sendMessage(ChatUtils.colorizeMini("&aPlayer " + target.getName() + " has been released from jail!"));
            } else {
                sender.sendMessage(ChatUtils.colorizeMini("&cPlayer " + target.getName() + " is not jailed!"));
            }
        } else {
            boolean found = false;
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                    UUID uuid = offlinePlayer.getUniqueId();
                    if (jailManager.unjailPlayer(uuid)) {
                        sender.sendMessage(ChatUtils.colorizeMini("&aOffline player " + offlinePlayer.getName() + " has been released from jail!"));
                    } else {
                        sender.sendMessage(ChatUtils.colorizeMini("&cPlayer " + offlinePlayer.getName() + " is not jailed!"));
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                sender.sendMessage("§cPlayer not found!");
            }
        }
        return true;
    }
}
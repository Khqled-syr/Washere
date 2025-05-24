package me.washeremc.Core.commands;

import me.washeremc.Core.proxy.PluginMessage;
import me.washeremc.Core.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConnectCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtils.colorizeMini("&cUsage: /connect <server>"));
            return true;
        }

        String serverName = args[0].toLowerCase();


        if (player.getServer().getName().equalsIgnoreCase(serverName)) {
            player.sendMessage(ChatUtils.colorizeMini("&cYou are already connected to &e" + serverName + "&c."));
            return true;
        }

        PluginMessage.connect(player, serverName);
        return true;

    }
}


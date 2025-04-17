package me.washeremc.Core.commands;

import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final Washere plugin;

    public ReloadCommand(Washere plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (sender.hasPermission("washere.reload")) {
            sender.sendMessage(ChatUtils.colorize("&eReloading WasHere plugin..."));
            plugin.getPluginReloadManager().reloadCustomConfig();
            sender.sendMessage(ChatUtils.colorize("&aWasHere plugin reloaded successfully."));
        } else {
            sender.sendMessage(ChatUtils.colorize("&cYou do not have permission to use this command."));
        }
        return true;
    }
}
package me.washeremc.SERVERMODE.survival.Jail;

import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetJailCommand implements CommandExecutor {
    private final JailManager jailManager;
    private final Washere plugin;
    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public SetJailCommand(JailManager jailManager, Washere plugin) {
        this.jailManager = jailManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThis command can only be used by players!"));
            return true;
        }


        if (!sender.hasPermission("washere.setjail")) {
            sender.sendMessage(ChatUtils.colorizeMini("&cYou don't have permission to use this command!"));
            return true;
        }

        if (isLobby()) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }


        if (jailManager.setJailLocation(player.getLocation())) {
            player.sendMessage(ChatUtils.colorizeMini("&aJail location has been set to your current position!"));
        } else {
            player.sendMessage(ChatUtils.colorizeMini("&cFailed to set jail location!"));
        }
        return true;
    }
}
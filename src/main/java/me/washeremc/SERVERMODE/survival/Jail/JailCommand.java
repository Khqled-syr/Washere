package me.washeremc.SERVERMODE.survival.Jail;

import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JailCommand implements CommandExecutor {
    private final JailManager jailManager;
    private final Washere plugin;
    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public JailCommand(JailManager jailManager, Washere plugin) {
        this.jailManager = jailManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command!"));
            return true;
        }

        if (!sender.hasPermission("washere.jail")) {
            sender.sendMessage(ChatUtils.colorizeMini("&cYou don't have permission to use this command!"));
            return true;
        }

        if (isLobby()) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.colorizeMini("&cUsage: /jail <player> <duration> <reason>"));
            sender.sendMessage(ChatUtils.colorizeMini("&cDuration format: 1d2h3m4s (days, hours, minutes, seconds)"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtils.colorizeMini("&cPlayer not found or not online!"));
            return true;
        }

        if (jailManager.getJailLocation() == null) {
            sender.sendMessage(ChatUtils.colorizeMini("&cJail location has not been set! Use /setjail first."));
            return true;
        }

        String durationStr = args[1].toLowerCase();
        long durationSeconds = 0;

        StringBuilder numBuilder = new StringBuilder();
        for (int i = 0; i < durationStr.length(); i++) {
            char c = durationStr.charAt(i);
            if (Character.isDigit(c)) {
                numBuilder.append(c);
            } else {
                int num = !numBuilder.isEmpty() ? Integer.parseInt(numBuilder.toString()) : 0;
                numBuilder = new StringBuilder();

                switch (c) {
                    case 'd':
                        durationSeconds += num * 86400L;
                        break;
                    case 'h':
                        durationSeconds += num * 3600L;
                        break;
                    case 'm':
                        durationSeconds += num * 60L;
                        break;
                    case 's':
                        durationSeconds += num;
                        break;
                    default:
                        sender.sendMessage(ChatUtils.colorizeMini("&cInvalid duration format! Use: 1d2h3m4s"));
                        return true;
                }
            }
        }

        if (durationSeconds <= 0) {
            sender.sendMessage(ChatUtils.colorizeMini("&cDuration must be greater than 0!"));
            return true;
        }

        StringBuilder reason = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        if (jailManager.jailPlayer(target.getUniqueId(), durationSeconds, reason.toString().trim())) {
            String timeFormatted = jailManager.formatTime(durationSeconds);
            sender.sendMessage(ChatUtils.colorizeMini("&aPlayer " + target.getName() + " has been jailed for " + timeFormatted));
        } else {
            sender.sendMessage(ChatUtils.colorizeMini("&cFailed to jail player. Is the jail location set?"));
        }
        return true;
    }
}
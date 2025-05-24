package me.washeremc.SERVERMODE.lobby.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.SERVERMODE.lobby.NPCUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public class NPCCommand implements CommandExecutor {

    private final Washere plugin = Washere.getPlugin(Washere.class);

    public NPCCommand(NPCUtils ignoredNpcUtils) {
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command!"));
            return true;
        }

        if (!isLobby()) {
            player.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "npc";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length < 2) {
            player.sendMessage(ChatUtils.colorizeMini("&cUsage: /npc <name> <action>"));
            player.sendMessage(ChatUtils.colorizeMini("&7Actions can be:"));
            player.sendMessage(ChatUtils.colorizeMini("&7- connect:<server> to connect to server"));
            player.sendMessage(ChatUtils.colorizeMini("&7- cmd:<command> to run as player"));
            player.sendMessage(ChatUtils.colorizeMini("&7- console:<command> to run as console"));
            player.sendMessage(ChatUtils.colorizeMini("&7- or just use a server name directly"));
            return true;
        }

        String name = args[0];
        String action = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Create the NPC with the specified action
        plugin.getNpcUtils().createNPC(player, name, action);
        return true;
    }
}
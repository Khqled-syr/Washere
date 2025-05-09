package me.washeremc.SERVERMODE.lobby;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NPCCommand implements CommandExecutor {
    private final NPCUtils npcUtils;

    private final Washere plugin = Washere.getPlugin(Washere.class);

    public NPCCommand(NPCUtils npcUtils) {
        this.npcUtils = npcUtils;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command!"));
            return true;
        }

        if (!isLobby()) {
            player.sendMessage(ChatUtils.colorize("&cThis command is not available in this server."));
            return true;
        }

        // 🔥 Cooldown check
        UUID uuid = player.getUniqueId();
        String cooldownKey = "npc";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);


        if (args.length < 1) {
            player.sendMessage(ChatUtils.colorize("&cUsage: /npc <name> <server>"));
            return true;
        }

        String name = args[0].replace("_", " ");
        String server = args[1];

        npcUtils.createNPC(player, name, server);
        return true;
    }
}
package me.washeremc.SERVERMODE.lobby.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.SERVERMODE.lobby.NPCUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class NPCDeleteCommand implements CommandExecutor {
    private final NPCUtils npcUtils;
    private final Washere plugin = Washere.getPlugin(Washere.class);

    public NPCDeleteCommand(NPCUtils npcUtils) {
        this.npcUtils = npcUtils;
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
        String cooldownKey = "deletenpc";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length < 1) {
            player.sendMessage(ChatUtils.colorizeMini("&cUsage: /deletenpc <id>"));
            return true;
        }

        File npcFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcFile.exists()) {
            player.sendMessage(ChatUtils.colorizeMini("&cNo NPCs found!"));
            return true;
        }

        FileConfiguration npcConfig = YamlConfiguration.loadConfiguration(npcFile);
        Set<String> keys = npcConfig.getKeys(false);

        if (keys.isEmpty()) {
            player.sendMessage(ChatUtils.colorizeMini("&cNo NPCs found!"));
            return true;
        }

        List<String> npcIds = new ArrayList<>(keys);

        try {
            int npcIndex = Integer.parseInt(args[0]);
            if (npcIndex < 1 || npcIndex > npcIds.size()) {
                player.sendMessage(ChatUtils.colorizeMini("&cInvalid NPC ID! Use /listnpcs to see valid IDs."));
                return true;
            }

            String npcUUID = npcIds.get(npcIndex - 1);
            npcUtils.deleteNPC(player, npcUUID);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtils.colorizeMini("&cInvalid ID! Use a number from /listnpcs."));
        }

        return true;
    }
}
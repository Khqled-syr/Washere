package me.washeremc.SERVERMODE.lobby.commands;


import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;
import java.util.UUID;

public class NPCListommand implements CommandExecutor {

    private final Washere plugin = Washere.getPlugin(Washere.class);

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public NPCListommand() {
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("listnpcs")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command!"));
                return true;
            }

            if (!isLobby()) {
                player.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
                return true;
            }

            UUID uuid = player.getUniqueId();
            String cooldownKey = "listnpcs";
            if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
                long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
                player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
                return true;
            }
            CooldownManager.setCooldown(uuid, cooldownKey, 3);

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

            player.sendMessage(ChatUtils.colorizeMini("&6==== NPC List ===="));

            int index = 1;
            for (String key : keys) {
                String npcName = npcConfig.getString(key + ".name", "Unknown NPC");
                player.sendMessage(ChatUtils.colorizeMini("&e" + index + ". &a" + npcName + " &7(ID: " + key + ")"));
                index++;
            }
            return true;
        }
        return false;
    }
}
package me.washeremc.SERVERMODE.lobby.commands;


import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetSpawnCommand implements CommandExecutor, TabCompleter {

    private final Washere plugin;
    private File spawnFile;
    private FileConfiguration spawnConfig;

    public SetSpawnCommand(Washere plugin) {
        this.plugin = plugin;
        if (isLobby()) {
            loadSpawnConfig();
        }
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    private void loadSpawnConfig() {
        spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        if (!spawnFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                spawnFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create spawn.yml file: " + e.getMessage());
            }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
    }

    private void saveSpawnConfig() {
        if (spawnConfig != null && spawnFile != null) {
            try {
                spawnConfig.save(spawnFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save spawn.yml file: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThis command can only be used by players!"));
            return true;
        }

        if (!isLobby()) {
            player.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        if (spawnConfig == null) {
            loadSpawnConfig();
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "spawn";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (!player.hasPermission("washere.staff")) {
            player.sendMessage(ChatUtils.colorizeMini("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            if (!spawnConfig.contains("serverSpawn")) {
                player.sendMessage(ChatUtils.colorizeMini("&cThe server spawn has not been set!"));
                return true;
            }

            Location spawnLocation = spawnConfig.getLocation("serverSpawn");
            if (spawnLocation == null) {
                player.sendMessage(ChatUtils.colorizeMini("&cThe server spawn location is invalid!"));
                return true;
            }

            player.teleport(spawnLocation);
            player.sendMessage(ChatUtils.colorizeMini("&aTeleported to the server spawn."));
            player.getInventory().clear();
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
            Location playerLoc = player.getLocation();
            spawnConfig.set("serverSpawn", playerLoc);
            saveSpawnConfig();
            player.sendMessage(ChatUtils.colorizeMini("&aThe server spawn has been set!"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            if (!spawnConfig.contains("serverSpawn")) {
                player.sendMessage(ChatUtils.colorizeMini("&cThe server spawn has not been set!"));
                return true;
            }

            spawnConfig.set("serverSpawn", null);
            saveSpawnConfig();
            player.sendMessage(ChatUtils.colorizeMini("&aThe server spawn has been cleared!"));
            return true;
        }

        player.sendMessage(ChatUtils.colorizeMini("&cUsage: /setspawn [set|clear]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String @NotNull [] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("washere.staff") && isLobby()) {
            options.add("set");
            options.add("clear");
        }
        return options;
    }
}
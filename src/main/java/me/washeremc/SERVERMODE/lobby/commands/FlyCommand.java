package me.washeremc.SERVERMODE.lobby.commands;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class FlyCommand implements CommandExecutor {

    public static final String FLY_PERMISSION = "washere.fly";
    private final Washere plugin;

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public FlyCommand(Washere plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command."));
            return true;
        }

        if (!isLobby()) {
            player.sendMessage(ChatUtils.colorize("&cThis command is not available in this server."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "fly";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (!player.hasPermission(FLY_PERMISSION)) {
            player.sendMessage(ChatUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        boolean flightEnabled = !player.getAllowFlight();
        player.setAllowFlight(flightEnabled);
        player.setFlying(flightEnabled);

        String status = flightEnabled ? "&aenabled" : "&cdisabled";
        player.sendMessage(ChatUtils.colorize("&eFlight mode: " + status));

        return true;
    }

    public static void setFlight(@NotNull Player player, boolean enabled) {
        player.setAllowFlight(enabled);
        player.setFlying(enabled);
    }
}
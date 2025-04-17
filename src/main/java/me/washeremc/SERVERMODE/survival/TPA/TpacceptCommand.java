package me.washeremc.SERVERMODE.survival.TPA;

import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TpacceptCommand implements CommandExecutor {

    private final Washere plugin;

    public TpacceptCommand(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player targetPlayer)) {
            sender.sendMessage(ChatUtils.colorize("&cOnly players can use this command."));
            return true;
        }

        if (isLobby()) {
            targetPlayer.sendMessage(ChatUtils.colorize("&cThis command is not available in this server."));
            return true;
        }

        // 🔥 Cooldown check
        UUID uuid = targetPlayer.getUniqueId();
        String cooldownKey = "tpaccept";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            sender.sendMessage(ChatUtils.colorize("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }

        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        UUID targetId = targetPlayer.getUniqueId();

        if (!plugin.getTpaManager().hasRequest(targetId)) {
            targetPlayer.sendMessage(ChatUtils.colorize("&cNo teleport requests found!"));
            return true;
        }

        UUID senderId = plugin.getTpaManager().getRequest(targetId);
        Player senderPlayer = targetPlayer.getServer().getPlayer(senderId);

        if (senderPlayer == null) {
            targetPlayer.sendMessage(ChatUtils.colorize("&cPlayer who sent the request is no longer online!"));
            plugin.getTpaManager().removeRequest(targetId);
            return true;
        }

        BukkitRunnable expirationTask = plugin.getTpaManager().getExpirationTask(targetId);
        if (expirationTask != null) {
            expirationTask.cancel();
        }

        plugin.getTpaManager().removeRequest(targetId);

        senderPlayer.teleport(targetPlayer.getLocation());
        senderPlayer.sendMessage(ChatUtils.colorize("&7Teleport request accepted by &e" + targetPlayer.getName()));
        targetPlayer.sendMessage(ChatUtils.colorize("&aYou have accepted the teleport request from &e" + senderPlayer.getName()));

        return true;
    }
}
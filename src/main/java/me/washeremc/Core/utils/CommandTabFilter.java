package me.washeremc.Core.utils;

import me.washeremc.Washere;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandTabFilter implements Listener {

    private final Washere plugin;

    public CommandTabFilter(Washere plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandTabShow(@NotNull PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        List<String> allowedCommands = plugin.getConfig().getStringList("allowed-commands");
        boolean opBypass = plugin.getConfig().getBoolean("op-bypass", true);
        String bypassPermission = plugin.getConfig().getString("bypass-permission", "washere.tabcomplete.bypass");

        if ((opBypass && player.isOp()) || player.hasPermission(bypassPermission)) return;

        Set<String> allowed = new HashSet<>();
        for (String cmd : allowedCommands) {
            allowed.add(cmd.toLowerCase());
        }

        event.getCommands().clear();
        event.getCommands().addAll(allowed);
    }
}

package me.washeremc.SERVERMODE.survival.utils;

import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarTask extends BukkitRunnable {

    private final Washere plugin;

    public ActionBarTask(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }


    @Override
    public void run() {
        if (isLobby()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (SettingsManager.isActionbarEnabled(player)) {
                String coords = String.format("X: %.0f, Y: %.0f, Z: %.0f", player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
                ActionBarUtils.sendActionBar(player, coords);
            }else {
                ActionBarUtils.hideActionBar(player);
            }

        }
    }
}
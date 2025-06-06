package me.washeremc.SERVERMODE.survival.utils;

import me.washeremc.Washere;
import org.bukkit.entity.Player;

public class ActionBarManager {
    private final Washere plugin;
    private ActionBarTask actionBarTask;

    public ActionBarManager(Washere plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        actionBarTask = new ActionBarTask(plugin);
        actionBarTask.runTaskTimer(plugin, 20L, 2L);
    }

    public void stop() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    public void setTemporaryMessage(Player player, long durationMs) {
        if (actionBarTask != null) {
            actionBarTask.setTemporaryMessage(player, durationMs);
        }
    }
}
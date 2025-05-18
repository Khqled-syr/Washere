package me.washeremc.SERVERMODE.survival.utils;

import me.washeremc.Core.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ActionBarUtils {
    public static void sendActionBar(@NotNull Player player, String message) {
        player.sendActionBar(ChatUtils.colorizeMini("&e" + message));
    }
    public static void hideActionBar(@NotNull Player player) {
        player.sendActionBar(Component.empty());
    }
}
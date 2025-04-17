package me.washeremc.SERVERMODE.survival.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ActionBarUtils {
    public static void sendActionBar(@NotNull Player player, String message) {
        player.sendActionBar(Component.text(message).color(NamedTextColor.WHITE));
    }
    public static void hideActionBar(@NotNull Player player) {
        player.sendActionBar(Component.empty());
    }
}
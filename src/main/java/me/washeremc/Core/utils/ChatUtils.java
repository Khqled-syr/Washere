package me.washeremc.Core.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ChatUtils {


    @Contract("_ -> new")
    @SuppressWarnings("deprecation")
    public static @NotNull String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static @NotNull String extractColorCode(String text) {
        if (text == null || !text.contains("&")) return "§7";
        return "§" + text.charAt(text.indexOf("&") + 1);
    }

    @Contract(pure = true)
    public static NamedTextColor getNamedTextColor(@NotNull String colorCode) {
        return switch (colorCode) {
            case "§4" -> NamedTextColor.DARK_RED;
            case "§c" -> NamedTextColor.RED;
            case "§6" -> NamedTextColor.GOLD;
            case "§e" -> NamedTextColor.YELLOW;
            case "§2" -> NamedTextColor.DARK_GREEN;
            case "§a" -> NamedTextColor.GREEN;
            case "§b" -> NamedTextColor.AQUA;
            case "§3" -> NamedTextColor.DARK_AQUA;
            case "§1" -> NamedTextColor.DARK_BLUE;
            case "§9" -> NamedTextColor.BLUE;
            case "§d" -> NamedTextColor.LIGHT_PURPLE;
            case "§5" -> NamedTextColor.DARK_PURPLE;
            case "§f" -> NamedTextColor.WHITE;
            case "§8" -> NamedTextColor.DARK_GRAY;
            case "§0" -> NamedTextColor.BLACK;
            default -> NamedTextColor.GRAY;
        };
    }


    public static String stripColor(String text) {
        if (text == null) return null;
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
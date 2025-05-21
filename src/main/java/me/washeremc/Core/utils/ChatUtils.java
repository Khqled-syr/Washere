package me.washeremc.Core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ChatUtils {


    public static @NotNull String colorize(String message) {
        if (message == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    public static @NotNull Component colorizeMini(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
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

    public static TextColor getTextColorFromLegacyCode(char code) {
        return switch (code) {
            case '4' -> NamedTextColor.DARK_RED;
            case 'c' -> NamedTextColor.RED;
            case '6' -> NamedTextColor.GOLD;
            case 'e' -> NamedTextColor.YELLOW;
            case '2' -> NamedTextColor.DARK_GREEN;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '9' -> NamedTextColor.BLUE;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case 'f' -> NamedTextColor.WHITE;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '0' -> NamedTextColor.BLACK;
            default -> NamedTextColor.GRAY;
        };
    }


    public static String stripColor(String text) {
        if (text == null) return null;

        Component component = LegacyComponentSerializer.legacySection().deserialize(text);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static @NotNull String stripColorLegacy(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }


}
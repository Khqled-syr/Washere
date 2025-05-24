package me.washeremc.Core.PlayerTime;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.washeremc.Washere;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PlayerTimeExpansion extends PlaceholderExpansion {
    private final Washere plugin;
    private final PlayerTimeManager playerTimeManager;

    public PlayerTimeExpansion(Washere plugin, PlayerTimeManager playerTimeManager) {
        this.plugin = plugin;
        this.playerTimeManager = playerTimeManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playertime";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        return switch (params.toLowerCase()) {
            case "hours" -> String.valueOf(playerTimeManager.getPlayerTime(player.getUniqueId()));
            case "hours_formatted" -> formatTime(playerTimeManager.getPlayerTime(player.getUniqueId()));
            default -> null;
        };
    }

    @Contract(pure = true)
    private @NotNull String formatTime(long hours) {
        if (hours < 24) {
            return hours + " hours";
        } else {
            long days = hours / 24;
            long remainingHours = hours % 24;
            return days + " days, " + remainingHours + " hours";
        }
    }
}
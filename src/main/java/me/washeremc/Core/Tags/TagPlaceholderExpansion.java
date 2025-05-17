package me.washeremc.Core.Tags;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.washeremc.Washere;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TagPlaceholderExpansion extends PlaceholderExpansion {
    private final Washere plugin;

    public TagPlaceholderExpansion(Washere plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "washere";
    }

    @Override
    public @NotNull String getAuthor() {
        return "WashereMC";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier) {
            case "tag_prefix" -> {
                Tag tag = TagManager.getPlayerTag(player.getUniqueId());
                return tag != null ? tag.prefix() : "";
            }
            case "tag_suffix" -> {
                Tag tag = TagManager.getPlayerTag(player.getUniqueId());
                return tag != null ? tag.suffix() : "";
            }
            case "tag_name" -> {
                Tag tag = TagManager.getPlayerTag(player.getUniqueId());
                return tag != null ? tag.displayName() : "";
            }
        }

        return null;
    }
}
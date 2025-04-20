package me.washeremc.SERVERMODE.survival.Warp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WarpTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        UUID playerUUID = player.getUniqueId();
        Set<String> warps = WarpManager.getWarps(playerUUID);
        Set<String> publicWarps = WarpManager.getPublicWarps();

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String currentArg = args[0].toLowerCase();

            for (String warp : warps) {
                if (warp.toLowerCase().startsWith(currentArg)) {
                    suggestions.add(publicWarps.contains(warp) ? warp + " (public)" : warp);
                }
            }

            for (String warp : publicWarps) {
                if (warp.toLowerCase().startsWith(currentArg) && !warps.contains(warp)) {
                    suggestions.add(warp + " (public)");
                }
            }
            return suggestions;
        }
        return new ArrayList<>();
    }
}
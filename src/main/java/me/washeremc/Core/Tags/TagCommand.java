package me.washeremc.Core.Tags;

import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TagCommand implements CommandExecutor {
    private final Washere plugin;
    private final TagGUI tagGUI;

    public TagCommand(Washere plugin) {
        this.plugin = plugin;
        this.tagGUI = new TagGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command!"));
            return true;
        }

        tagGUI.openTagSelector(player);
        return true;
    }
}
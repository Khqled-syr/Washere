package me.washeremc.SERVERMODE.survival.commands;


import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DonateCommand implements CommandExecutor {

    private final Washere plugin;

    public DonateCommand(Washere plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

        if (!(sender instanceof Player donor)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command."));
            return true;
        }

        if (isLobby()) {
            sender.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        UUID uuid = donor.getUniqueId();
        String cooldownKey = "donate";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            sender.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length != 1) {
            sender.sendMessage(ChatUtils.colorizeMini("&cUsage: /donate <player>"));
            return true;
        }


        Player recipient = Bukkit.getPlayer(args[0]);

        if (recipient == null) {
            donor.sendMessage(ChatUtils.colorizeMini("&cPlayer not found!"));
            return true;
        }

        if (recipient.equals(donor)) {
            donor.sendMessage(ChatUtils.colorizeMini("&cReally? You cannot do that lol!"));
            return true;
        }

        openDonateGui(donor, recipient);
        return true;
    }

    private void openDonateGui(@NotNull Player donor, @NotNull Player recipient) {
        Inventory donateInventory = Bukkit.createInventory(null, 36, Component.text("Donate to " + recipient.getName()));

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text(ChatUtils.colorize("&cCancel")));
        cancelItem.setItemMeta(cancelMeta);
        donateInventory.setItem(27, cancelItem);

        ItemStack donateItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta donateMeta = donateItem.getItemMeta();
        donateMeta.displayName(Component.text(ChatUtils.colorize("&aDonate")));
        donateItem.setItemMeta(donateMeta);
        donateInventory.setItem(35, donateItem);

        donor.openInventory(donateInventory);
        donor.setMetadata("donateRecipient", new FixedMetadataValue(plugin, recipient.getUniqueId()));
    }
}

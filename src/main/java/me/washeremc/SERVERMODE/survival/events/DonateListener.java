package me.washeremc.SERVERMODE.survival.events;

import me.washeremc.Core.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("ALL")
public class DonateListener implements Listener {

    private final Plugin plugin;
    public DonateListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Donate to ")) {
            Player donor = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot >= 0 && slot <= 26) {
                event.setCancelled(false);
            } else if (slot == 27) {
                // Cancel button clicked
                returnItemsToDonor(donor);
                donor.closeInventory();
                donor.sendMessage(ChatUtils.colorize("&cDonation cancelled."));
                donor.playSound(donor.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                event.setCancelled(true);
            } else
                if (slot == 35) {
                if (isInventoryEmpty(event.getInventory())) {
                    donor.sendMessage(ChatUtils.colorize("&cYou have to select at least 1 item to donate!"));
                    donor.playSound(donor.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                } else {
                    donateItems(donor);
                }
                event.setCancelled(true);
            } else event.setCancelled(slot < event.getView().getTopInventory().getSize());
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("Donate to ")) {
            Player donor = (Player) event.getPlayer();
            if (!donor.hasMetadata("donationComplete")) {
                returnItemsToDonor(donor);
            }
            donor.removeMetadata("donateRecipient", plugin);
            donor.removeMetadata("donationComplete", plugin);
        }
    }

    private boolean isInventoryEmpty(Inventory inventory) {
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) != null && Objects.requireNonNull(inventory.getItem(i)).getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void donateItems(@NotNull Player donor) {
        List<MetadataValue> metadataValues = donor.getMetadata("donateRecipient");
        if (metadataValues.isEmpty()) return;

        UUID recipientUUID = (UUID) metadataValues.get(0).value();
        Player recipient = Bukkit.getPlayer(Objects.requireNonNull(recipientUUID));

        if (recipient != null && recipient.isOnline()) {
            Inventory donateInventory = donor.getOpenInventory().getTopInventory();
            HashMap<Integer, ItemStack> excessItems = new HashMap<>();
            StringBuilder donatedItems = new StringBuilder();
            for (int i = 0; i < 27; i++) {
                ItemStack item = donateInventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> recipientExcess = recipient.getInventory().addItem(item);
                    if (!recipientExcess.isEmpty()) {
                        for (ItemStack excessItem : recipientExcess.values()) {
                            excessItems.put(i, excessItem);
                        }
                    } else {
                        donateInventory.setItem(i, null);
                        donatedItems.append(item.getAmount()).append("x ").append(item.getType()).append(", ");
                    }
                }
            }
            if (donatedItems.length() > 2) {
                donatedItems.setLength(donatedItems.length() - 2);
            }
            for (int slot : excessItems.keySet()) {
                ItemStack excessItem = excessItems.get(slot);
                if (excessItem != null) {
                    donor.getInventory().addItem(excessItem);
                }
            }
            donor.setMetadata("donationComplete", new FixedMetadataValue(plugin, true));
            donor.sendMessage(ChatUtils.colorize("&aDonation successful! You donated: " + donatedItems));
            donor.playSound(donor.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            recipient.sendMessage(ChatUtils.colorize("&aYou have received a donation from " + donor.getName() + ": " + donatedItems));
            recipient.playSound(recipient.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        } else {
            donor.sendMessage(ChatUtils.colorize("&cDonation failed: Recipient is offline."));
            donor.playSound(donor.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
        }
        donor.closeInventory();
    }

    private void returnItemsToDonor(@NotNull Player donor) {
        Inventory donateInventory = donor.getOpenInventory().getTopInventory();
        HashMap<Integer, ItemStack> excessItems = new HashMap<>();
        for (int i = 0; i < 27; i++) {
            ItemStack item = donateInventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> donorExcess = donor.getInventory().addItem(item);
                // Store excess items to drop
                if (!donorExcess.isEmpty()) {
                    excessItems.putAll(donorExcess);
                } else {
                    donateInventory.setItem(i, null);
                }
            }
        }
        for (ItemStack excessItem : excessItems.values()) {
            donor.getWorld().dropItem(donor.getLocation(), excessItem);
        }
    }
}
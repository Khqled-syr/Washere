package me.washeremc.Core.Tags;

import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.Settings.SettingsManager;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TagGUI implements Listener {
    private final Washere plugin;
    private static final Map<UUID, Inventory> openInventories = new HashMap<>();

    public TagGUI(@NotNull Washere plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openTagSelector(Player player) {
        List<Tag> allTags = TagManager.getAllTags();

        if (allTags.isEmpty()) {
            player.sendMessage(ChatUtils.colorizeMini("&cNo tags are available."));
            return;
        }

        int rows = 6;
        Inventory inventory = Bukkit.createInventory(null, rows * 9, ChatUtils.colorizeMini("Tag Selector"));

        Tag currentTag = TagManager.getPlayerTag(player.getUniqueId());

        for (Tag tag : allTags) {
            Material material;
            try {
                material = Material.valueOf(tag.material());
            } catch (IllegalArgumentException e) {
                material = Material.NAME_TAG;
            }

            boolean hasAccess = player.hasPermission(tag.permission());
            ItemStack item = new ItemStack(hasAccess ? material : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatUtils.colorize(tag.displayName()));

                List<String> lore = new ArrayList<>();
                if (currentTag != null && currentTag.id().equals(tag.id())) {
                    lore.add(ChatUtils.colorize("&aCurrently Selected"));
                } else if (hasAccess) {
                    lore.add(ChatUtils.colorize("&7Click to select this tag"));
                } else {
                    lore.add(ChatUtils.colorize("&cYou do not have access to this tag."));
                }

                String name = "%luckperms_prefix%%player_displayname%";
                lore.add("");
                String preview = ChatUtils.colorize(tag.prefix() + "&r" + PlaceholderAPI.setPlaceholders(player, name) + "&r" + tag.suffix());
                lore.add(ChatUtils.colorize("&7Preview: " + preview));

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            int slot = tag.slot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            } else {
                inventory.addItem(item);
            }
        }

        // Add "Remove Tag" button
        ItemStack removeTagItem = new ItemStack(Material.BARRIER);
        ItemMeta removeTagMeta = removeTagItem.getItemMeta();
        if (TagManager.getAvailableTags(player).isEmpty()) {
            removeTagItem.setType(Material.AIR);
        } else {
            removeTagItem.setType(Material.BARRIER);
            if (removeTagMeta != null) {
                removeTagMeta.setDisplayName(ChatUtils.colorize("&cRemove Tag"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatUtils.colorize("&7Click to remove your current tag"));
                if (currentTag == null) {
                    lore.add(ChatUtils.colorize("&8You don't have a tag selected"));
                }
                removeTagMeta.setLore(lore);
                removeTagItem.setItemMeta(removeTagMeta);
            }
        }
        inventory.setItem(inventory.getSize() - 1, removeTagItem);

        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), inventory);
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!openInventories.containsKey(player.getUniqueId())) {
            return;
        }

        if (event.getInventory() != openInventories.get(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Handle "Remove Tag" button
        if (clickedItem.getType() == Material.BARRIER && ChatUtils.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Remove Tag")) {
            TagManager.setPlayerTag(player.getUniqueId(), null);
            player.sendMessage(ChatUtils.colorizeMini("&aYour tag has been removed."));
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() == Material.BARRIER) {
            player.sendMessage(ChatUtils.colorizeMini("&cYou cannot select this tag as you do not have access to it."));
            return;
        }

        String tagName = ChatUtils.stripColor(clickedItem.getItemMeta().getDisplayName());
        for (Tag tag : TagManager.getAllTags()) {
            if (ChatUtils.stripColor(tag.displayName()).equals(tagName)) {
                // Set the tag for the player
                TagManager.setPlayerTag(player.getUniqueId(), tag.id());
                player.sendMessage(ChatUtils.colorizeMini("&aYou have selected the " + tag.displayName() + " &atag."));
                player.closeInventory();
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            openInventories.remove(event.getPlayer().getUniqueId());
        }
    }
}
package me.washeremc.Core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record GuiItems(String name) {

    public static @NotNull ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        if (displayName != null) meta.displayName(Component.text(displayName));
        if (lore != null) {
            List<TextComponent> componentLore = lore.stream().map(Component::text).toList();
            meta.lore(componentLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static @NotNull ItemStack createPlayerHead(@NotNull Player player, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;

        meta.setOwningPlayer(player);
        if (displayName != null) meta.displayName(Component.text(displayName));
        if (lore != null) {
            List<TextComponent> componentLore = lore.stream().map(Component::text).toList();
            meta.lore(componentLore);
        }
        head.setItemMeta(meta);
        return head;
    }
}
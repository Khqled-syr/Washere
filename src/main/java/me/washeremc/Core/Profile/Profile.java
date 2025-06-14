package me.washeremc.Core.Profile;


import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.Settings.SettingsMenu;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Core.utils.GuiItems;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;


@SuppressWarnings("ALL")
public class Profile implements Listener {

    private static Washere plugin;

    static String lp = "%luckperms_prefix%";
    static String meta = "%luckperms_meta_color%";
    static String playeTime = "%playertime_full_format%";


    public static void initialize(Washere pluginInstance) {
        plugin = pluginInstance;
        plugin.getServer().getPluginManager().registerEvents(new Profile(), plugin);
    }

    public static void openProfile(Player player) {
        GuiItems guiItems = new GuiItems("Profile");
        Inventory profileGui = Profile.createProfileGui(player);
        player.openInventory(profileGui);
    }

    public static @NotNull Inventory createProfileGui(@NotNull Player player) {
        Inventory profileGui = Bukkit.createInventory(null, 36, "Profile");

        int totalMined = 0;
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                totalMined += player.getStatistic(Statistic.MINE_BLOCK, material);
            }
        }

        String rank = (lp == null || lp.trim().isEmpty()) ?
                ChatUtils.colorize("&7N/A") :
                ChatUtils.colorize(PlaceholderAPI.setPlaceholders(player, lp));

        ItemStack profileHead = GuiItems.createPlayerHead(player, ChatUtils.colorize("&eInfo"), Arrays.asList(
                ChatUtils.colorize("&7Name: &f" + PlaceholderAPI.setPlaceholders(player, meta) + player.getName()),
                ChatUtils.colorize("&7Rank: &f" + rank),
                ChatUtils.colorize("&7Playtime: &e" + PlaceholderAPI.setPlaceholders(player, playeTime)),
                "",
                ChatUtils.colorize("&eClick to open settings.")
        ));
        profileGui.setItem(13, profileHead);

        ItemStack closeIcon = GuiItems.createItem(Material.BARRIER, "§cClose", List.of("§7Click to close this menu!"));
        profileGui.setItem(31, closeIcon);


        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        assert fillerMeta != null;
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < profileGui.getSize(); i++) {
            if (profileGui.getItem(i) == null) {
                profileGui.setItem(i, filler);
            }
        }
        return profileGui;
    }


    public static void openOtherProfile(Player viewer, Player target) {
        Inventory profileGui = createOtherProfileGui(viewer, target);
        viewer.openInventory(profileGui);
    }

    public static @NotNull Inventory createOtherProfileGui(@NotNull Player viewer, @NotNull Player target) {
        Inventory profileGui = Bukkit.createInventory(null, 36, "Profile: " + target.getName());

        int totalMined = 0;
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                totalMined += target.getStatistic(Statistic.MINE_BLOCK, material);
            }
        }

        String rank = (lp == null || lp.trim().isEmpty()) ?
                ChatUtils.colorize("&7N/A") :
                ChatUtils.colorize(PlaceholderAPI.setPlaceholders(target, lp));

        ItemStack profileHead = GuiItems.createPlayerHead(target, ChatUtils.colorize("&eInfo"), Arrays.asList(
                ChatUtils.colorize("&7Name: &f" + PlaceholderAPI.setPlaceholders(target, meta) + target.getName()),
                ChatUtils.colorize("&7Rank: &f" + rank),
                ChatUtils.colorize("&7Playtime: &e" + PlaceholderAPI.setPlaceholders(target, playeTime))
        ));
        profileGui.setItem(13, profileHead);

        ItemStack closeIcon = GuiItems.createItem(Material.BARRIER, "§cClose", List.of("§7Click to close this menu!"));
        profileGui.setItem(31, closeIcon);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        assert fillerMeta != null;
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < profileGui.getSize(); i++) {
            if (profileGui.getItem(i) == null) {
                profileGui.setItem(i, filler);
            }
        }
        return profileGui;
    }


    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (event.getView().getTitle().startsWith("Profile: ")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.BARRIER && clickedItem.getItemMeta().getDisplayName().equals("§cClose")) {
                event.getInventory().close();
            }
        }

        if (event.getView().getTitle().equals("Profile")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.getItemMeta().getDisplayName().equals("§eInfo")) {
                SettingsMenu.openSettingsMenu(player);
                event.setCancelled(true);
            }

            if (clickedItem.getType() == Material.BARRIER && clickedItem.getItemMeta().getDisplayName().equals("§cClose")) {
                event.getInventory().close();
                event.setCancelled(true);
            }

        }
    }
}
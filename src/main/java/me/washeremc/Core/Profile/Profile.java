package me.washeremc.Core.Profile;


import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.Settings.SettingsMenu;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Core.utils.GuiItems;
import me.washeremc.SERVERMODE.survival.Home.HomeManager;
import me.washeremc.SERVERMODE.survival.Warp.WarpManager;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.Set;



@SuppressWarnings("ALL")
public class Profile implements Listener {

    private static Washere plugin;

    static String lp = "%luckperms_prefix%";
    static String meta = "%luckperms_meta_color%";
    static String playeTime = "%playertime_hours%";


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
                ChatUtils.colorize("&7Playtime: &e" + PlaceholderAPI.setPlaceholders(player, playeTime) + " hours" ),
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

    public static void openHomeMenu(@NotNull Player player) {
        Location homeLocation = HomeManager.getHome(player.getUniqueId());
        Inventory homeMenu = Bukkit.createInventory(null, 9, "Your home");

        if (homeLocation != null) {
            List<String> lore = List.of(
                    "§7X: §f" + homeLocation.getBlockX(),
                    "§7Y: §f" + homeLocation.getBlockY(),
                    "§7Z: §f" + homeLocation.getBlockZ(),
                    " ",
                    "§eClick to teleport!"
            );
            ItemStack homeItem = GuiItems.createItem(Material.PAPER, "§eHome", lore);
            homeMenu.addItem(homeItem);
        } else {
            ItemStack noHomeItem = GuiItems.createItem(Material.BARRIER, "§cNo Home", List.of("§7You have no home set."));
            homeMenu.addItem(noHomeItem);
        }

        player.openInventory(homeMenu);
    }

    public static void openWarpsMenu(@NotNull Player player) {
        Set<String> warps = WarpManager.getWarps(player.getUniqueId());
        Inventory warpsMenu = Bukkit.createInventory(null, 9, "Your warps");

        if (!warps.isEmpty()) {
            for (String warpName : warps) {
                Location warpLocation = WarpManager.getWarp(player.getUniqueId(), warpName);
                if (warpLocation != null) {
                    List<String> lore = List.of(
                            "§7X: §f" + warpLocation.getBlockX(),
                            "§7Y: §f" + warpLocation.getBlockY(),
                            "§7Z: §f" + warpLocation.getBlockZ(),
                            " ",
                            "§eClick to teleport!"
                    );
                    ItemStack warpItem = GuiItems.createItem(Material.PAPER, "§e" + warpName, lore);
                    warpsMenu.addItem(warpItem);
                }
            }
        } else {
            ItemStack noWarpsItem = GuiItems.createItem(Material.BARRIER, "§cNo Warps", List.of("§7You have no warps set."));
            warpsMenu.addItem(noWarpsItem);
        }

        player.openInventory(warpsMenu);
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (event.getView().getTitle().equals("Profile")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.getItemMeta().getDisplayName().equals("§eInfo")) {
                SettingsMenu.openSettingsMenu(player);
                event.setCancelled(true);
            }

            if (clickedItem.getType() == Material.COMPASS && clickedItem.getItemMeta().getDisplayName().equals("§eWarps")) {
                openWarpsMenu(player);
                event.setCancelled(true);
            }

            if (clickedItem.getType() == Material.DARK_OAK_DOOR && clickedItem.getItemMeta().getDisplayName().equals("§eHome")) {
                openHomeMenu(player);
                event.setCancelled(true);
            }
            if (clickedItem.getType() == Material.BARRIER && clickedItem.getItemMeta().getDisplayName().equals("§cClose")){
                event.getInventory().close();
                event.setCancelled(true);
            }

        } else if (event.getView().getTitle().equals("Your warps")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.PAPER) {
                String warpName = clickedItem.getItemMeta().getDisplayName().substring(2);
                Location warpLocation = WarpManager.getWarp(player.getUniqueId(), warpName);
                if (warpLocation != null) {
                    player.teleport(warpLocation);
                    player.sendMessage("§aTeleported to warp: §e" + warpName);
                } else {
                    player.sendMessage("§cWarp not found.");
                }
            }
        } else if(event.getView().getTitle().equals("Your home")) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.PAPER) {
                String homeName = clickedItem.getItemMeta().getDisplayName().substring(2);
                Location homeLocation = HomeManager.getHome(player.getUniqueId());
                if (homeLocation != null) {
                    player.teleport(homeLocation);
                    player.sendMessage("§aTeleported to Home");
                }else{
                    player.sendMessage("§cHome not found.");
                }
            }
        }
    }
}
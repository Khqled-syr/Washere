package me.washeremc.Core.Settings;


import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SettingsMenu {
    private static final Map<String, SettingDisplay> settingDisplays = new HashMap<>();
    private static final String MENU_TITLE = "Settings Menu";
    private static final int MENU_SIZE = 36; // 4 rows for better spacing
    private static Washere plugin;

    static {
        registerSettingDisplay("scoreboard", new SettingDisplay(Material.BARRIER, Material.BAMBOO_SIGN, 10, "Toggle your scoreboard visibility"));
        registerSettingDisplay("messaging", new SettingDisplay(Material.BARRIER, Material.PAPER, 12, "Toggle private messaging"));
        registerSettingDisplay("pinging", new SettingDisplay(Material.BARRIER, Material.BELL, 14, "Toggle mention notifications"));
        registerSettingDisplay("tpa", new SettingDisplay(Material.BARRIER, Material.ENDER_PEARL, 16, "Toggle teleport requests"));

        registerSettingDisplay("actionbar", new SettingDisplay(Material.BARRIER, Material.COMPASS, 19, "Toggle actionbar messages"));
        registerSettingDisplay("players_visibility", new SettingDisplay(Material.BARRIER, Material.PLAYER_HEAD, 21, "Toggle player visibility"));
        registerSettingDisplay("player_time", new SettingDisplay(Material.BARRIER, Material.CLOCK, 23, "Change your personal time"));

        // New PVP toggle setting - only for survival mode
        registerSettingDisplay("pvp", new SettingDisplay(Material.WOODEN_SWORD, Material.DIAMOND_SWORD, 25, "Toggle your PVP status"));
    }

    public static Washere getPlugin() {
        return plugin;
    }

    public static void setPlugin(Washere plugin) {
        SettingsMenu.plugin = plugin;
    }

    public static void registerSettingDisplay(String key, SettingDisplay display) {
        settingDisplays.put(key, display);
    }

    public static void openSettingsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new SettingsMenuHolder(), MENU_SIZE, MENU_TITLE);
        boolean isLobby = plugin != null && "lobby".equalsIgnoreCase(plugin.getServerType());
        boolean isSurvival = plugin != null && "survival".equalsIgnoreCase(plugin.getServerType());

        // Create border first
        createBorder(inventory);

        // Add settings
        for (Setting<?> setting : SettingRegistry.getSettings()) {
            String key = setting.getKey();
            SettingDisplay display = settingDisplays.get(key);

            if (display != null) {
                Object value = SettingsManager.getSettingValue(player, key);

                // For lobby-only settings, show them as unavailable in non-lobby servers
                boolean isLobbyOnlySetting = key.equals("players_visibility") || key.equals("player_time");

                // For survival-only settings, show them as unavailable in non-survival servers
                boolean isSurvivalOnlySetting = key.equals("pvp");

                if (!isLobby && isLobbyOnlySetting) {
                    inventory.setItem(display.slot(), createUnavailableSettingItem(setting, display, "Only available in lobby"));
                } else if (!isSurvival && isSurvivalOnlySetting) {
                    inventory.setItem(display.slot(), createUnavailableSettingItem(setting, display, "Only available in survival"));
                } else {
                    inventory.setItem(display.slot(), createSettingItem(setting, display, value));
                }
            }
        }

        // Add a close button at the bottom center
        inventory.setItem(31, createCloseButton());

        player.openInventory(inventory);
    }

    private static void createBorder(Inventory inventory) {
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            borderMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            borderItem.setItemMeta(borderMeta);
        }

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem(MENU_SIZE - 9 + i, borderItem);
        }

        // Left and right columns
        for (int i = 1; i < MENU_SIZE / 9 - 1; i++) {
            inventory.setItem(i * 9, borderItem);
            inventory.setItem(i * 9 + 8, borderItem);
        }

        // Fill remaining empty slots with a different color
        ItemStack fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            fillerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            fillerItem.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < MENU_SIZE; i++) {
            final int slot = i; // Make `i` effectively final
            if (inventory.getItem(slot) == null) {
                if (settingDisplays.values().stream().anyMatch(display -> display.slot() == slot)) {
                    continue;
                }
                if (slot == 31) continue; // Skip close button slot
                inventory.setItem(slot, fillerItem);
            }
        }
    }

    private static @NotNull ItemStack createCloseButton() {
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = closeButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Close");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to close this menu"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            closeButton.setItemMeta(meta);
        }
        return closeButton;
    }

    private static @NotNull ItemStack createSettingItem(Setting<?> setting, SettingDisplay display, Object value) {
        boolean isEnabled = value instanceof Boolean ? (Boolean) value : true;

        // Use the appropriate material based on enabled/disabled state
        Material material = isEnabled ? display.enabledMaterial() : display.disabledMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Create a more appealing title with status indicator
            String statusIndicator = isEnabled ?
                    ChatColor.GREEN + "✓ " :
                    ChatColor.RED + "✗ ";

            meta.setDisplayName(statusIndicator + ChatColor.GOLD + setting.getDisplayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            List<String> lore = new ArrayList<>();

            // Status line with color
            if (value instanceof Boolean) {
                lore.add(isEnabled ?
                        ChatColor.GREEN + "● ENABLED" :
                        ChatColor.RED + "● DISABLED");

                // Add cooldown info for PVP setting
                if (setting.getKey().equals("pvp")) {
                    Player player = Bukkit.getPlayer(UUID.randomUUID()); // Just to avoid null warnings
                    if (player != null) {
                        long cooldownTime = CooldownManager.getRemainingTime(player.getUniqueId(), "pvp_toggle");
                        if (cooldownTime > 0) {
                            lore.add(ChatColor.RED + "Cooldown: " + cooldownTime + "s");
                        }
                    }
                }
            } else if (value instanceof PlayerTime time) {
                String timeDisplay = switch (time) {
                    case DAY -> ChatColor.YELLOW + "Day";
                    case NIGHT -> ChatColor.BLUE + "Night";
                    case SUNSET -> ChatColor.GOLD + "Sunset";
                };
                lore.add(ChatColor.AQUA + "Current: " + timeDisplay);
            }

            lore.add("");
            lore.add(ChatColor.GRAY + display.description());
            lore.add("");
            lore.add(ChatColor.YELLOW + "» Click to toggle");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createUnavailableSettingItem(Setting<?> setting, SettingDisplay display, String reason) {
        Material material = Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "❌ " + ChatColor.GRAY + setting.getDisplayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.RED + "● UNAVAILABLE");
            lore.add("");
            lore.add(ChatColor.GRAY + display.description());
            lore.add("");
            lore.add(ChatColor.RED + "» " + reason);

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static @Nullable String getSettingKeyFromSlot(int slot) {
        // Only return a key if the setting should be active
        String key = settingDisplays.entrySet().stream()
                .filter(entry -> entry.getValue().slot() == slot)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        // Check if setting is available in current server mode
        if (key != null) {
            boolean isLobby = plugin != null && "lobby".equalsIgnoreCase(plugin.getServerType());
            boolean isSurvival = plugin != null && "survival".equalsIgnoreCase(plugin.getServerType());

            boolean isLobbyOnlySetting = key.equals("players_visibility") || key.equals("player_time");
            boolean isSurvivalOnlySetting = key.equals("pvp");

            if (!isLobby && isLobbyOnlySetting) {
                return null; // Return null to indicate this setting shouldn't be toggled
            }

            if (!isSurvival && isSurvivalOnlySetting) {
                return null; // Return null to indicate this setting shouldn't be toggled
            }
        }

        return key;
    }

    // Added close button handling
    public static boolean isCloseButton(int slot) {
        return slot == 31;
    }

    public static String getMenuTitle() {
        return MENU_TITLE;
    }

    // Custom inventory holder to prevent item movement
    public static class SettingsMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, MENU_SIZE, MENU_TITLE);
        }
    }

    public record SettingDisplay(Material disabledMaterial, Material enabledMaterial, int slot, String description) {
    }
}
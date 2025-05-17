package me.washeremc.Core.Settings;


import me.washeremc.Core.Managers.CooldownManager;
import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
    private static final String MENU_TITLE = "Settings";
    private static final int MENU_SIZE = 36;
    private static Washere plugin;

    static {
        registerSettingDisplay("scoreboard", new SettingDisplay(Material.BARRIER, Material.BAMBOO_SIGN, 10, "Toggle your scoreboard visibility"));
        registerSettingDisplay("messaging", new SettingDisplay(Material.BARRIER, Material.PAPER, 12, "Toggle private messaging"));
        registerSettingDisplay("pinging", new SettingDisplay(Material.BARRIER, Material.BELL, 14, "Toggle mention notifications"));
        registerSettingDisplay("tpa", new SettingDisplay(Material.BARRIER, Material.ENDER_PEARL, 16, "Toggle teleport requests"));

        registerSettingDisplay("actionbar", new SettingDisplay(Material.BARRIER, Material.COMPASS, 19, "Toggle actionbar messages"));
        registerSettingDisplay("players_visibility", new SettingDisplay(Material.BARRIER, Material.PLAYER_HEAD, 21, "Toggle player visibility"));
        registerSettingDisplay("player_time", new SettingDisplay(Material.BARRIER, Material.CLOCK, 23, "Change your personal time"));

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
        Inventory inventory = Bukkit.getServer().createInventory(new SettingsMenuHolder(), MENU_SIZE, MENU_TITLE);
        boolean isLobby = plugin != null && "lobby".equalsIgnoreCase(plugin.getServerType());
        boolean isSurvival = plugin != null && "survival".equalsIgnoreCase(plugin.getServerType());

        createBorder(inventory);

        for (Setting<?> setting : SettingRegistry.getSettings()) {
            String key = setting.getKey();
            SettingDisplay display = settingDisplays.get(key);

            if (display != null) {
                Object value = SettingsManager.getSettingValue(player, key);
                boolean isLobbyOnlySetting = key.equals("players_visibility") || key.equals("player_time");

                boolean isSurvivalOnlySetting = key.equals("pvp") || key.equals("tpa") || key.equals("actionbar");

                if (!isLobby && isLobbyOnlySetting) {
                    inventory.setItem(display.slot(), createUnavailableSettingItem(setting, display, "Only available in lobby"));
                } else if (!isSurvival && isSurvivalOnlySetting) {
                    inventory.setItem(display.slot(), createUnavailableSettingItem(setting, display, "Only available in survival"));
                } else {
                    inventory.setItem(display.slot(), createSettingItem(setting, display, value));
                }
            }
        }

        inventory.setItem(31, createCloseButton());

        player.openInventory(inventory);
    }

    private static void createBorder(Inventory inventory) {
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.text( " "));
            borderMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            borderItem.setItemMeta(borderMeta);
        }

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem(MENU_SIZE - 9 + i, borderItem);
        }

        for (int i = 1; i < MENU_SIZE / 9 - 1; i++) {
            inventory.setItem(i * 9, borderItem);
            inventory.setItem(i * 9 + 8, borderItem);
        }

        ItemStack fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.text( " "));
            fillerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            fillerItem.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < MENU_SIZE; i++) {
            final int slot = i;
            if (inventory.getItem(slot) == null) {
                if (settingDisplays.values().stream().anyMatch(display -> display.slot() == slot)) {
                    continue;
                }
                if (slot == 31) continue;
                inventory.setItem(slot, fillerItem);
            }
        }
    }

    private static @NotNull ItemStack createCloseButton() {
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = closeButton.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(ChatUtils.colorize("&cClose")));
            meta.lore(List.of(Component.text(ChatUtils.colorize("&7Click to close this menu"))));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            closeButton.setItemMeta(meta);
        }
        return closeButton;
    }


    private static @NotNull ItemStack createSettingItem(Setting<?> setting, SettingDisplay display, Object value) {
        boolean isEnabled = value instanceof Boolean ? (Boolean) value : true;

        Material material = isEnabled ? display.enabledMaterial() : display.disabledMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String statusIndicator = isEnabled ?
                    ChatUtils.colorize("&a&l✓ "):
                    ChatUtils.colorize("&c&l✗ ");

            meta.displayName(Component.text(ChatUtils.colorize(statusIndicator + "&e" + setting.getDisplayName())));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            List<String> lore = new ArrayList<>();

            if (value instanceof Boolean) {
                lore.add(isEnabled ?
                        ChatUtils.colorize("&a● ENABLED"):
                        ChatUtils.colorize("&c● DISABLED"));

                if (setting.getKey().equals("pvp")) {
                    Player player = Bukkit.getPlayer(UUID.randomUUID());
                    if (player != null) {
                        long cooldownTime = CooldownManager.getRemainingTime(player.getUniqueId(), "pvp_toggle");
                        if (cooldownTime > 0) {
                            lore.add(ChatUtils.colorize("&cCooldown: " + cooldownTime + "s"));
                        }
                    }
                }
            } else if (value instanceof PlayerTime) {
                PlayerTime time = (PlayerTime) value;
                String timeDisplay = switch (time) {
                    case DAY -> ChatUtils.colorize("&eDay");
                    case NIGHT -> ChatUtils.colorize("&1Night");
                    case SUNSET -> ChatUtils.colorize("&6Sunset");
                };
                lore.add(ChatUtils.colorize("&bCurrent: " + timeDisplay));
            }

            lore.add("");
            lore.add(ChatUtils.colorize("&7" + display.description()));
            lore.add("");
            lore.add(ChatUtils.colorize("&e» Click to toggle"));

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
            meta.displayName(Component.text(ChatUtils.colorize("&c❌ " + ChatUtils.colorize("&7" + setting.getDisplayName()))));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils.colorize("&c● UNAVAILABLE"));
            lore.add("");
            lore.add(ChatUtils.colorize("&7" + display.description()));
            lore.add("");
            lore.add(ChatUtils.colorize("&c» " + reason));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static @Nullable String getSettingKeyFromSlot(int slot) {
        String key = settingDisplays.entrySet().stream()
                .filter(entry -> entry.getValue().slot() == slot)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (key != null) {
            boolean isLobby = plugin != null && "lobby".equalsIgnoreCase(plugin.getServerType());
            boolean isSurvival = plugin != null && "survival".equalsIgnoreCase(plugin.getServerType());

            boolean isLobbyOnlySetting = key.equals("players_visibility") || key.equals("player_time");
            boolean isSurvivalOnlySetting = key.equals("pvp") || key.equals("tpa") || key.equals("actionbar");

            if (!isLobby && isLobbyOnlySetting) {
                return null;
            }

            if (!isSurvival && isSurvivalOnlySetting) {
                return null;
            }
        }

        return key;
    }

    public static boolean isCloseButton(int slot) {
        return slot == 31;
    }

    public static String getMenuTitle() {
        return MENU_TITLE;
    }

    public static class SettingsMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, MENU_SIZE, MENU_TITLE);
        }
    }
    public record SettingDisplay(Material disabledMaterial, Material enabledMaterial, int slot, String description) {
    }
}
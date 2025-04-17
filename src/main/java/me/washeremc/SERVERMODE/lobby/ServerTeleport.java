package me.washeremc.SERVERMODE.lobby;


import me.washeremc.Core.proxy.PluginMessage;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Core.utils.GuiItems;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServerTeleport implements Listener {

    private static Washere plugin;
    private static final Map<Player, Inventory> openGUIs = new HashMap<>();

    public static void initialize(Washere pluginInstance) {
        if (pluginInstance == null) throw new IllegalArgumentException("ServerTeleport pluginInstance cannot be null!");
        plugin = pluginInstance;
        plugin.getServer().getPluginManager().registerEvents(new ServerTeleport(), plugin);
    }

    public static void openServerTeleport(Player player) {
        if (plugin == null) return;

        PluginMessage.requestServerStatus(player);
        Inventory serversGui = createServersGui(player);
        openGUIs.put(player, serversGui);
        player.openInventory(serversGui);
    }

    public static @NotNull Inventory createServersGui(@NotNull Player ignoredPlayer) {
        Inventory serverGui = Bukkit.createInventory(null, 9, Component.text(ChatUtils.colorize("&8Select a Server")));

        int slot = 0;
        for (String server : Objects.requireNonNull(plugin.getConfig().getConfigurationSection("servers")).getKeys(false)) {
            if (PluginMessage.isServerOnline(server)) continue;

            ItemStack serverItem = GuiItems.createItem(Material.GREEN_WOOL, "§e§l" + server, List.of(
                    "",
                    "§7Status: §aOnline",
                    "§eClick to teleport."
            ));

            serverGui.setItem(slot++, serverItem);
        }

        return serverGui;
    }

    public static void updateAllGUIs() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Map.Entry<Player, Inventory> entry : openGUIs.entrySet()) {
                Player player = entry.getKey();
                Inventory gui = entry.getValue();

                if (player.getOpenInventory().title().equals(Component.text(ChatUtils.colorize("&8Select a Server")))) {
                    gui.clear();

                    int slot = 0;
                    for (String server : Objects.requireNonNull(plugin.getConfig().getConfigurationSection("servers")).getKeys(false)) {
                        if (PluginMessage.isServerOnline(server)) continue;

                        ItemStack serverItem = GuiItems.createItem(Material.GREEN_WOOL, "§e§l" + server, List.of(
                                "",
                                "§7Status: §aOnline",
                                "§eClick to teleport."
                        ));

                        gui.setItem(slot++, serverItem);
                    }

                    player.updateInventory();
                }
            }
        });
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text(ChatUtils.colorize("&8Select a Server")))) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            Component displayNameComponent = clickedItem.getItemMeta().displayName();
            String serverName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(Objects.requireNonNull(displayNameComponent)).replace("§e§l", "");


            PluginMessage.connect(player, serverName);
            player.closeInventory();
        }
    }
}

package me.washeremc.SERVERMODE.survival.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.jetbrains.annotations.NotNull;

public class RecipeInventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getView().title().toString().toLowerCase().contains("recipe:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (event.getView().title().toString().toLowerCase().contains("recipe:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMove(@NotNull InventoryMoveItemEvent event) {
        if (event.getDestination().getViewers().isEmpty()) {
            return;
        }

        String title = event.getDestination().getViewers().getFirst().getOpenInventory().title().toString().toLowerCase();
        if (title.contains("recipe:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryInteract(@NotNull InventoryInteractEvent event) {
        if (event.getView().title().toString().toLowerCase().contains("recipe:")) {
            event.setCancelled(true);
        }
    }
}
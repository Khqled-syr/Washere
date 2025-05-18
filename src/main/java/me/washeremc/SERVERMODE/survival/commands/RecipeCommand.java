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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;


@SuppressWarnings("deprecation")
public class RecipeCommand implements CommandExecutor, TabCompleter {

    private final Washere plugin;

    private boolean isLobby() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    public RecipeCommand(Washere plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.colorizeMini("&cOnly players can use this command!"));
            return true;
        }

        if (isLobby()) {
            player.sendMessage(ChatUtils.colorizeMini("&cThis command is not available in this server."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        String cooldownKey = "recipe";
        if (CooldownManager.isOnCooldown(uuid, cooldownKey)) {
            long timeLeft = CooldownManager.getRemainingTime(uuid, cooldownKey);
            player.sendMessage(ChatUtils.colorizeMini("&cYou must wait &e" + timeLeft + "s &cbefore using this again!"));
            return true;
        }
        CooldownManager.setCooldown(uuid, cooldownKey, 3);

        if (args.length != 1) {
            player.sendMessage("Usage: /recipe <item>");
            return true;
        }

        Material material = Material.matchMaterial(args[0]);
        if (material == null || !material.isItem()) {
            player.sendMessage("Invalid item!");
            return true;
        }

        showRecipeGUI(player, material);
        return true;
    }

    public void showRecipeGUI(Player player, Material result) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String title = "recipe: " + result.name().toLowerCase(); // No colors, lowercase
            Inventory craftingInventory = Bukkit.createInventory( null, InventoryType.WORKBENCH, Component.text( title));

            Optional<Recipe> recipeOpt = Bukkit.getRecipesFor(new ItemStack(result)).stream()
                    .filter(r -> r instanceof ShapedRecipe || r instanceof ShapelessRecipe)
                    .findFirst();

            if (recipeOpt.isPresent()) {
                Recipe recipe = recipeOpt.get();
                if (recipe instanceof ShapedRecipe shaped) {
                    Map<Character, ItemStack> ingredientMap = shaped.getIngredientMap();

                    String[] shape = shaped.getShape();
                    int slot = 1;

                    for (String row : shape) {
                        for (char key : row.toCharArray()) {
                            if (ingredientMap.containsKey(key) && ingredientMap.get(key) != null) {
                                craftingInventory.setItem(slot, ingredientMap.get(key));
                            }
                            slot++;
                        }
                    }
                } else if (recipe instanceof ShapelessRecipe shapeless) {
                    int slot = 1;
                    for (ItemStack item : shapeless.getIngredientList()) {
                        craftingInventory.setItem(slot++, item);
                    }
                }
            }

            craftingInventory.setItem(0, new ItemStack(result));

            player.openInventory(craftingInventory);
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(Material::name)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
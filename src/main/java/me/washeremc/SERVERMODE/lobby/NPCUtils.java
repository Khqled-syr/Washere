package me.washeremc.SERVERMODE.lobby;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Core.proxy.PluginMessage;
import me.washeremc.Core.utils.ChatUtils;
import me.washeremc.Washere;
import org.bukkit.*;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class NPCUtils implements Listener {
    private final Washere plugin;
    private final NamespacedKey npcKey;
    private File npcFile;
    private FileConfiguration npcConfig;
    private boolean initialized = false;

    public NPCUtils(Washere plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "npc_id");

        if (isLobbyMode()) {
            initializeNpcConfig();
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
            Bukkit.getPluginManager().registerEvents(this, plugin);
            initialized = true;
            plugin.getLogger().info("NPC system initialized.");
        } else {
            plugin.getLogger().info("NPC system not initialized - not in lobby mode.");
        }
    }

    private boolean isLobbyMode() {
        return "lobby".equalsIgnoreCase(plugin.getServerType());
    }

    private void initializeNpcConfig() {
        npcFile = new File(plugin.getDataFolder(), "npcs.yml");

        if (!npcFile.exists()) {
            File parentDir = npcFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                plugin.getLogger().severe("Failed to create parent directories for npcs.yml");
                return;
            }

            try {
                if (!npcFile.createNewFile()) {
                    plugin.getLogger().severe("Failed to create npcs.yml (already exists or unknown error)");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create npcs.yml", e);
            }
        }

        npcConfig = YamlConfiguration.loadConfiguration(npcFile);
    }

    private void saveNpcConfig() {
        if (!initialized || npcFile == null || npcConfig == null) return;

        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save npcs.yml", e);
        }
    }

    public void createNPC(@NotNull Player player, String npcName, String action) {
        if (!initialized) {
            player.sendMessage(ChatUtils.colorizeMini("&cNPC system is not available on this server."));
            return;
        }

        Location loc = player.getLocation();
        UUID npcId = UUID.randomUUID();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            npcName = PlaceholderAPI.setPlaceholders(player, npcName);
        }

        Villager npc = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        npc.customName(Component.text(npcName).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, npcId.toString());

        applyPlayerSkin(npc, player);

        saveNPC(npcId, action, npcName);
        player.sendMessage(ChatUtils.colorizeMini("&aNPC created with name: &e" + npcName + " &7(ID: " + npcId + ")"));
    }

    private void applyPlayerSkin(@NotNull Villager ignoredNpc, @NotNull Player player) {
        PlayerProfile profile = Bukkit.createProfile(player.getUniqueId());
        profile.complete();

        Collection<ProfileProperty> properties = profile.getProperties();
        if (!properties.isEmpty()) {
            for (ProfileProperty property : properties) {
                profile.setProperty(property);
            }
        }
    }

    public void deleteNPC(Player player, String npcIdStr) {
        if (!initialized) {
            player.sendMessage(ChatUtils.colorizeMini("&cNPC system is not available on this server."));
            return;
        }

        try {
            UUID npcId = UUID.fromString(npcIdStr);
            boolean found = false;

            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Villager villager) {
                        String storedId = villager.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
                        if (storedId != null && storedId.equals(npcId.toString())) {
                            villager.remove();
                            found = true;
                        }
                    }
                }
            }

            if (found) {
                removeNPCFromConfig(npcId);
                player.sendMessage(ChatUtils.colorizeMini("&aNPC (ID: " + npcId + ") deleted successfully!"));
            } else {
                player.sendMessage(ChatUtils.colorizeMini("&cNPC with ID " + npcId + " not found!"));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.colorizeMini("&cInvalid NPC ID! Use a valid UUID."));
        }
    }

    @EventHandler
    public void onNPCClick(@NotNull PlayerInteractAtEntityEvent event) {
        if (!initialized) return;

        if (event.getRightClicked() instanceof Villager villager) {
            String npcIdStr = villager.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
            if (npcIdStr != null) {
                String action = npcConfig.getString(npcIdStr + ".action");
                if (action != null) {
                    Player player = event.getPlayer();

                    if (action.startsWith("cmd:")) {
                        // Execute command as player with proper slash prefix
                        String command = action.substring(4);
                        // Dispatch command properly with slash added if needed
                        Bukkit.dispatchCommand(player, command.startsWith("/") ? command.substring(1) : command);
                    } else if (action.startsWith("console:")) {
                        // Execute command as console
                        String command = action.substring(8);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                command.startsWith("/") ? command.substring(1) : command);
                    } else if (action.startsWith("connect:")) {
                        // Direct server connection
                        String server = action.substring(8);
                        PluginMessage.connect(player, server);
                    } else {
                        // Default behavior - assume it's a server name
                        PluginMessage.connect(player, action);
                    }
                }
            }
        }
    }

    private void saveNPC(@NotNull UUID npcId, String action, String npcName) {
        if (!initialized) return;

        npcConfig.set(npcId + ".action", action);
        npcConfig.set(npcId + ".name", npcName);
        saveNpcConfig();
    }

    private void removeNPCFromConfig(@NotNull UUID npcId) {
        if (!initialized) return;

        npcConfig.set(npcId.toString(), null);
        saveNpcConfig();
    }

    public void loadNPCs() {
        if (!initialized) return;

        Set<String> keys = npcConfig.getKeys(false);

        if (!keys.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Villager villager) {
                            String npcIdStr = villager.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
                            if (npcIdStr == null || !keys.contains(npcIdStr)) {
                                villager.remove();
                            }
                        }
                    }
                }
            }, 20L);
        }
    }
}
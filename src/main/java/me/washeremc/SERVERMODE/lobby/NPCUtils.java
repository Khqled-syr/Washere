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

        // Only initialize if we're in lobby mode
        if (isLobbyMode()) {
            initializeNpcConfig();
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
            Bukkit.getPluginManager().registerEvents(this, plugin);
            initialized = true;
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

    public void createNPC(@NotNull Player player, String npcName, String serverName) {
        if (!initialized) {
            player.sendMessage(ChatUtils.colorize("&cNPC system is not available on this server."));
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

        saveNPC(npcId, serverName, npcName);
        player.sendMessage(ChatUtils.colorize("&aNPC created with name: &e" + npcName + " &7(ID: " + npcId + ")"));
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
            player.sendMessage(ChatUtils.colorize("&cNPC system is not available on this server."));
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
                player.sendMessage(ChatUtils.colorize("&aNPC (ID: " + npcId + ") deleted successfully!"));
            } else {
                player.sendMessage(ChatUtils.colorize("&cNPC with ID " + npcId + " not found!"));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.colorize("&cInvalid NPC ID! Use a valid UUID."));
        }
    }

    @EventHandler
    public void onNPCClick(@NotNull PlayerInteractAtEntityEvent event) {
        if (!initialized) return;

        if (event.getRightClicked() instanceof Villager villager) {
            String npcIdStr = villager.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
            if (npcIdStr != null) {
                String server = npcConfig.getString(npcIdStr + ".server");
                if (server != null) {
                    PluginMessage.connect(event.getPlayer(), server);
                }
            }
        }
    }

    private void saveNPC(@NotNull UUID npcId, String serverName, String npcName) {
        if (!initialized) return;

        npcConfig.set(npcId.toString() + ".server", serverName);
        npcConfig.set(npcId.toString() + ".name", npcName);
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
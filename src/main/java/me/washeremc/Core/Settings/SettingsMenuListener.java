package me.washeremc.Core.Settings;

import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SettingsMenuListener implements Listener {

    // Setting key constants
    private static final String SETTING_PLAYER_TIME = "player_time";
    private static final String SETTING_PVP = "pvp";

    // Sound constants
    private static final float SOUND_VOLUME_LOW = 0.5f;
    private static final float SOUND_VOLUME_HIGH = 1.0f;
    private static final float SOUND_PITCH_LOW = 0.6f;
    private static final float SOUND_PITCH_MEDIUM = 0.8f;
    private static final float SOUND_PITCH_HIGH = 1.2f;

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        // Early return if not a player or not our menu
        if (!(event.getWhoClicked() instanceof Player player) || 
            !(event.getInventory().getHolder() instanceof SettingsMenu.SettingsMenuHolder)) {
            return;
        }

        event.setCancelled(true); // Prevent taking items

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return; // Click was outside the inventory
        }

        // Handle close button click
        if (SettingsMenu.isCloseButton(slot)) {
            player.closeInventory();
            playCloseSound(player);
            return;
        }

        String settingKey = SettingsMenu.getSettingKeyFromSlot(slot);
        if (settingKey == null) {
            return; // Not a setting slot
        }

        // Handle setting toggle
        handleSettingToggle(player, settingKey);
    }

    /**
     * Handles the toggling of a setting
     * 
     * @param player The player toggling the setting
     * @param settingKey The key of the setting to toggle
     */
    private void handleSettingToggle(@NotNull Player player, @NotNull String settingKey) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(settingKey, "Setting key cannot be null");

        boolean needsRefresh = true;

        // Handle player time setting separately
        if (SETTING_PLAYER_TIME.equals(settingKey)) {
            PlayerTime currentTime = SettingsManager.getSettingValue(player, settingKey);
            playPlayerTimeSound(player, currentTime);
        }

        // Get current state for non-player-time settings
        boolean wasEnabled = !SETTING_PLAYER_TIME.equals(settingKey) && 
                             Boolean.TRUE.equals(SettingsManager.getSettingValue(player, settingKey));

        // Toggle the setting - if false is returned, we should abort (cooldown active)
        boolean toggleSuccessful = SettingsManager.toggleSetting(player, settingKey);

        if (toggleSuccessful) {
            // Play appropriate sound based on new state (except for player_time which already played)
            if (!SETTING_PLAYER_TIME.equals(settingKey)) {
                boolean isNowEnabled = Boolean.TRUE.equals(SettingsManager.getSettingValue(player, settingKey));
                playToggleSound(player, settingKey, isNowEnabled, wasEnabled);
            }
        } else {
            // Cooldown active or other issue - play error sound
            playErrorSound(player);
            needsRefresh = false; // No need to refresh if toggle failed
        }

        // Refresh the menu only if needed
        if (needsRefresh) {
            Bukkit.getScheduler().runTask(SettingsMenu.getPlugin(), () -> SettingsMenu.openSettingsMenu(player));
        }
    }

    /**
     * Plays a sound for the player at their location
     * 
     * @param player The player to play the sound for
     * @param sound The sound to play
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    private void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(sound, "Sound cannot be null");
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Plays the menu close sound for a player
     * 
     * @param player The player to play the sound for
     */
    private void playCloseSound(@NotNull Player player) {
        playSound(player, Sound.BLOCK_CHEST_CLOSE, SOUND_VOLUME_LOW, SOUND_VOLUME_HIGH);
    }

    /**
     * Plays the appropriate sound for a player time setting
     * 
     * @param player The player to play the sound for
     * @param time The current player time setting
     */
    private void playPlayerTimeSound(@NotNull Player player, @NotNull PlayerTime time) {
        Sound sound = Sound.BLOCK_NOTE_BLOCK_HARP;
        float pitch = switch (time) {
            case DAY -> SOUND_PITCH_MEDIUM;
            case NIGHT -> SOUND_PITCH_LOW;
            case SUNSET -> SOUND_VOLUME_HIGH;
        };
        playSound(player, sound, SOUND_VOLUME_LOW, pitch);
    }

    /**
     * Plays the appropriate sound for a boolean setting toggle
     * 
     * @param player The player to play the sound for
     * @param settingKey The setting key that was toggled
     * @param enabled Whether the setting is now enabled
     * @param wasEnabled Whether the setting was previously enabled
     */
    private void playToggleSound(@NotNull Player player, @NotNull String settingKey, boolean enabled, boolean wasEnabled) {
        if (SETTING_PVP.equals(settingKey)) {
            if (enabled && !wasEnabled) {
                // PVP turned on - play combat sound
                playSound(player, Sound.ITEM_ARMOR_EQUIP_DIAMOND, SOUND_VOLUME_HIGH, SOUND_VOLUME_HIGH);
            } else if (!enabled && wasEnabled) {
                // PVP turned off - play peaceful sound
                playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SOUND_VOLUME_HIGH, SOUND_VOLUME_HIGH);
            }
        } else {
            // Regular toggle sounds
            if (enabled != wasEnabled) {
                playSound(player, Sound.BLOCK_LEVER_CLICK, SOUND_VOLUME_LOW, 
                    enabled ? SOUND_PITCH_HIGH : SOUND_PITCH_MEDIUM);
            }
        }
    }

    /**
     * Plays an error sound for the player
     * 
     * @param player The player to play the sound for
     */
    private void playErrorSound(@NotNull Player player) {
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, SOUND_VOLUME_LOW, SOUND_PITCH_LOW);
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsMenu.SettingsMenuHolder)) {
            return;
        }

        // Play a sound when closing the menu
        if (event.getPlayer() instanceof Player player) {
            playCloseSound(player);
        }
    }
}

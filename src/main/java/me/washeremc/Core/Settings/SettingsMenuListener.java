package me.washeremc.Core.Settings;

import me.washeremc.Core.Settings.PlayerSetting.PlayerTime;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SettingsMenuListener implements Listener {

    private static final String SETTING_PLAYER_TIME = "player_time";
    private static final String SETTING_PVP = "pvp";

    private static final float SOUND_VOLUME_LOW = 0.5f;
    private static final float SOUND_VOLUME_HIGH = 1.0f;
    private static final float SOUND_PITCH_LOW = 0.6f;
    private static final float SOUND_PITCH_MEDIUM = 0.8f;
    private static final float SOUND_PITCH_HIGH = 1.2f;

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || 
            !(event.getInventory().getHolder() instanceof SettingsMenu.SettingsMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (SettingsMenu.isCloseButton(slot)) {
            player.closeInventory();
            return;
        }

        String settingKey = SettingsMenu.getSettingKeyFromSlot(slot);
        if (settingKey == null) {
            return;
        }

        handleSettingToggle(player, settingKey);
    }

    private void handleSettingToggle(@NotNull Player player, @NotNull String settingKey) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(settingKey, "Setting key cannot be null");

        boolean needsRefresh = true;

        if (SETTING_PLAYER_TIME.equals(settingKey)) {
            PlayerTime currentTime = SettingsManager.getSettingValue(player, settingKey);
            playPlayerTimeSound(player, Objects.requireNonNull(currentTime));
        }

        boolean wasEnabled = !SETTING_PLAYER_TIME.equals(settingKey) && 
                             Boolean.TRUE.equals(SettingsManager.getSettingValue(player, settingKey));

        boolean toggleSuccessful = SettingsManager.toggleSetting(player, settingKey);

        if (toggleSuccessful) {
            if (!SETTING_PLAYER_TIME.equals(settingKey)) {
                boolean isNowEnabled = Boolean.TRUE.equals(SettingsManager.getSettingValue(player, settingKey));
                playToggleSound(player, settingKey, isNowEnabled, wasEnabled);
            }
        } else {
            playErrorSound(player);
            needsRefresh = false;
        }

        if (needsRefresh) {
            Bukkit.getScheduler().runTask(SettingsMenu.getPlugin(), () -> SettingsMenu.openSettingsMenu(player));
        }
    }

    private void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(sound, "Sound cannot be null");
        player.playSound(player.getLocation(), sound, volume, pitch);
    }


    private void playPlayerTimeSound(@NotNull Player player, @NotNull PlayerTime time) {
        Sound sound = Sound.BLOCK_NOTE_BLOCK_HARP;
        float pitch = switch (time) {
            case DAY -> SOUND_PITCH_MEDIUM;
            case NIGHT -> SOUND_PITCH_LOW;
            case SUNSET -> SOUND_VOLUME_HIGH;
        };
        playSound(player, sound, SOUND_VOLUME_LOW, pitch);
    }

    private void playToggleSound(@NotNull Player player, @NotNull String settingKey, boolean enabled, boolean wasEnabled) {
        if (SETTING_PVP.equals(settingKey)) {
            if (enabled && !wasEnabled) {
                playSound(player, Sound.ITEM_ARMOR_EQUIP_DIAMOND, SOUND_VOLUME_HIGH, SOUND_VOLUME_HIGH);
            } else if (!enabled && wasEnabled) {
                playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SOUND_VOLUME_HIGH, SOUND_VOLUME_HIGH);
            }
        } else {
            if (enabled != wasEnabled) {
                playSound(player, Sound.BLOCK_LEVER_CLICK, SOUND_VOLUME_LOW, 
                    enabled ? SOUND_PITCH_HIGH : SOUND_PITCH_MEDIUM);
            }
        }
    }
    private void playErrorSound(@NotNull Player player) {
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, SOUND_VOLUME_LOW, SOUND_PITCH_LOW);
    }
}

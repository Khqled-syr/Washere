package me.washeremc.Core.PlayerTime;

import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayTimeTracker {
    private final Washere plugin;
    private final Map<UUID, Long> playerTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> accumulatedMinutes = new ConcurrentHashMap<>();
    private static final long SAVE_INTERVAL = 300L; // 5 minutes in seconds
    private File timeFile;
    private FileConfiguration timeConfig;

    public PlayTimeTracker(@NotNull Washere plugin) {
        this.plugin = plugin;

        if (DatabaseManager.useMySQL) {
            createTable();
        } else {
            setupConfig();
        }
        startSaveTask();
    }

    private void setupConfig() {
        timeFile = new File(plugin.getDataFolder(), "playtime.yml");
        if (!timeFile.exists()) {
            timeFile.getParentFile().mkdirs();
            plugin.saveResource("playtime.yml", false);
        }
        timeConfig = YamlConfiguration.loadConfiguration(timeFile);
    }

    private void saveConfig() {
        if (!DatabaseManager.useMySQL && timeConfig != null) {
            try {
                timeConfig.save(timeFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save playtime.yml", e);
            }
        }
    }

    private void createTable() {
        try (Connection conn = DatabaseManager.dataSource.getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS player_playtime (
                    uuid VARCHAR(36) PRIMARY KEY,
                    playtime_hours INT NOT NULL DEFAULT 0,
                    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX (last_update)
                )
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player_playtime table", e);
        }
    }

    public void startTracking(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        sessionStartTimes.put(uuid, System.currentTimeMillis());
        loadPlayerTime(uuid);
    }

    public void stopTracking(@NotNull UUID uuid) {
        updateAndSaveTime(uuid);
        sessionStartTimes.remove(uuid);
        accumulatedMinutes.remove(uuid);
    }

    public void forceSave(@NotNull UUID uuid) {
        updateAndSaveTime(uuid);
        if (!DatabaseManager.useMySQL) {
            saveConfig();
        }
    }

    private void loadPlayerTime(@NotNull UUID uuid) {
        if (DatabaseManager.useMySQL) {
            loadMySQLTime(uuid);
        } else {
            loadYamlTime(uuid);
        }
    }

    private void loadMySQLTime(@NotNull UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = DatabaseManager.dataSource.getConnection()) {
                String sql = "SELECT playtime_hours FROM player_playtime WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            playerTimes.put(uuid, (long) rs.getInt("playtime_hours"));
                        } else {
                            playerTimes.put(uuid, 0L);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load playtime for " + uuid, e);
            }
        });
    }

    private void loadYamlTime(@NotNull UUID uuid) {
        if (timeConfig == null) return;
        String path = "players." + uuid;
        long hours = timeConfig.getLong(path + ".hours", 0L);
        playerTimes.put(uuid, hours);
    }

    private void startSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (UUID uuid : new HashSet<>(sessionStartTimes.keySet())) {
                updateAndSaveTime(uuid);
            }
            if (!DatabaseManager.useMySQL) {
                saveConfig();
            }
        }, SAVE_INTERVAL * 20L, SAVE_INTERVAL * 20L);
    }

    private void updateAndSaveTime(@NotNull UUID uuid) {
        Long startTime = sessionStartTimes.get(uuid);
        if (startTime == null) return;

        long currentTime = System.currentTimeMillis();
        long sessionMinutes = (currentTime - startTime) / 60000;

        // Accumulate minutes
        long totalMinutes = accumulatedMinutes.getOrDefault(uuid, 0L) + sessionMinutes;
        long hours = totalMinutes / 60;
        long remainingMinutes = totalMinutes % 60;

        // Store remaining minutes for next session
        accumulatedMinutes.put(uuid, remainingMinutes);

        // Update hours if we have a full hour
        if (hours > 0) {
            long existingHours = playerTimes.getOrDefault(uuid, 0L);
            playerTimes.put(uuid, existingHours + hours);
            savePlayerTime(uuid, existingHours + hours);
        }

        // Reset session start time
        sessionStartTimes.put(uuid, System.currentTimeMillis());
    }

    private void savePlayerTime(@NotNull UUID uuid, long hours) {
        if (DatabaseManager.useMySQL) {
            saveMySQLTime(uuid, hours);
        } else {
            saveYamlTime(uuid, hours);
        }
    }

    private void saveMySQLTime(@NotNull UUID uuid, long hours) {
        try (Connection conn = DatabaseManager.dataSource.getConnection()) {
            String sql = """
                INSERT INTO player_playtime (uuid, playtime_hours, last_update)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    playtime_hours = ?,
                    last_update = CURRENT_TIMESTAMP
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setLong(2, hours);
                stmt.setLong(3, hours);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save playtime for " + uuid, e);
        }
    }

    private void saveYamlTime(@NotNull UUID uuid, long hours) {
        if (timeConfig == null) return;
        String path = "players." + uuid;
        timeConfig.set(path + ".hours", hours);
        timeConfig.set(path + ".last_update", System.currentTimeMillis());
        saveConfig();
    }

    public long getPlayerTime(@NotNull UUID uuid) {
        return playerTimes.getOrDefault(uuid, 0L);
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(sessionStartTimes.keySet())) {
            forceSave(uuid);
            stopTracking(uuid);
        }
        if (!DatabaseManager.useMySQL) {
            saveConfig();
        }
    }
}
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayTimeTracker {
    private final Washere plugin;
    private final Map<UUID, Long> playerTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> accumulatedMinutes = new ConcurrentHashMap<>();
    private static final long SAVE_INTERVAL = 300L;
    private File timeFile;
    private FileConfiguration timeConfig;
    private PreparedStatement batchInsertStmt;
    private PreparedStatement batchUpdateStmt;

    public PlayTimeTracker(@NotNull Washere plugin) {
        this.plugin = plugin;

        if (DatabaseManager.useMySQL) {
            createTable();
            prepareBatchStatements();
        } else {
            setupConfig();
            loadAllYamlData();
        }
        startSaveTask();
    }

    private void setupConfig() {
        timeFile = new File(plugin.getDataFolder(), "playtime.yml");
        if (!timeFile.exists()) {
            timeFile.getParentFile().mkdirs();
            try {
                timeFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create playtime.yml", e);
            }
        }
        timeConfig = YamlConfiguration.loadConfiguration(timeFile);
    }

    private void loadAllYamlData() {
        if (timeConfig == null) return;

        if (timeConfig.getConfigurationSection("players") != null) {
            for (String uuidString : timeConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String path = "players." + uuidString;
                    long hours = timeConfig.getLong(path + ".hours", 0L);
                    long minutes = timeConfig.getLong(path + ".minutes", 0L);

                    playerTimes.put(uuid, hours);
                    accumulatedMinutes.put(uuid, minutes);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Invalid UUID in playtime.yml: " + uuidString);
                }
            }
        }
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
                    playtime_minutes INT NOT NULL DEFAULT 0,
                    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_last_update (last_update)
                )
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player_playtime table", e);
        }
    }

    private void prepareBatchStatements() {
        try {
            Connection conn = DatabaseManager.dataSource.getConnection();
            batchInsertStmt = conn.prepareStatement("""
                INSERT INTO player_playtime (uuid, playtime_hours, playtime_minutes, last_update)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    playtime_hours = VALUES(playtime_hours),
                    playtime_minutes = VALUES(playtime_minutes),
                    last_update = CURRENT_TIMESTAMP
            """);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to prepare batch statements", e);
        }
    }

    public void startTracking(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        sessionStartTimes.put(uuid, System.currentTimeMillis());

        if (!playerTimes.containsKey(uuid) || !accumulatedMinutes.containsKey(uuid)) {
            loadPlayerTime(uuid);
        }
    }

    public void stopTracking(@NotNull UUID uuid) {
        updateAndSaveTime(uuid);
        sessionStartTimes.remove(uuid);
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
                String sql = "SELECT playtime_hours, playtime_minutes FROM player_playtime WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            playerTimes.put(uuid, (long) rs.getInt("playtime_hours"));
                            accumulatedMinutes.put(uuid, (long) rs.getInt("playtime_minutes"));
                        } else {
                            playerTimes.put(uuid, 0L);
                            accumulatedMinutes.put(uuid, 0L);
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
        long minutes = timeConfig.getLong(path + ".minutes", 0L);
        playerTimes.put(uuid, hours);
        accumulatedMinutes.put(uuid, minutes);
    }

    private void startSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (DatabaseManager.useMySQL) {
                batchSaveMySQL();
            } else {
                for (UUID uuid : sessionStartTimes.keySet()) {
                    updateAndSaveTime(uuid);
                }
                saveConfig();
            }
        }, SAVE_INTERVAL * 20L, SAVE_INTERVAL * 20L);
    }

    private void batchSaveMySQL() {
        List<UUID> playersToUpdate = new ArrayList<>(sessionStartTimes.keySet());
        if (playersToUpdate.isEmpty()) return;

        try (Connection conn = DatabaseManager.dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO player_playtime (uuid, playtime_hours, playtime_minutes, last_update)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    playtime_hours = VALUES(playtime_hours),
                    playtime_minutes = VALUES(playtime_minutes),
                    last_update = CURRENT_TIMESTAMP
            """)) {

                for (UUID uuid : playersToUpdate) {
                    updateTime(uuid);

                    stmt.setString(1, uuid.toString());
                    stmt.setLong(2, playerTimes.getOrDefault(uuid, 0L));
                    stmt.setLong(3, accumulatedMinutes.getOrDefault(uuid, 0L));
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to batch save playtime data", e);
        }
    }

    private void updateTime(@NotNull UUID uuid) {
        Long startTime = sessionStartTimes.get(uuid);
        if (startTime == null) return;

        long currentTime = System.currentTimeMillis();
        long sessionMinutes = (currentTime - startTime) / 60000;

        long totalMinutes = accumulatedMinutes.getOrDefault(uuid, 0L) + sessionMinutes;
        long hours = totalMinutes / 60;
        long remainingMinutes = totalMinutes % 60;

        accumulatedMinutes.put(uuid, remainingMinutes);

        if (hours > 0) {
            long existingHours = playerTimes.getOrDefault(uuid, 0L);
            playerTimes.put(uuid, existingHours + hours);
        }

        sessionStartTimes.put(uuid, currentTime);
    }

    private void updateAndSaveTime(@NotNull UUID uuid) {
        updateTime(uuid);
        if (!DatabaseManager.useMySQL) {
            saveYamlTime(uuid, playerTimes.getOrDefault(uuid, 0L), accumulatedMinutes.getOrDefault(uuid, 0L));
        }
    }

    private void saveYamlTime(@NotNull UUID uuid, long hours, long minutes) {
        if (timeConfig == null) return;
        String path = "players." + uuid;
        timeConfig.set(path + ".hours", hours);
        timeConfig.set(path + ".minutes", minutes);
        timeConfig.set(path + ".last_update", System.currentTimeMillis());
    }

    public long getPlayerTime(@NotNull UUID uuid) {
        return playerTimes.getOrDefault(uuid, 0L);
    }

    public long getPlayerMinutes(@NotNull UUID uuid) {
        Long startTime = sessionStartTimes.get(uuid);
        if (startTime == null) {
            return accumulatedMinutes.getOrDefault(uuid, 0L);
        }

        long currentTime = System.currentTimeMillis();
        long sessionMinutes = (currentTime - startTime) / 60000;
        return (accumulatedMinutes.getOrDefault(uuid, 0L) + sessionMinutes) % 60;
    }

    public void shutdown() {
        if (DatabaseManager.useMySQL) {
            batchSaveMySQL();
        } else {
            for (UUID uuid : sessionStartTimes.keySet()) {
                forceSave(uuid);
            }
            saveConfig();
        }

        sessionStartTimes.clear();
        accumulatedMinutes.clear();
        playerTimes.clear();

        try {
            if (batchInsertStmt != null) batchInsertStmt.close();
            if (batchUpdateStmt != null) batchUpdateStmt.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close prepared statements", e);
        }
    }
}
package me.washeremc.Core.PlayerTime;

import me.washeremc.Core.database.DatabaseManager;
import me.washeremc.Washere;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerTimeManager {
    private final Washere plugin;
    private final Map<UUID, Long> playerTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();
    private static final long SAVE_INTERVAL = 3600L; // 1 hour in seconds

    public PlayerTimeManager(@NotNull Washere plugin) {
        this.plugin = plugin;
        createTable();
        startSaveTask();
    }

    private void createTable() {
        if (!DatabaseManager.useMySQL) {
            plugin.getLogger().warning("PlayerTime feature requires MySQL!");
            return;
        }

        try (Connection conn = DatabaseManager.dataSource.getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS player_playtime (
                    uuid VARCHAR(36) PRIMARY KEY,
                    playtime_hours INT NOT NULL DEFAULT 0,
                    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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

    public void stopTracking(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        updateAndSaveTime(uuid);
        sessionStartTimes.remove(uuid);
    }

    private void loadPlayerTime(@NotNull UUID uuid) {
        if (!DatabaseManager.useMySQL) return;

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

    private void startSaveTask() {
        // Run every hour
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (UUID uuid : sessionStartTimes.keySet()) {
                updateAndSaveTime(uuid);
            }
        }, SAVE_INTERVAL * 20L, SAVE_INTERVAL * 20L);
    }

    private void updateAndSaveTime(@NotNull UUID uuid) {
        Long startTime = sessionStartTimes.get(uuid);
        if (startTime == null) return;

        long currentTime = System.currentTimeMillis();
        long sessionHours = (currentTime - startTime) / 3600000;
        if (sessionHours < 1) return;

        sessionStartTimes.put(uuid, currentTime);

        // Update total time
        long totalHours = playerTimes.getOrDefault(uuid, 0L) + sessionHours;
        playerTimes.put(uuid, totalHours);

        // Save to database
        savePlayerTime(uuid, totalHours);
    }

    private void savePlayerTime(@NotNull UUID uuid, long hours) {
        if (!DatabaseManager.useMySQL) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = DatabaseManager.dataSource.getConnection()) {
                String sql = """
                    INSERT INTO player_playtime (uuid, playtime_hours)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE playtime_hours = ?
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
        });
    }

    public long getPlayerTime(@NotNull UUID uuid) {
        return playerTimes.getOrDefault(uuid, 0L);
    }

    public void shutdown() {
        for (UUID uuid : sessionStartTimes.keySet()) {
            updateAndSaveTime(uuid);
        }
        playerTimes.clear();
        sessionStartTimes.clear();
    }
}
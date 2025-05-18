package me.washeremc.Core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.washeremc.Core.Settings.Setting;
import me.washeremc.Core.Settings.SettingRegistry;
import me.washeremc.Washere;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {
    private static final int MAX_POOL_SIZE = 10, MIN_IDLE = 2, IDLE_TIMEOUT = 60000, CONNECTION_TIMEOUT = 30000, MAX_LIFETIME = 1800000, MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000L;
    public static HikariDataSource dataSource;
    public static boolean useMySQL;
    private static File settingsFile;
    public static FileConfiguration settingsConfig;
    private static Washere plugin;

    public static void initialize(@NotNull Washere pluginInstance) {
        plugin = Objects.requireNonNull(pluginInstance, "Plugin cannot be null");
        useMySQL = plugin.getConfig().getString("storage.type", "yaml").equalsIgnoreCase("mysql");
        try {
            if (useMySQL) {
                plugin.getLogger().info("Using MySQL for storage.");
                setupMySQL();
            } else {
                plugin.getLogger().info("Using YAML for storage.");
                setupYAML();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException(e);
        }
    }

    private static void setupMySQL() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl());
        config.setUsername(plugin.getConfig().getString("mysql.username", "root"));
        config.setPassword(plugin.getConfig().getString("mysql.password", ""));
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setIdleTimeout(IDLE_TIMEOUT);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setMaxLifetime(MAX_LIFETIME);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    private static void setupYAML() {
        settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                settingsFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                settingsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create settings.yml", e);
            }
        }
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
        if (!settingsConfig.contains("players")) settingsConfig.createSection("players");
    }

    @NotNull
    private static String buildJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&rewriteBatchedStatements=true",
                plugin.getConfig().getString("mysql.host", "localhost"),
                plugin.getConfig().getInt("mysql.port", 3306),
                plugin.getConfig().getString("mysql.database", "minecraft"));
    }

    private static void createTables() {
        if (!useMySQL) return;
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "player_settings", null);

            if (!tables.next()) {
                StringBuilder columns = new StringBuilder("uuid VARCHAR(36) PRIMARY KEY, selectedTag VARCHAR(255) DEFAULT NULL");
                for (Setting<?> setting : SettingRegistry.getSettings()) {
                    if (!setting.getKey().equals("selectedTag")) {
                        columns.append(", ").append(setting.getKey()).append(" ").append(getSqlType(setting));
                    }
                }
                executeUpdate("CREATE TABLE player_settings (" + columns + ");");
                plugin.getLogger().info("Created player_settings table in database");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check or create tables: " + e.getMessage(), e);
        }
    }

    private static @NotNull String getSqlType(@NotNull Setting<?> setting) {
        Object def = setting.getDefaultValue();
        if (def instanceof Boolean) return "BOOLEAN DEFAULT " + def;
        if (def instanceof Integer) return "INT DEFAULT " + def;
        if (def instanceof Double) return "DOUBLE DEFAULT " + def;
        if (def.getClass().isEnum()) return "VARCHAR(64) DEFAULT '" + def + "'";
        return "VARCHAR(255) DEFAULT '" + def + "'";
    }

    public static <T> @NotNull CompletableFuture<Void> saveSetting(UUID uuid, String key, T value) {
        return saveSettingWithRetry(uuid, key, value, 1);
    }

    private static <T> @NotNull CompletableFuture<Void> saveSettingWithRetry(UUID uuid, String key, T value, int attempt) {
        return CompletableFuture.runAsync(() -> {
            if (useMySQL) saveToMySQL(uuid, key, value instanceof Enum<?> ? value.toString() : value);
            else saveToYAML(uuid, key, value instanceof Enum<?> ? value.toString() : value);
        }).exceptionallyCompose(ex -> {
            if (attempt >= MAX_RETRIES) return CompletableFuture.failedFuture(ex);

            CompletableFuture<Void> delayedFuture = new CompletableFuture<>();

            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                    () -> delayedFuture.complete(null),
                    RETRY_DELAY / 50);
            return delayedFuture.thenCompose(ignored ->
                    saveSettingWithRetry(uuid, key, value, attempt + 1));
        });
    }

    private static void saveToMySQL(@NotNull UUID uuid, String key, Object value) {
        String sql = "INSERT INTO player_settings (uuid, " + key + ") VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE " + key + " = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setObject(2, value);
            ps.setObject(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save setting to MySQL: " + e.getMessage(), e);
        }
    }

    private static void saveToYAML(UUID uuid, String key, Object value) {
        settingsConfig.set("players." + uuid + "." + key, value);
        try {
            settingsConfig.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save settings.yml", e);
        }
    }

    @Contract("_, _, _ -> new")
    public static <T> @NotNull CompletableFuture<T> loadSetting(UUID uuid, String key, T defaultValue) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (useMySQL) return (T) loadFromMySQL(uuid, key, defaultValue);
                Object value = settingsConfig.get("players." + uuid + "." + key, defaultValue);
                if (defaultValue instanceof Enum && value instanceof String) {
                    try {
                        Class<Enum> enumClass = (Class<Enum>) defaultValue.getClass();
                        return (T) Enum.valueOf(enumClass, (String) value);
                    } catch (IllegalArgumentException e) {
                        return defaultValue;
                    }
                }
                return (T) value;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading setting: " + e.getMessage());
                return defaultValue;
            }
        });
    }

    private static Object loadFromMySQL(@NotNull UUID uuid, String key, Object defaultValue) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT " + key + " FROM player_settings WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(key);
                    if (defaultValue instanceof Enum && value instanceof String) {
                        try {
                            Class<Enum> enumClass = (Class<Enum>) defaultValue.getClass();
                            return Enum.valueOf(enumClass, (String) value);
                        } catch (IllegalArgumentException e) {
                            return defaultValue;
                        }
                    }
                    return value != null ? value : defaultValue;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load setting from database: " + e.getMessage());
        }
        return defaultValue;
    }

    public static void closeConnection() {
        if (useMySQL && dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        } else if (settingsConfig != null && settingsFile != null) {
            try {
                settingsConfig.save(settingsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save settings.yml: " + e.getMessage());
            }
        }
    }

    private static void executeUpdate(String sql) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQL error: " + e.getMessage());
        }
    }
}
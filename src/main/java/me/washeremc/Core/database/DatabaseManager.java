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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DatabaseManager {
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 2;
    private static final int IDLE_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int MAX_LIFETIME = 1800000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000L;

    private static Washere plugin;
    private static HikariDataSource dataSource;
    private static boolean useMySQL;
    private static volatile File settingsFile;
    private static volatile FileConfiguration settingsConfig;
    private static final Map<String, PreparedStatement> preparedStatements = new ConcurrentHashMap<>();






    public static void initialize(@NotNull Washere plugin) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        DatabaseManager.plugin = plugin;
        useMySQL = plugin.getConfig().getString("storage.type", "yaml").equalsIgnoreCase("mysql");

        try {
            if (useMySQL) {
                setupMySQL();
            } else {
                setupYAML();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }


    private static void setupMySQL() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("WashereCP");
        config.setJdbcUrl(buildJdbcUrl());
        config.setUsername(plugin.getConfig().getString("mysql.username", "root"));
        config.setPassword(plugin.getConfig().getString("mysql.password", ""));

        // Connection pool settings
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setIdleTimeout(IDLE_TIMEOUT);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setMaxLifetime(MAX_LIFETIME);

        // Performance optimizations
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
                boolean dirCreated = settingsFile.getParentFile().mkdirs();
                boolean fileCreated = settingsFile.createNewFile();
                if (!dirCreated && !settingsFile.getParentFile().exists()) {
                    plugin.getLogger().warning("Failed to create parent directories for settings file");
                }
                if (!fileCreated) {
                    plugin.getLogger().warning("Failed to create settings file");
                }
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

        // Drop the table to recreate it with all columns
        executeUpdate("DROP TABLE IF EXISTS player_settings;");

        StringBuilder columns = new StringBuilder();
        for (Setting<?> setting : SettingRegistry.getSettings()) {
            if (!columns.isEmpty()) columns.append(", ");
            columns.append(setting.getKey()).append(" ").append(getSqlType(setting));
        }

        executeUpdate("CREATE TABLE IF NOT EXISTS player_settings (uuid VARCHAR(36) PRIMARY KEY, " + columns + ");");
        plugin.getLogger().info("Database tables created successfully");
    }

    private static @NotNull String getSqlType(@NotNull Setting<?> setting) {
        Object defaultValue = setting.getDefaultValue();
        Class<?> type = defaultValue.getClass();

        if (type == Boolean.class) return "BOOLEAN DEFAULT " + defaultValue;
        if (type == Integer.class) return "INT DEFAULT " + defaultValue;
        if (type == Double.class) return "DOUBLE DEFAULT " + defaultValue;
        if (type.isEnum()) {
            // Store enums as strings, not serialized objects
            return "VARCHAR(64) DEFAULT '" + defaultValue + "'";
        }
        return "VARCHAR(255) DEFAULT '" + defaultValue + "'";
    }

    @Contract("_, _, _ -> new")
    public static <T> @NotNull CompletableFuture<Void> saveSetting(UUID uuid, String key, T value) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        return CompletableFuture.runAsync(() -> {
            for (int attempt = 1; true; attempt++) {
                try {
                    if (useMySQL) {
                        saveToMySQL(uuid, key, value);
                    } else {
                        saveToYAML(uuid, key, value instanceof Enum<?> ? value.toString() : value);
                    }
                    return;
                } catch (Exception e) {
                    if (attempt == MAX_RETRIES) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save setting after " + MAX_RETRIES + " attempts", e);
                        throw new RuntimeException("Failed to save setting", e);
                    }
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                }
            }
        });
    }


    private static void saveToMySQL(UUID uuid, String key, Object value) {
        String sql = "INSERT INTO player_settings (uuid, " + key + ") VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE " + key + " = VALUES(" + key + ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = preparedStatements.computeIfAbsent(sql,
                     k -> createPreparedStatement(conn, sql))) {

            ps.setString(1, uuid.toString());
            ps.setObject(2, value instanceof Enum<?> ? value.toString() : value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save to MySQL", e);
        }
    }


    private static void saveToYAML(UUID uuid, String key, Object value) {
        settingsConfig.set("players." + uuid + "." + key, value);
        saveYAML();
    }

    @Contract("_, _, _ -> new")
    @SuppressWarnings("unchecked")
    public static <T> @NotNull CompletableFuture<T> loadSetting(UUID uuid, String key, T defaultValue) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (useMySQL) {
                    return (T) loadFromMySQL(uuid, key, defaultValue);
                } else {
                    // For YAML, handle enum conversion
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
                }
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
                    if (value != null) {
                        // Convert string back to enum if needed
                        if (defaultValue instanceof Enum && value instanceof String) {
                            try {
                                @SuppressWarnings("unchecked")
                                Class<Enum> enumClass = (Class<Enum>) defaultValue.getClass();
                                return Enum.valueOf(enumClass, (String) value);
                            } catch (IllegalArgumentException e) {
                                return defaultValue;
                            }
                        }
                        return value;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load setting from database: " + e.getMessage());
        }
        return defaultValue;
    }

    private static void saveYAML() {
        try {
            settingsConfig.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save settings.yml: " + e.getMessage());
        }
    }

    private static PreparedStatement createPreparedStatement(@NotNull Connection conn, String sql) {
        try {
            return conn.prepareStatement(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create prepared statement", e);
        }
    }


    public static void closeConnection() {
        if (useMySQL && dataSource != null && !dataSource.isClosed()) {
            preparedStatements.clear();
            dataSource.close();
        } else if (settingsConfig != null && settingsFile != null) {
            saveYAML();
        }
    }


    private static void executeUpdate(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQL error: " + e.getMessage());
        }
    }
}
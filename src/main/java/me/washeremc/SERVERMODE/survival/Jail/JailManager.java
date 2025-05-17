package me.washeremc.SERVERMODE.survival.Jail;

    import me.washeremc.Washere;
    import org.bukkit.Bukkit;
    import org.bukkit.Location;
    import org.bukkit.configuration.ConfigurationSection;
    import org.bukkit.configuration.file.FileConfiguration;
    import org.bukkit.configuration.file.YamlConfiguration;
    import org.bukkit.entity.Player;
    import org.jetbrains.annotations.NotNull;

    import java.io.File;
    import java.io.IOException;
    import java.util.Map;
    import java.util.Objects;
    import java.util.UUID;
    import java.util.concurrent.ConcurrentHashMap;

    public class JailManager {
        private File jailFile;
        private FileConfiguration jailConfig;
        private final Washere plugin;
        private Location jailLocation;
        private Location releaseLocation;
        private final Map<UUID, JailData> jailedPlayers = new ConcurrentHashMap<>();
        private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();

        public JailManager(@NotNull Washere plugin) {
            this.plugin = plugin;
        }

        public void initialize() {
            if (isSurvival()) {
                this.jailFile = new File(plugin.getDataFolder(), "jail.yml");
                loadJailConfig();
                loadJailData();
                Bukkit.getScheduler().runTaskTimer(plugin, this::checkJailTimes, 20L, 20L * 10);
            }
        }

//        public void start() {
//            if (isSurvival()) {
//                Bukkit.getScheduler().runTaskTimer(plugin, this::checkJailTimes, 20L, 20L * 10);
//            }
//        }


        private boolean isSurvival() {
            return "survival".equalsIgnoreCase(plugin.getServerType());
        }

        private void loadJailConfig() {
            if (!isSurvival()) return;

            try {
                if (!plugin.getDataFolder().exists()) {
                    if (!plugin.getDataFolder().mkdirs()) {
                        plugin.getLogger().severe("Failed to create plugin directory!");
                        return;
                    }
                }

                jailFile = new File(plugin.getDataFolder(), "jail.yml");
                if (!jailFile.exists()) {
                    try {
                        if (!jailFile.createNewFile()) {
                            plugin.getLogger().severe("Failed to create jail.yml!");
                            return;
                        }
                        // Create default structure
                        jailConfig = new YamlConfiguration();
                        jailConfig.createSection("jail.location");
                        jailConfig.createSection("jail.release-location");
                        jailConfig.createSection("jail.players");
                        jailConfig.save(jailFile);
                    } catch (IOException e) {
                        plugin.getLogger().severe("Could not create jail.yml: " + e.getMessage());
                        plugin.getLogger().severe("Failed to load jail configuration - Error: " + e.getMessage());
                        plugin.getLogger().severe("Stack trace: " + e.getClass().getName());
                        return;
                    }
                }
                jailConfig = YamlConfiguration.loadConfiguration(jailFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load jail configuration: " + e.getMessage());
            }
        }

        private void saveJailConfig() {
            if (!isSurvival() || jailConfig == null) return;

            try {
                jailConfig.save(jailFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save jail.yml!");
                plugin.getLogger().severe(e.getMessage());
            }
        }

        public void loadJailData() {
            if (!isSurvival() || jailConfig == null) return;

            try {
                if (jailConfig.isConfigurationSection("jail.location")) {
                    ConfigurationSection locSection = jailConfig.getConfigurationSection("jail.location");
                    if (locSection != null && locSection.contains("world")) {
                        String worldName = locSection.getString("world");
                        if (worldName != null && Bukkit.getWorld(worldName) != null) {
                            double x = locSection.getDouble("x");
                            double y = locSection.getDouble("y");
                            double z = locSection.getDouble("z");
                            float yaw = (float) locSection.getDouble("yaw");
                            float pitch = (float) locSection.getDouble("pitch");
                            jailLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                        }
                    }
                }

                if (jailConfig.isConfigurationSection("jail.release-location")) {
                    ConfigurationSection locSection = jailConfig.getConfigurationSection("jail.release-location");
                    if (locSection != null && locSection.contains("world")) {
                        String worldName = locSection.getString("world");
                        if (worldName != null && Bukkit.getWorld(worldName) != null) {
                            double x = locSection.getDouble("x");
                            double y = locSection.getDouble("y");
                            double z = locSection.getDouble("z");
                            float yaw = (float) locSection.getDouble("yaw");
                            float pitch = (float) locSection.getDouble("pitch");
                            releaseLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                        }
                    }
                }

                if (releaseLocation == null) {
                    releaseLocation = Bukkit.getWorlds().getFirst().getSpawnLocation();
                }

                if (jailConfig.isConfigurationSection("jail.players")) {
                    ConfigurationSection playersSection = jailConfig.getConfigurationSection("jail.players");
                    if (playersSection != null) {
                        for (String uuidString : playersSection.getKeys(false)) {
                            try {
                                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);
                                if (playerSection == null) continue;

                                UUID uuid = UUID.fromString(uuidString);
                                long releaseTime = playerSection.getLong("release-time");
                                String reason = playerSection.getString("reason", "No reason provided");

                                jailedPlayers.put(uuid, new JailData(releaseTime, reason));

                                if (playerSection.isConfigurationSection("previous-location")) {
                                    loadPreviousLocation(uuid, playerSection.getConfigurationSection("previous-location"));
                                }
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid UUID in jail.yml: " + uuidString);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading jail data!");
                plugin.getLogger().severe("Failed to load jail data - Error: " + e.getMessage());
            }
        }

        private void loadPreviousLocation(UUID uuid, ConfigurationSection section) {
            if (section == null) return;

            String worldName = section.getString("world");
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                Location prevLoc = new Location(
                        Bukkit.getWorld(worldName),
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z"),
                        (float) section.getDouble("yaw"),
                        (float) section.getDouble("pitch")
                );
                previousLocations.put(uuid, prevLoc);
            }
        }

        public void saveJailData() {
            if (!isSurvival() || jailConfig == null) return;

            if (jailLocation != null) {
                ConfigurationSection locSection = jailConfig.createSection("jail.location");
                locSection.set("world", jailLocation.getWorld().getName());
                locSection.set("x", jailLocation.getX());
                locSection.set("y", jailLocation.getY());
                locSection.set("z", jailLocation.getZ());
                locSection.set("yaw", jailLocation.getYaw());
                locSection.set("pitch", jailLocation.getPitch());
            }

            if (releaseLocation != null) {
                ConfigurationSection locSection = jailConfig.createSection("jail.release-location");
                locSection.set("world", releaseLocation.getWorld().getName());
                locSection.set("x", releaseLocation.getX());
                locSection.set("y", releaseLocation.getY());
                locSection.set("z", releaseLocation.getZ());
                locSection.set("yaw", releaseLocation.getYaw());
                locSection.set("pitch", releaseLocation.getPitch());
            }

            jailConfig.set("jail.players", null);
            ConfigurationSection playersSection = jailConfig.createSection("jail.players");

            for (Map.Entry<UUID, JailData> entry : jailedPlayers.entrySet()) {
                ConfigurationSection playerSection = playersSection.createSection(entry.getKey().toString());
                playerSection.set("release-time", entry.getValue().releaseTime());
                playerSection.set("reason", entry.getValue().reason());

                Location prevLoc = previousLocations.get(entry.getKey());
                if (prevLoc != null) {
                    ConfigurationSection prevLocSection = playerSection.createSection("previous-location");
                    prevLocSection.set("world", prevLoc.getWorld().getName());
                    prevLocSection.set("x", prevLoc.getX());
                    prevLocSection.set("y", prevLoc.getY());
                    prevLocSection.set("z", prevLoc.getZ());
                    prevLocSection.set("yaw", prevLoc.getYaw());
                    prevLocSection.set("pitch", prevLoc.getPitch());
                }
            }

            saveJailConfig();
        }

        private void checkJailTimes() {
            long currentTime = System.currentTimeMillis();

            jailedPlayers.entrySet().removeIf(entry -> {
                if (currentTime >= entry.getValue().releaseTime()) {
                    UUID uuid = entry.getKey();
                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null && player.isOnline()) {
                        player.sendMessage("§aYou have been released from jail!");
                        teleportToReleaseLocation(player);
                    }

                    previousLocations.remove(uuid);
                    return true;
                }
                return false;
            });
        }

        private void teleportToReleaseLocation(@NotNull Player player) {
            UUID uuid = player.getUniqueId();
            Location prevLoc = previousLocations.get(uuid);
            if (prevLoc != null && prevLoc.getWorld() != null) {
                player.teleport(prevLoc);
            } else {
                player.teleport(Objects.requireNonNullElseGet(releaseLocation,
                    () -> Bukkit.getWorlds().getFirst().getSpawnLocation()));
            }
            previousLocations.remove(uuid);
        }

        public boolean isJailed(UUID uuid) {
            return jailedPlayers.containsKey(uuid);
        }

        public JailData getJailData(UUID uuid) {
            return jailedPlayers.get(uuid);
        }

        public boolean setJailLocation(Location location) {
            if (location == null) return false;
            this.jailLocation = location.clone();
            saveJailData();
            return true;
        }

        public Location getJailLocation() {
            return jailLocation;
        }

        public boolean jailPlayer(UUID uuid, long durationSeconds, String reason) {
            if (jailLocation == null) return false;

            Player player = Bukkit.getPlayer(uuid);
            long releaseTime = System.currentTimeMillis() + (durationSeconds * 1000);
            jailedPlayers.put(uuid, new JailData(releaseTime, reason));

            if (player != null && player.isOnline()) {
                previousLocations.put(uuid, player.getLocation().clone());
                player.teleport(jailLocation);
                player.sendMessage("§cYou have been jailed for: " + reason);
                player.sendMessage("§cTime: " + formatTime(durationSeconds));
            }

            saveJailData();
            return true;
        }

        public boolean unjailPlayer(UUID uuid) {
            if (!jailedPlayers.containsKey(uuid)) return false;

            jailedPlayers.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                player.sendMessage("§aYou have been released from jail!");
                teleportToReleaseLocation(player);
            }

            saveJailData();
            return true;
        }

        public String formatTime(long seconds) {
            if (seconds <= 0) return "0 seconds";

            long days = seconds / 86400;
            seconds %= 86400;
            long hours = seconds / 3600;
            seconds %= 3600;
            long minutes = seconds / 60;
            seconds %= 60;

            StringBuilder time = new StringBuilder();
            if (days > 0) time.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
            if (hours > 0) time.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
            if (minutes > 0) time.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
            if (seconds > 0) time.append(seconds).append(" second").append(seconds > 1 ? "s" : "");

            return time.toString().trim();
        }

        public record JailData(long releaseTime, String reason) {}
    }
package me.washeremc.Core.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.washeremc.Washere;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.team.ScoreboardTeam;
import net.megavex.scoreboardlibrary.api.team.TeamDisplay;
import net.megavex.scoreboardlibrary.api.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ScoreBoard {
    private final Washere plugin;
    private final LuckPerms luckPerms;
    private final ScoreboardLibrary scoreboardLibrary;
    public TeamManager teamManager;
    public final Map<UUID, Sidebar> playerSidebars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> sidebarUpdaters = new HashMap<>();

    public ScoreBoard(Washere plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();

        try {
            this.scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(plugin);
            this.teamManager = scoreboardLibrary.createTeamManager();
        } catch (Exception e) {
            plugin.getLogger().severe("[Scoreboard] Could not load ScoreboardLibrary");
            throw new RuntimeException(e);
        }
    }

    public void createSidebar(@NotNull Player player) {
        if (playerSidebars.containsKey(player.getUniqueId())) {
            removeSidebar(player);
        }

        Sidebar sidebar = scoreboardLibrary.createSidebar();
        sidebar.title(Component.text(ChatUtils.colorize(plugin.getConfig().getString("scoreboard.title"))));
        updateSidebarLines(player, sidebar);
        sidebar.addPlayer(player);
        playerSidebars.put(player.getUniqueId(), sidebar);
        scheduleSidebarUpdater(player);
    }

    private void scheduleSidebarUpdater(@NotNull Player player) {
        sidebarUpdaters.computeIfAbsent(player.getUniqueId(), k -> new BukkitRunnable() {
            @Override
            public void run() {
                Sidebar sidebar = playerSidebars.get(player.getUniqueId());
                if (sidebar == null || sidebar.closed()) {
                    cancel();
                    return;
                }
                updateSidebarLines(player, sidebar);
                setPlayerTeams(player);
            }
        }).runTaskTimer(plugin, 0L, 100L);
    }

    private void updateSidebarLines(@NotNull Player player, Sidebar sidebar) {
        int index = 0;
        for (String line : plugin.getConfig().getStringList("scoreboard.lines")) {
            sidebar.line(index++, Component.text(ChatUtils.colorize(PlaceholderAPI.setPlaceholders(player, line))));
        }
    }

    public void setPlayerTeams(@NotNull Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        String rank = user.getPrimaryGroup().toLowerCase();
        int priority = plugin.getConfig().getInt("scoreboard.teams." + rank + ".priority", 99);
        String prefix = plugin.getConfig().getString("scoreboard.teams." + rank + ".prefix", "&7");
        String rankColor = ChatUtils.extractColorCode(prefix);
        String teamName = String.format("rank_%02d_%s", priority, rank);

        ScoreboardTeam team = teamManager.createIfAbsent(teamName);
        TeamDisplay teamDisplay = team.defaultDisplay();

        teamDisplay.displayName(Component.text(rank.substring(0, 1).toUpperCase() + rank.substring(1)));
        teamDisplay.playerColor(ChatUtils.getNamedTextColor(rankColor));
        teamDisplay.addEntry(player.getName());

        teamManager.addPlayer(player);
        team.display(player, teamDisplay);

        boolean useRankColors = plugin.getConfig().getBoolean("tablist.use-rank-colors", true);

        if (useRankColors) {
            String displayName = ChatUtils.colorize(rankColor + player.getName());
            player.displayName(Component.text(displayName));
            player.playerListName(Component.text(displayName));
        } else {
            String format = plugin.getConfig().getString("tablist.player-list-name-format", "&e%player_name%");
            String formattedName = PlaceholderAPI.setPlaceholders(player, format);
            player.displayName(Component.text(ChatUtils.colorize(formattedName)));
            player.playerListName(Component.text(ChatUtils.colorize(formattedName)));
        }
    }

    public void toggleSidebar(@NotNull Player player) {
        if (playerSidebars.containsKey(player.getUniqueId())) {
            removeSidebar(player);
        }else {
            createSidebar(player);
        }
    }

    public void removePlayerTeams(@NotNull Player player) {
        if (teamManager == null) return;
        teamManager.removePlayer(player);
    }

    public void removeSidebar(@NotNull Player player) {
        Optional.ofNullable(playerSidebars.remove(player.getUniqueId())).ifPresent(sidebar -> sidebar.removePlayer(player));
        Optional.ofNullable(sidebarUpdaters.remove(player.getUniqueId())).ifPresent(BukkitRunnable::cancel);
    }

    public void resetSidebars() {
        Bukkit.getOnlinePlayers().forEach(this::removeSidebar);
    }
}
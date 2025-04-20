package me.washeremc.Registration;


import me.washeremc.Core.Profile.ProfileCommand;
import me.washeremc.Core.Settings.SettingsCommand;
import me.washeremc.Core.commands.*;
import me.washeremc.SERVERMODE.lobby.*;
import me.washeremc.SERVERMODE.survival.Home.HomeCommand;
import me.washeremc.SERVERMODE.survival.Home.SetHomeCommand;
import me.washeremc.SERVERMODE.survival.Jail.JailCommand;
import me.washeremc.SERVERMODE.survival.Jail.SetJailCommand;
import me.washeremc.SERVERMODE.survival.Jail.UnjailCommand;
import me.washeremc.SERVERMODE.survival.TPA.TpaCommand;
import me.washeremc.SERVERMODE.survival.TPA.TpacceptCommand;
import me.washeremc.SERVERMODE.survival.Warp.*;
import me.washeremc.SERVERMODE.survival.commands.BackpackCommand;
import me.washeremc.SERVERMODE.survival.commands.DonateCommand;
import me.washeremc.SERVERMODE.survival.commands.RecipeCommand;
import me.washeremc.Washere;
import org.bukkit.command.CommandExecutor;

import java.util.Objects;


public class CommandManager {

    private final Washere plugin;
    private final MsgCommand msgCommand;

    public CommandManager(Washere plugin) {
        this.plugin = plugin;
        this.msgCommand = new MsgCommand();
    }

    public void registerCommands() {
        registerCommand("sethome", new SetHomeCommand(plugin));
        registerCommand("home", new HomeCommand(plugin));
        registerCommand("backpack", new BackpackCommand(plugin));
        registerCommand("tpa", new TpaCommand(plugin));
        registerCommand("tpaccept", new TpacceptCommand(plugin));
        registerCommand("wreload", new ReloadCommand(plugin));
        registerCommand("donate", new DonateCommand(plugin));
        registerCommand("setwarp", new SetWarpCommand(plugin));
        registerCommand("delwarp", new DelWarpCommand(plugin));
        registerCommand("help", new HelpCommand());
        registerCommand("warp", new WarpCommand(plugin));
        registerCommand("warps", new WarpsCommand(plugin));
        registerCommand("msg", msgCommand);
        registerCommand("reply", new ReplyCommand(msgCommand));
        registerCommand("settings", new SettingsCommand());
        registerCommand("profile", new ProfileCommand());
        registerCommand("spawn", new SetSpawnCommand(plugin));
        registerCommand("fly", new FlyCommand(plugin));
        registerCommand("npc", new NPCCommand(plugin.getNpcUtils()));
        registerCommand("deletenpc", new NPCDeleteCommand(plugin.getNpcUtils()));
        registerCommand("listnpcs", new NPCListommand());
        registerCommand("recipe", new RecipeCommand(plugin));
        registerCommand("whois", new WhoisCommand());


        registerCommand("jail", new JailCommand(plugin.getJailManager(), plugin));
        registerCommand("unjail", new UnjailCommand(plugin.getJailManager(), plugin));
        registerCommand("setjail", new SetJailCommand(plugin.getJailManager(), plugin));

    }

    private void registerCommand(String name, CommandExecutor executor) {
        if (plugin.getCommand(name) != null) {
            Objects.requireNonNull(plugin.getCommand(name)).setExecutor(executor);
        } else {
            plugin.getLogger().warning("Command " + name + " is not defined in plugin.yml");
        }
    }
}
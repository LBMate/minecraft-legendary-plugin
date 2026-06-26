package dev.legendary;

import dev.legendary.items.WeaponRegistry;
import dev.legendary.listeners.*;
import dev.legendary.managers.FrostmourneManager;
import dev.legendary.managers.MjolnirManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LegendaryWeapons extends JavaPlugin {

    private static LegendaryWeapons instance;
    private WeaponRegistry weaponRegistry;
    private FrostmourneManager frostmourneManager;
    private MjolnirManager mjolnirManager;

    @Override
    public void onEnable() {
        instance = this;
        weaponRegistry = new WeaponRegistry();
        frostmourneManager = new FrostmourneManager(this);
        mjolnirManager = new MjolnirManager(this);

        getServer().getPluginManager().registerEvents(new ExcaliburListener(this), this);
        getServer().getPluginManager().registerEvents(new DarkinBladeListener(this), this);
        getServer().getPluginManager().registerEvents(new FrostmourneListener(this), this);
        getServer().getPluginManager().registerEvents(new MjolnirListener(this, mjolnirManager), this);

        getLogger().info("LegendaryWeapons enabled! 4 weapons loaded.");
    }

    @Override
    public void onDisable() {
        if (mjolnirManager != null) mjolnirManager.cleanup();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("giveweapon")) return false;
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /giveweapon <player> <darkin_blade|frostmourne|excalibur|mjolnir>");
            return true;
        }
        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }
        var item = weaponRegistry.getWeapon(args[1].toLowerCase());
        if (item == null) {
            sender.sendMessage("§cUnknown weapon. Options: darkin_blade, frostmourne, excalibur, mjolnir");
            return true;
        }
        target.getInventory().addItem(item);
        sender.sendMessage("§aGave §6" + args[1] + "§a to §e" + target.getName());
        return true;
    }

    public static LegendaryWeapons getInstance() { return instance; }
    public WeaponRegistry getWeaponRegistry() { return weaponRegistry; }
    public MjolnirManager getMjolnirManager() { return mjolnirManager; }
}

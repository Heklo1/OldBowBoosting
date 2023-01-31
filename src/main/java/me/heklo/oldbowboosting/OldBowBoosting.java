package me.heklo.oldbowboosting;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class OldBowBoosting extends JavaPlugin
{
    private static OldBowBoosting instance;
    public static OldBowBoosting getInstance()
    {
        return instance;
    }
    @Override
    public void onEnable()
    {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new ObbEventListener(this), this);
        PluginCommand command = getCommand("obb-reload");
        if(command != null)
        {
            command.setExecutor(new ObbCommandExecutor(this));
        }
        else
        {
            Bukkit.getLogger().warning("Unable to register command 'obb-reload'");
        }
    }

    @Override
    public void onLoad() {
        if(Bukkit.getPluginManager().getPlugin("WorldGuard") != null)
        {
            Bukkit.getLogger().info("[OldBowBoosting] WorldGuard Detected - enabling custom flag 'bowboost'.");
            WorldGuardHelper.registerWorldGuardFlags();
        }
        else
        {
            Bukkit.getLogger().warning("[OldBowBoosting] WorldGuard Missing - players can bowboost everywhere.");
        }
    }
}


package me.heklo.oldbowboosting;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ObbCommandExecutor implements CommandExecutor
{
    private final OldBowBoosting plugin;
    public ObbCommandExecutor(OldBowBoosting plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "Reloading OldBowBoosting...");
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.AQUA + "Reload Complete!");
        return true;
    }
}

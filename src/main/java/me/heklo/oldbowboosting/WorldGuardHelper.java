package me.heklo.oldbowboosting;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WorldGuardHelper
{
    public static StateFlag BOWBOOST_FLAG;
    public static final String BOWBOOST_FLAG_STRING = "bowboost";

    public static void registerWorldGuardFlags()
    {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try
        {
            StateFlag flag = new StateFlag(BOWBOOST_FLAG_STRING, true);
            registry.register(flag);
            BOWBOOST_FLAG = flag;
            Bukkit.getLogger().info("[OldBowBoosting] Successfully registered wg flag '" + BOWBOOST_FLAG_STRING + "'");
        }
        catch (FlagConflictException e) {
            Flag<?> existing = registry.get(BOWBOOST_FLAG_STRING);
            Bukkit.getLogger().warning("[OldBowBoosting] Error: Unable to register '" + BOWBOOST_FLAG_STRING + "'");
            if (existing instanceof StateFlag)
            {
                BOWBOOST_FLAG = (StateFlag) existing;
            }
        }
    }

    public static boolean canBowBoost(Player player)
    {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet regionSet = query.getApplicableRegions(localPlayer.getLocation());
        return regionSet.testState(localPlayer, BOWBOOST_FLAG);
    }
}

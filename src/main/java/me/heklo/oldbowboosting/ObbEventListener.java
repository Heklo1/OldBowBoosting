package me.heklo.oldbowboosting;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Random;

public class ObbEventListener implements Listener
{
    private final OldBowBoosting plugin;
    public ObbEventListener(OldBowBoosting plugin)
    {
        this.plugin = plugin;
    }

    public void checkBoost(ArrowInfo arrowInfo)
    {
        if(arrowInfo.isValid())
        {
            // If arrow fully escaped the hitbox, set true.
            if(arrowInfo.outsideLeaveHitbox())
            {
                arrowInfo.setLeftHitbox(true);
            }

            // If arrow re-entered the hitbox after leaving it, boost.
            if(arrowInfo.canBoost())
            {
                fakeShot(arrowInfo);
                return;
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkBoost(arrowInfo), 1L);
        }

    }

    public static void fakeShot(ArrowInfo arrowInfo)
    {
        Player player = arrowInfo.getShooter();
        Arrow arrow = arrowInfo.getArrow();
        if(!arrowInfo.isValid())
                return;

        // Apply velocity
        player.setVelocity(arrowInfo.getPunchVelocity());

        // Apply damage
        damagePlayer(player, arrow);

        // Apply armor damage
        damageArmor(player);

        // Apply effects
        if(arrow.hasCustomEffects())
        {
            for (PotionEffect effect: arrow.getCustomEffects())
            {
                player.addPotionEffect(effect);
            }
        }

        // Apply fire
        if(OldBowBoosting.getInstance().getConfig().getBoolean("burn-booster"))
        {
            if(arrow.getFireTicks() > 0)
            {
                int flameDuration = 5 * 20;
                if (flameDuration > player.getFireTicks())
                {
                    player.setFireTicks(flameDuration);
                }
            }
        }

        // Destroy the arrow entity
        arrow.remove();
    }

    public static void damageArmor(Player player)
    {
        Random random = new Random();
        for(ItemStack armor : player.getInventory().getArmorContents())
        {
            if(armor != null)
            {
                int unbreaking = armor.getEnchantmentLevel(Enchantment.DURABILITY);
                float chanceOfDamaging = 1.0F / (unbreaking + 1.0F);
                if(random.nextFloat() < chanceOfDamaging)
                {
                    armor.setDurability((short) (armor.getDurability() - 1));
                }
            }
        }
    }

    public static void damagePlayer(Player player, Arrow arrow)
    {
        player.damage(arrow.getDamage());
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event)
    {
        Entity shooter = event.getEntity();
        Entity projectile = event.getProjectile();
        if(!(shooter instanceof Player && projectile instanceof Arrow))
            return;

        Arrow arrow = (Arrow) event.getProjectile();
        Vector shooterLook = shooter.getLocation().getDirection().clone();
        double arrowSpeed = arrow.getVelocity().length();
        if(plugin.getConfig().getBoolean("remove-arrow-randomization"))
            arrow.setVelocity(shooterLook.multiply(arrowSpeed));

        Bukkit.getScheduler().runTaskLater(plugin, () -> checkBoost(new ArrowInfo(plugin, arrow)), 1L);
    }


}

class ArrowInfo
{
    private final OldBowBoosting plugin;
    private final Arrow arrow;
    private final Player shooter;
    private boolean leftHitbox;

    public ArrowInfo(OldBowBoosting plugin, Arrow arrow)
    {
        this.plugin = plugin;
        this.arrow = arrow;
        this.shooter = (Player) arrow.getShooter();
        this.leftHitbox = false;
    }

    public Arrow getArrow() { return this.arrow; }
    public Player getShooter() { return this.shooter; }
    public boolean getLeftHitbox() { return this.leftHitbox; }

    public void setLeftHitbox(boolean leftHitbox) { this.leftHitbox = leftHitbox; }

    public Vector getPunchVelocity()
    {
        double horizontalVelocity = plugin.getConfig().getDouble("velocity-horizontal");
        double verticalVelocity = plugin.getConfig().getDouble("velocity-vertical");

        // Account for punch
        int punch = arrow.getKnockbackStrength();
        horizontalVelocity *= (punch+1);

        // Velocity constants
        Vector horizontalVelocityScale = new Vector(horizontalVelocity, 0, horizontalVelocity);
        Vector verticalVelocityScale = new Vector(0, verticalVelocity, 0);

        Vector arrowDirection = arrow.getVelocity().clone().normalize();
        Vector newVelocity = arrowDirection.multiply(horizontalVelocityScale).add(verticalVelocityScale);
        newVelocity = newVelocity.add(shooter.getVelocity());
        return newVelocity;
    }

    public boolean canBoost()
    {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard") && !WorldGuardHelper.canBowBoost(shooter))
            return false;
        if (shooter.getGameMode() == GameMode.CREATIVE || shooter.getGameMode() == GameMode.SPECTATOR)
            return false;
        if (shooter.getNoDamageTicks() > 0)
            return false;
        if (shooter.getVelocity().length() == 0)
            return false;
        if (!overlapsEnterHitbox())
            return false;
        if (!getLeftHitbox())
            return false;
        if (!isAliveLongEnough())
            return false;

        return true;
    }

    public boolean outsideLeaveHitbox()
    {

        double leaveHitboxReduction = plugin.getConfig().getDouble("leave-hitbox-reduction");

        BoundingBox leaveHitBox = shooter.getBoundingBox().clone().expand(0.1 - leaveHitboxReduction);
        BoundingBox arrowBox = arrow.getBoundingBox();
        return !arrowBox.overlaps(leaveHitBox);
    }

    public boolean overlapsEnterHitbox()
    {
        double enterHitboxExpansion = plugin.getConfig().getDouble("enter-hitbox-expansion");
        BoundingBox enterHitBox = shooter.getBoundingBox().clone().expand(0.1 + enterHitboxExpansion);
        BoundingBox arrowBox = arrow.getBoundingBox();
        return arrowBox.overlaps(enterHitBox);
    }

    public boolean isAliveLongEnough()
    {
        double minTicks = plugin.getConfig().getInt("min-life-ticks");
        double speed = arrow.getVelocity().length();
        double maxSpeed = 3.0D;
        double scale = (1 - speed / maxSpeed);
        minTicks *= scale;
        return arrow.getTicksLived() >= minTicks;
    }

    public boolean isValid()
    {
        return shooter != null &&
                arrow != null &&
                arrow.isValid() &&
                arrow.getTicksLived() < 15 &&
                !arrow.isInBlock() &&
                shooter.isValid();
    }
}



package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityZaizan;
import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class HonKaiZaizan extends SpecialAttackBase {
    private static final float BASE_DAMAGE = 20.0F;
    private static final double AOE_RADIUS = 5.8D;
    private static final double AOE_FRONT_EXTENSION = 1.3D;
    private static final int VISUAL_LIFETIME = 32;
    private static final double LUNGE_SPEED = 0.92D;

    @Override
    public String toString() {
        return "zaizan";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer player) {
        Vec3d look = player.getLookVec();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double lookX = horizontal > 1.0E-4D ? look.x / horizontal : 0.0D;
        double lookZ = horizontal > 1.0E-4D ? look.z / horizontal : 0.0D;

        player.motionX = lookX * LUNGE_SPEED;
        player.motionZ = lookZ * LUNGE_SPEED;
        player.velocityChanged = true;

        if (player.world.isRemote) return;

        EntityZaizan.spawn(player.world, player, 1.55F, 1.16F, VISUAL_LIFETIME, 1.12F);
        player.world.playSound(null, player.posX + lookX * 1.55D, player.posY + 1.16D,
                player.posZ + lookZ * 1.55D, SoundEvents.BLOCK_END_PORTAL_SPAWN,
                SoundCategory.PLAYERS, 1.06F, 1.52F);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.92F, 1.34F);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.52F, 1.72F);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_FIREWORK_BLAST, SoundCategory.PLAYERS, 0.72F, 0.64F);

        AxisAlignedBB area = player.getEntityBoundingBox()
                .grow(AOE_RADIUS, 1.0D, AOE_RADIUS)
                .union(player.getEntityBoundingBox()
                        .offset(lookX * AOE_FRONT_EXTENSION, 0.0D, lookZ * AOE_FRONT_EXTENSION)
                        .grow(AOE_RADIUS, 1.0D, AOE_RADIUS))
                .offset(player.motionX, player.motionY, player.motionZ);
        List<Entity> entities = player.world.getEntitiesInAABBexcluding(player, area,
                input -> TargetingUtil.canSelectForDamage(player, input));
        NBTTagCompound tag = itemStack.getTagCompound();
        float bladeAttack = tag != null ? ItemSlashBlade.BaseAttackModifier.get(tag) : 4.0F;
        float extraDamage = MathFunc.amplifierCalc(bladeAttack, 20.0F);
        for (Entity entity : TargetingUtil.getDistinctDamageTargets(entities)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(entity);
            if (target == null) continue;
            if (target instanceof EntityPlayer) {
                target.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 100, 2));
            } else {
                player.onCriticalHit(entity);
                target.hurtResistantTime = 0;
                entity.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageBypassesArmor(),
                        BASE_DAMAGE + extraDamage);
                target.hurtResistantTime = 0;
                itemStack.hitEntity(target, player);
            }
        }
        WorldEvent.addTickDelayTask(12,
                () -> player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 140, 2)));
    }
}

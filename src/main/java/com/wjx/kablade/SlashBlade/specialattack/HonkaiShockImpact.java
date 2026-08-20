package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import com.wjx.kablade.Entity.EntityShockImpact;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class HonkaiShockImpact extends SpecialAttackBase {
    private static final float BASE_DAMAGE = 22.0F;
    private static final double AOE_RADIUS = 6.0D;
    private static final double AOE_VERTICAL = 3.2D;
    private static final double LUNGE_SPEED = 1.38D;

    @Override
    public String toString() {
        return "shock_impact";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Vec3d look = entityPlayer.getLookVec();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double lookX = horizontal > 1.0E-4D ? look.x / horizontal : 0.0D;
        double lookZ = horizontal > 1.0E-4D ? look.z / horizontal : 0.0D;

        entityPlayer.motionX = lookX * LUNGE_SPEED;
        entityPlayer.motionZ = lookZ * LUNGE_SPEED;
        entityPlayer.velocityChanged = true;

        if (!entityPlayer.world.isRemote) {
            EntityShockImpact.spawn(entityPlayer.world, entityPlayer, 0.62F, 1.05F, 26, 1.12F);
            entityPlayer.world.playSound(null, entityPlayer.posX, entityPlayer.posY + 1.0D, entityPlayer.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.16F, 1.88F);
            entityPlayer.world.playSound(null, entityPlayer.posX, entityPlayer.posY + 1.0D, entityPlayer.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.05F, 1.55F);
            entityPlayer.world.playSound(null, entityPlayer.posX, entityPlayer.posY + 1.0D, entityPlayer.posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.82F, 1.72F);
        }

        AxisAlignedBB ax = entityPlayer.getEntityBoundingBox()
                .grow(AOE_RADIUS, AOE_VERTICAL, AOE_RADIUS)
                .offset(entityPlayer.motionX * 0.5D, entityPlayer.motionY * 0.5D,
                        entityPlayer.motionZ * 0.5D);
        List<Entity> entities = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer, ax,
                input -> TargetingUtil.canSelectForDamage(entityPlayer, input));
        float extraDamage = MathFunc.amplifierCalc(
                ItemSlashBlade.BaseAttackModifier.get(itemStack.getTagCompound()), 10.0F);
        for (Entity entity : TargetingUtil.getDistinctDamageTargets(entities)){
            EntityLivingBase effectTarget = TargetingUtil.getSelectionTarget(entity);
            if (effectTarget != null){
                if (!entityPlayer.world.isRemote && !(effectTarget instanceof EntityPlayer)){
                    entityPlayer.onCriticalHit(entity);
                    effectTarget.hurtResistantTime = 0;
                    entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setDamageBypassesArmor(),
                            BASE_DAMAGE + extraDamage);
                    effectTarget.hurtResistantTime = 0;
                    itemStack.hitEntity(effectTarget,entityPlayer);
                }
            }
        }
        if (!entityPlayer.world.isRemote) {
            entityPlayer.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 100, 5));
        }
    }
}

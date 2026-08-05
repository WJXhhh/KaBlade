package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class SaCutMetal extends SpecialAttackBase {

    @Override
    public String toString() {
        return "cut_metal";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,60,2));
        AxisAlignedBB bb = entityPlayer.getEntityBoundingBox().grow(8,4,8).offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> l = world.getEntitiesInAABBexcluding(entityPlayer, bb,
                input -> TargetingUtil.canSelectForDamage(entityPlayer, input));
        float extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),8f);
        for (Entity e: TargetingUtil.getDistinctDamageTargets(l)){
            EntityLivingBase en = TargetingUtil.getSelectionTarget(e);
            if (en != null){
                entityPlayer.onCriticalHit(en);
                en.hurtResistantTime = 0;
                e.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setDamageBypassesArmor(),8 + extraDamage);
                en.hurtResistantTime = 0;
                if (en instanceof EntityLivingBase)
                    itemStack.hitEntity(en,entityPlayer);
                double armor;
                armor =en.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue();
                en.hurtResistantTime = 0;
                e.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setDamageBypassesArmor(), (float) (armor*0.5d));
                en.hurtResistantTime = 0;
                if (en instanceof EntityLivingBase)
                    itemStack.hitEntity(en,entityPlayer);
                double x = en.posX;
                double y = en.posY;
                double z = en.posZ;
                for (int i = 0; i < 20; ++i)
                {
                    Random r1 =new Random();
                    Random r2 =new Random(r1.nextLong());
                    int state1;
                    int state2;
                    if (r1.nextBoolean()){
                        state1 = 1;
                    }
                    else state1 = -1;
                    if (r2.nextBoolean()){
                        state2 = 1;
                    }
                    else state2 = -1;
                    world.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR, x + (world.rand.nextDouble() * 2 * state1), y + world.rand.nextDouble() * (double)entityPlayer.height, z + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D); }
            }
        }
    }
}

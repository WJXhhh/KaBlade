package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.Spear;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class HonkaiShockImpact extends SpecialAttackBase {
    @Override
    public String toString() {
        return "shock_impact";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Spear spear = new Spear();
        spear.doSpacialAttack(itemStack,entityPlayer);
        AxisAlignedBB ax = entityPlayer.getEntityBoundingBox();
        ax= ax.grow(3,1,3);
        ax=ax.offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> entities = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer,ax, input -> input != entityPlayer && input instanceof EntityLivingBase);
        float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound())),10f);
        for (Entity entity : entities){
            if (entity != null){
                if (!entityPlayer.world.isRemote && !(entity instanceof EntityPlayer)){
                    ((ItemSlashBlade)itemStack.getItem()).attackTargetEntity(itemStack, entity, entityPlayer, true);
                    entityPlayer.onCriticalHit(entity);
                    entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),22 + extraDamage);
                    entityPlayer.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,100,5));
                }
            }
        }
    }
}

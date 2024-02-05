package com.wjx.kablade.SlashBlade.specialattack;

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

public class HonKaiZaizan extends SpecialAttackBase {
    @Override
    public String toString() {
        return "zaizan";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Spear spear = new Spear();
        spear.doSpacialAttack(itemStack,entityPlayer);
        AxisAlignedBB ax = entityPlayer.getEntityBoundingBox();
       ax= ax.grow(5,1,5);
        ax=ax.offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> entities = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer,ax,input -> input instanceof EntityLivingBase);
        for (Entity entity : entities){
            if (entity != null){
                if(!(entity instanceof EntityPlayer)){
                    entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),20);
                }
                else{
                    ((EntityLivingBase)entity).addPotionEffect(new PotionEffect(MobEffects.STRENGTH,100,2));
                }
            }
        }
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,100,2));
    }
}

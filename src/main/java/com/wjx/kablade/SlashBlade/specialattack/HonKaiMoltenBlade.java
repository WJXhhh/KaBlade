package com.wjx.kablade.SlashBlade.specialattack;

import mods.flammpfeil.slashblade.specialattack.Spear;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class HonKaiMoltenBlade extends SpecialAttackBase {
    @Override
    public String toString() {
        return "molten_blade";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Spear spear = new Spear();
        spear.doSpacialAttack(itemStack,entityPlayer);
        AxisAlignedBB ax = entityPlayer.getEntityBoundingBox();
       ax= ax.grow(3,1,3);
        ax=ax.offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> entities = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer,ax,input -> input != entityPlayer && input instanceof EntityLivingBase);
        for (Entity entity : entities){
            if (entity != null){
                entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),22);
                entity.setFire(5);
            }
        }
    }
}

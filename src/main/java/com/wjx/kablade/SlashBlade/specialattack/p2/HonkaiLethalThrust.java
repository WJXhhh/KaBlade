package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityTuna;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class HonkaiLethalThrust extends SpecialAttackBase {
    @Override
    public String toString() {
        return "lethal_thrust";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Entity entity = SATool.getEntityToWatch(entityPlayer);
        if (entity instanceof EntityLivingBase){
            EntityTuna tuna = new EntityTuna(entityPlayer.world);
            tuna.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
            tuna.blade = itemStack;
            tuna.owner = entityPlayer;

            entityPlayer.world.spawnEntity(tuna);
        }
    }
}

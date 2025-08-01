package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityRainUmbrella;
import com.wjx.kablade.Entity.EntityTuna;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class HonkaiYHSY extends SpecialAttackBase {
    @Override
    public String toString() {
        return "lethal_thrust";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Entity entity = SATool.getEntityToWatch(entityPlayer);
        if (entity instanceof EntityLivingBase){
            EntityRainUmbrella tuna = new EntityRainUmbrella(entityPlayer.world);
            tuna.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
            tuna.blade = itemStack;
            tuna.owner = entityPlayer;

            entityPlayer.world.spawnEntity(tuna);
        }else if(entity== null){
            EntityRainUmbrella tuna = new EntityRainUmbrella(entityPlayer.world);
            tuna.setPositionAndUpdate(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ);
            tuna.blade = itemStack;
            tuna.owner = entityPlayer;

            entityPlayer.world.spawnEntity(tuna);
        }
    }
}

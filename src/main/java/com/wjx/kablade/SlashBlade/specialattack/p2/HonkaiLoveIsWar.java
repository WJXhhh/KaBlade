package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityRainUmbrella;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class HonkaiLoveIsWar extends SpecialAttackBase {
    @Override
    public String toString() {
        return "love_is_war";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Entity entity = SATool.getEntityToWatch(entityPlayer);
        if (entity instanceof EntityLivingBase){
            EntityRainUmbrella rainUmbrella = new EntityRainUmbrella(entityPlayer.world);
            rainUmbrella.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
            rainUmbrella.blade = itemStack;
            rainUmbrella.owner = entityPlayer;

            entityPlayer.world.spawnEntity(rainUmbrella);
        }else if(entity== null){
            EntityRainUmbrella rainUmbrella = new EntityRainUmbrella(entityPlayer.world);
            rainUmbrella.setPositionAndUpdate(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ);
            rainUmbrella.blade = itemStack;
            rainUmbrella.owner = entityPlayer;

            entityPlayer.world.spawnEntity(rainUmbrella);
        }
    }
}

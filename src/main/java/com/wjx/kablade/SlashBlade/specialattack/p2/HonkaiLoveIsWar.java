package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityRainUmbrella;
import com.wjx.kablade.Entity.EntityTuna;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public class HonkaiLoveIsWar extends SpecialAttackBase {
    @Override
    public String toString() {
        return "love_is_war";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        if(!entityPlayer.world.isRemote)
        {
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(itemStack.getTagCompound());
            if (entityId != 0) {
                Entity tmp = entityPlayer.world.getEntityByID(entityId);
                if (tmp != null && tmp.getDistance(entityPlayer) < 100.0F && tmp instanceof EntityLivingBase) {
                    target = tmp;
                }
            }
            Entity entity = (target==null?SATool.getEntityToWatch(entityPlayer):target);
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemSlashBlade && entityPlayer.getHeldItemMainhand().hasTagCompound()){
                ItemSlashBlade.setComboSequence(Objects.requireNonNull(entityPlayer.getHeldItemMainhand().getTagCompound()), ItemSlashBlade.ComboSequence.SlashDim);
            }
            if (entity instanceof EntityLivingBase) {
                EntityRainUmbrella tuna = new EntityRainUmbrella(entityPlayer.world);
                tuna.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
                tuna.blade = itemStack;
                tuna.owner = entityPlayer;


                entityPlayer.world.spawnEntity(tuna);
            }else if(entity== null){
                EntityRainUmbrella tuna = new EntityRainUmbrella(entityPlayer.world);
                tuna.setPositionAndUpdate(entityPlayer.posX+entityPlayer.motionX, entityPlayer.posY+entityPlayer.motionY, entityPlayer.posZ+entityPlayer.motionZ);
                tuna.blade = itemStack;
                tuna.owner = entityPlayer;

                entityPlayer.world.spawnEntity(tuna);
            }
        }
    }
}

package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityConfinementForceField;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public class HonkaiInductionCollapse extends SpecialAttackBase {
    @Override
    public String toString() {
        return "induction_collapse";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        if(!entityPlayer.world.isRemote)
        {
            Entity entity = TargetingUtil.resolveTarget(entityPlayer, itemStack, 30.0D, 8.0D, 8.0D);
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemSlashBlade && entityPlayer.getHeldItemMainhand().hasTagCompound()){
                ItemSlashBlade.setComboSequence(Objects.requireNonNull(entityPlayer.getHeldItemMainhand().getTagCompound()), ItemSlashBlade.ComboSequence.SlashDim);
            }
            if (entity instanceof EntityLivingBase) {
                EntityConfinementForceField tuna = new EntityConfinementForceField(entityPlayer.world);
                tuna.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
                tuna.blade = itemStack;
                tuna.owner = entityPlayer;


                entityPlayer.world.spawnEntity(tuna);
            }else if(entity== null){
                EntityConfinementForceField tuna = new EntityConfinementForceField(entityPlayer.world);
                tuna.setPositionAndUpdate(entityPlayer.posX+entityPlayer.motionX, entityPlayer.posY+entityPlayer.motionY, entityPlayer.posZ+entityPlayer.motionZ);
                tuna.blade = itemStack;
                tuna.owner = entityPlayer;

                entityPlayer.world.spawnEntity(tuna);
            }
        }
    }
}

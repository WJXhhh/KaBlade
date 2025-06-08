package com.wjx.kablade.ExSA.special_attack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public class OverSlash extends SpecialAttackBase {
    @Override
    public String toString() {
        return "overslash";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        for(int i = 0;i<20;i++){
            final double d0 = entityPlayer.getRNG().nextGaussian() * 0.02;
            final double d2 = entityPlayer.getRNG().nextGaussian() * 0.02;
            final double d3 = entityPlayer.getRNG().nextGaussian() * 0.02;
            final double d4 = 10.0;
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, entityPlayer.posX + (double)(entityPlayer.getRNG().nextFloat() * entityPlayer.width * 2.0f) - (double)entityPlayer.width - d0 * d4, entityPlayer.posY, entityPlayer.posZ + (double)(entityPlayer.getRNG().nextFloat() * entityPlayer.width * 2.0f) - (double)entityPlayer.width - d2 * d4, d0, d2, d3);

        }
    }
}

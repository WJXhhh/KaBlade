package com.wjx.kablade.SlashBlade.specialattack;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class HonKaiChopWillow extends SpecialAttackBase {
    @Override
    public String toString() {
        return "chop_willow";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;

            entityPlayer.motionY = 1.1;
            entityPlayer.getEntityData().setBoolean("to_chop_willow",true);
            entityPlayer.getEntityData().setInteger("chop_willow_retry_count",0);
    }
}

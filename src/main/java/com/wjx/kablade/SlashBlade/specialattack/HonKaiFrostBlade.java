package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.SummonBladeOfFrostBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class HonKaiFrostBlade extends SpecialAttackBase {
    @Override
    public String toString() {
        return "frost_blade";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        for (int i =0;i<6;i++){
            SummonBladeOfFrostBlade entity = new SummonBladeOfFrostBlade(entityPlayer.world,entityPlayer,3);
            int in = world.rand.nextBoolean() ? 1 : 0;
            int in2 = world.rand.nextBoolean() ? 1 : 0;
            entity.setPosition(entityPlayer.posX + (world.rand.nextDouble() * in),entityPlayer.getEyeHeight() + entityPlayer.posY + (world.rand.nextDouble()/2),entityPlayer.posZ + (world.rand.nextDouble() * in2));
            assert itemStack.hasTagCompound();
            entity.setColor(itemStack.getTagCompound().getInteger("SummonedSwordColor"));
            world.spawnEntity(entity);
        }
    }
}

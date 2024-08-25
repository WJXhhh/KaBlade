package com.wjx.kablade.AllWeapon.blade.specialattack;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.IJustSpecialAttack;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSpectralArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Random;

public class AL_RandomSA extends SpecialAttackBase{
    @Override
    public String toString() {
        return "suiji" ;
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        if(!world.isRemote){
            Random random = new Random();
            ItemSlashBlade slashBlade = (ItemSlashBlade) itemStack.getItem();
            Integer[] Key = ItemSlashBlade.specialAttacks.keySet().toArray(new Integer[0]);
            Integer randomKey = Key[random.nextInt(Key.length)];
            SpecialAttackBase sa = ItemSlashBlade.specialAttacks.get(randomKey);
            if(sa instanceof IJustSpecialAttack){
                ((IJustSpecialAttack) sa).doJustSpacialAttack(itemStack,entityPlayer);
            }
            else{
                sa.doSpacialAttack(itemStack,entityPlayer);
            }
        }
    }
}

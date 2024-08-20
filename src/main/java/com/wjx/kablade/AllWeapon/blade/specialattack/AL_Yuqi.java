package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class AL_Yuqi extends SpecialAttackBase {
    @Override
    public String toString() {
        return "yuqi";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        if(!world.isRemote){
            NBTTagCompound tag = itemStack.getTagCompound();
            if(tag!=null){
                float magicDamage = MathFunc.amplifierCalc(ItemSlashBlade.AttackAmplifier.get(tag),10f)+ItemSlashBlade.BaseAttackModifier.get(tag);
                EntityDriveAdd entityDriveAdd = new EntityDriveAdd(world, entityPlayer, magicDamage, false, 0.0f - ItemSlashBlade.ComboSequence.Battou.swingDirection);
                entityDriveAdd.getDataManager().set(EntityDriveAdd.COLOR_R,1f);
                entityDriveAdd.getDataManager().set(EntityDriveAdd.COLOR_G,1f);
                entityDriveAdd.getDataManager().set(EntityDriveAdd.COLOR_B,1f);
                entityDriveAdd.setInitialSpeed(0.008f);
                entityDriveAdd.setLifeTime(80);
                //entityDriveAdd.getDataManager().set(EntityDriveAdd.PARTICLE_STYLE,"item_snowball");
                world.spawnEntity(entityDriveAdd);
                ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.Battou);
            }

        }

    }
}

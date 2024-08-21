package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Random;

public class AL_Xingbao extends SpecialAttackBase {
    @Override
    public String toString() {
        return "xingbao";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);

        if(!world.isRemote){
            float baseModif = ItemSlashBlade.BaseAttackModifier.get(tag)/5;
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
            float magicDamage = baseModif;
            magicDamage+= MathFunc.amplifierCalc(ItemSlashBlade.AttackAmplifier.get(tag),0.5f);
            Random rand = new Random();
            for (int i = 0; i < 21; ++i) {
                float a = rand.nextInt(360);
                EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, magicDamage, false, a - ItemSlashBlade.ComboSequence.Battou.swingDirection);

                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_R,1f);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_G,0f);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_B,0f);
                entityDrive.setChangetime(20);
                entityDrive.setNextspeed(1.5f);
                entityDrive.setLocationAndAngles(entityPlayer.posX + (double)rand.nextInt(25) * 0.1, entityPlayer.posY + (double)rand.nextInt(13) + (double)entityPlayer.getEyeHeight() / 2.0, entityPlayer.posZ + (double)rand.nextInt(25) * 0.1, entityPlayer.rotationYaw, 0.0f);
                entityDrive.getDataManager().set(EntityDriveAdd.SCALE_X,2f);
                entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Y,2f);
                entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Z,2f);
                entityDrive.setInitialSpeed(5.0E-4f * (float)i + 0.001f);
                entityDrive.getDataManager().set(EntityDriveAdd.PL_PARTICAL,false);
                entityDrive.setLifeTime(90);
                if (entityDrive == null) continue;
                world.spawnEntity(entityDrive);
            }
        }
    }
}

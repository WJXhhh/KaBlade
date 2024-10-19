package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntitySummonedButterfly;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Random;

public class AL_HuanyingdieS extends SpecialAttackBase {
    @Override
    public String toString() {
        return "huanyingdies";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        Entity target = null;
        EntitySummonedButterfly entityDrive;
        float magicDamage;
        int level;
        float baseModif;
        ItemSlashBlade blade;
        float d;
        float e;
        float b;
        float a;
        int i;
        Random rand;
        Entity tmp;
        int entityId = ItemSlashBlade.TargetEntityId.get(tag);

        if(entityId == 0)
            target = SATool.getEntityToWatch(entityPlayer);
        else
            target = world.getEntityByID(entityId);

        if(target==null){
            rand = new Random();
            for (i = 0; i < 12; ++i) {
                a = rand.nextInt(100);
                b = rand.nextInt(40);
                e = rand.nextInt(100);
                d = rand.nextInt(30);
                blade = (ItemSlashBlade)((Object)itemStack.getItem());
                baseModif = ItemSlashBlade.BaseAttackModifier.get(tag);

                magicDamage = (baseModif+ MathFunc.amplifierCalc(baseModif,4f))/ 10.0f;
                entityDrive = new EntitySummonedButterfly(world, entityPlayer, magicDamage += ItemSlashBlade.AttackAmplifier.get(tag).floatValue(), 0.0f);
                entityDrive.setLocationAndAngles(entityDrive.posX + (double)((50.0f - a) / 10.0f), entityDrive.posY + (double)((20.0f - b) / 10.0f) + 0.75 + (double)entityPlayer.getEyeHeight() / 2.0, entityPlayer.posZ + (double)((50.0f - e) / 10.0f), entityPlayer.rotationYaw + (float)(i * 30), 0.0f);
                entityDrive.setDriveVector((float)(1.0E-4 * (double)a * (double)i + 1.0E-4));
                entityDrive.setInterval((i + 1));
                entityDrive.setLifeTime(100);
                entityDrive.setColor(0xFFFFFF);
                world.spawnEntity(entityDrive);

            }
        }

        if (target != null) {
            
            rand = new Random();
            for (i = 0; i < 50; ++i) {
                a = rand.nextInt(100);
                b = rand.nextInt(40);
                e = rand.nextInt(100);
                d = rand.nextInt(30);
                blade = (ItemSlashBlade)((Object)itemStack.getItem());
                baseModif = blade.getBaseAttackModifiers(tag);
                level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);


                magicDamage = (baseModif+ MathFunc.amplifierCalc(baseModif,2f))/ 50.0f;
                entityDrive = new EntitySummonedButterfly(world, entityPlayer, magicDamage += ItemSlashBlade.AttackAmplifier.get(tag).floatValue() * ((float)level / 50.0f), 0.0f);
                entityDrive.setLocationAndAngles(target.posX + (double)((50.0f - a) / 3.0f), target.posY + (double)((20.0f - b) / 10.0f) + 1.0 + (double)entityPlayer.getEyeHeight() / 2.0, target.posZ + (double)((50.0f - e) / 3.0f), target.rotationYaw + (float)(i * 7), 0.0f);
                entityDrive.setDriveVector((float)(1.0E-4 * (double)a * (double)i + 1.0E-4));
                entityDrive.setInterval((i + 1) * 1);
                entityDrive.setLifeTime(100);
                entityDrive.setColor(0xFFFFFF);
                world.spawnEntity(entityDrive);

            }
        }

    }
}

package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.Entity.EntityDriveAdd;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class AL_Yueyatianchong extends SpecialAttackBase {
    @Override
    public String toString() {
        return "yueyatianchong";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        if (!world.isRemote) {

            ItemSlashBlade blade = (ItemSlashBlade)((Object)itemStack.getItem());
            float baseModif = blade.getBaseAttackModifiers(tag);
            int level = EnchantmentHelper.getEnchantmentLevel( Enchantments.POWER, (ItemStack)itemStack);
            float magicDamage ;
            magicDamage = (baseModif+ MathFunc.amplifierCalc(baseModif,4f));
            float[] speeds = new float[]{0.25f, 0.3f, 0.35f, 0.4f, 0.45f};
            for (int i = 0; i < 5; ++i) {

                EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, magicDamage, false, 0.0f - ItemSlashBlade.ComboSequence.Battou.swingDirection);
                entityDrive.setColor(0xFFFFFF);
                entityDrive.scaleX=(float)(0.15 * (double)i * (double)i * (double)i + 0.5);
                entityDrive.scaleY = 0.25f;
                entityDrive.scaleZ=(float)(0.15 * (double)i * (double)i * (double)i + 0.5);
                entityDrive.setInitialSpeed(speeds[i] / 5.0f);
                entityDrive.setLifeTime(100);
                entityDrive.getDataManager().set(EntityDriveAdd.PL_PARTICAL,false);
                if (entityDrive == null) continue;
                world.spawnEntity(entityDrive);
            }
        }
    }
}

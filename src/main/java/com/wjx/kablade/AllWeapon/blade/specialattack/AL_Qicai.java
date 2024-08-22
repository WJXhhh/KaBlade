package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class AL_Qicai extends SpecialAttackBase {
    @Override
    public String toString() {
        return "qicaizhan";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);

        if(!world.isRemote){
            ItemSlashBlade blade = (ItemSlashBlade)(itemStack.getItem());
            float baseModif = ItemSlashBlade.BaseAttackModifier.get(tag)/3;
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
            float magicDamage = baseModif;
            magicDamage+= MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(tag),1f);
            float[] speeds = new float[]{0.55f, 0.5f, 0.45f, 0.4f, 0.35f, 0.3f, 0.25f};
            int[] colors = new int[]{0xFF0000, 0xFF7F00, 0xFFFF00, 65280, 65535, 0x4D4DFF, 0x9932CD};

            for (int i = 0; i < 7; ++i) {
                EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, magicDamage, false, 0.0f - ItemSlashBlade.ComboSequence.Battou.swingDirection);
                entityDrive.setColor(colors[i]);
                entityDrive.scaleX=2.0f;
                entityDrive.scaleY=0.45f;
                entityDrive.scaleZ=2.0f;
                entityDrive.setInitialSpeed(speeds[i] / 5.0f);
                entityDrive.setLifeTime(80);
                entityDrive.getDataManager().set(EntityDriveAdd.PARTICLE_STYLE,"ENCHANTMENT_TABLE");
                if (entityDrive == null) continue;
                world.spawnEntity(entityDrive);
            }

        }
    }
}

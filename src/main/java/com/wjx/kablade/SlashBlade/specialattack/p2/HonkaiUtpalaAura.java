package com.wjx.kablade.SlashBlade.specialattack.p2;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class HonkaiUtpalaAura extends SpecialAttackBase {
    @Override
    public String toString() {
        return "utpala_aura";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.Noutou);
    }
}

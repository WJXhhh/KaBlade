package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityUtpalaAura;
import com.wjx.kablade.util.MathFunc;
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
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SSlashBlade);
        if (!entityPlayer.world.isRemote) {
            float bladeAttack = ItemSlashBlade.BaseAttackModifier.get(tag);
            float damage = (26.0F + MathFunc.amplifierCalc(bladeAttack, 10.0F)) * 2.0F;
            EntityUtpalaAura.spawn(entityPlayer.world, entityPlayer, damage);
            itemStack.damageItem(2, entityPlayer);
        }
    }
}

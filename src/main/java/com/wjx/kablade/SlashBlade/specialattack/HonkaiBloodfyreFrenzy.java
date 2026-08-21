package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityBloodfyreFrenzy;
import com.wjx.kablade.SlashBlade.SpeacialEffects.SEFuelTheRuin;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;

/** 魂妖刀「血樱寂灭」专属 SA：浴血狂樱。 */
public class HonkaiBloodfyreFrenzy extends SpecialAttackBase {
    @Override
    public String toString() {
        return "bloodfyre_frenzy";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer player) {
        if (player.world.isRemote) return;

        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SSlashBlade);
        player.swingArm(EnumHand.MAIN_HAND);

        float bladeAttack = ItemSlashBlade.BaseAttackModifier.get(tag);
        float damage = (54.0F + MathFunc.amplifierCalc(bladeAttack, 12.0F)) * 4.0F;
        EntityBloodfyreFrenzy.spawn(player.world, player, damage);
        SEFuelTheRuin.trigger(player);
        itemStack.damageItem(3, player);

        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.82F, 1.32F);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.74F, 0.72F);
    }
}

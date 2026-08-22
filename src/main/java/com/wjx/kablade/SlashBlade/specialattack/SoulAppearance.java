package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityJizoMitamaSoul;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** 御魂示现：召唤地藏御魂，并在显形蓄势后完成一次正前方下劈。 */
public class SoulAppearance extends SpecialAttackBase {
    private static final float BASE_DAMAGE = 30.0F;
    private static final float EXTRA_DAMAGE_FACTOR = 18.0F;
    private static final float DAMAGE_MULTIPLIER = 2.0F;

    @Override
    public String toString() {
        return "soul_appearance";
    }

    @Override
    public void doSpacialAttack(ItemStack blade, EntityPlayer player) {
        if (player.world.isRemote || EntityJizoMitamaSoul.isCasting(player)) {
            return;
        }

        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(blade);
        float bladeAttack = ItemSlashBlade.BaseAttackModifier.get(tag);
        float damage = (BASE_DAMAGE
                + MathFunc.amplifierCalc(bladeAttack, EXTRA_DAMAGE_FACTOR))
                * DAMAGE_MULTIPLIER;
        EntityJizoMitamaSoul.spawn(player.world, player, damage);
    }
}

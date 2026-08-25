package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityNuclearShock;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** 融核动力剑·改专属 SA：核能震动。 */
public class NuclearShock extends SpecialAttackBase {
    @Override
    public String toString() { return "nuclear_shock"; }

    @Override
    public void doSpacialAttack(ItemStack blade, EntityPlayer player) {
        if (player.world.isRemote || !blade.hasTagCompound()) return;
        Vec3d look = flatLook(player);
        Vec3d impact = player.getPositionVector().add(look.scale(1.65D));
        float attack = ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound());
        float damage = 17.0F + MathFunc.amplifierCalc(attack, 6.0F);
        EntityNuclearShock.spawn(player.world, player, impact, damage);
        blade.damageItem(1, player);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.85F);
    }

    private static Vec3d flatLook(EntityPlayer player) {
        Vec3d look = player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0.0D, look.z);
        return flat.lengthSquared() < 1.0E-6D ? new Vec3d(0.0D, 0.0D, 1.0D) : flat.normalize();
    }
}

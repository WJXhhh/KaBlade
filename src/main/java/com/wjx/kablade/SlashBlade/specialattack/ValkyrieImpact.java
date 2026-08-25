package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityValkyrieImpact;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** 超重剑·冲锋专属 SA：女武神冲击。 */
public class ValkyrieImpact extends SpecialAttackBase {
    @Override
    public String toString() { return "valkyrie_impact"; }

    @Override
    public void doSpacialAttack(ItemStack blade, EntityPlayer player) {
        if (player.world.isRemote || !blade.hasTagCompound()) return;
        Vec3d look = flatLook(player);
        Vec3d impact = player.getPositionVector().add(look.scale(1.25D));
        float attack = ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound());
        float damage = 13.5F + MathFunc.amplifierCalc(attack, 4.875F);
        EntityValkyrieImpact.spawn(player.world, player, impact, player.rotationYaw, damage);
        blade.damageItem(1, player);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.3F, 0.75F);
        player.world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0F, 1.4F);
    }

    private static Vec3d flatLook(EntityPlayer player) {
        Vec3d look = player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0.0D, look.z);
        return flat.lengthSquared() < 1.0E-6D ? new Vec3d(0.0D, 0.0D, 1.0D) : flat.normalize();
    }
}

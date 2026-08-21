package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

final class KBladeSaSupport {
    private KBladeSaSupport() {}

    static float damage(ItemStack blade, float base, float factor, float multiplier) {
        return (base + MathFunc.amplifierCalc(
                ItemSlashBlade.BaseAttackModifier.get(ItemSlashBlade.getItemTagCompound(blade)), factor)) * multiplier;
    }

    static Entity target(EntityPlayer player, ItemStack blade, double range) {
        return TargetingUtil.resolveTarget(player, blade, range, range, Math.min(6.0D, range));
    }

    static Vec3d anchor(Entity target, Vec3d fallback) {
        return target == null ? fallback : target.getEntityBoundingBox().getCenter();
    }

    static Vec3d horizontalLook(EntityPlayer player) {
        Vec3d look = player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        return flat.lengthSquared() < 1.0E-8D ? new Vec3d(0, 0, 1) : flat.normalize();
    }
}

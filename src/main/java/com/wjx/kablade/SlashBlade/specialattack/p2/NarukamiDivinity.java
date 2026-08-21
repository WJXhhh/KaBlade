package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntitySevenThunders;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** 涤罪七雷·鸣雷见 SA：鸣雷神。 */
public class NarukamiDivinity extends SpecialAttackBase {
    @Override public String toString() { return "narukami_divinity"; }
    @Override public void doSpacialAttack(ItemStack blade, EntityPlayer player) {
        if (player.world.isRemote) return;
        if (EntitySevenThunders.isCasting(player, EntitySevenThunders.MODE_NARUKAMI_DIVINITY)) return;
        Entity target = KBladeSaSupport.target(player, blade, 14.0D);
        Vec3d direction = KBladeSaSupport.horizontalLook(player);
        Vec3d anchor = KBladeSaSupport.anchor(target,
                player.getPositionVector().add(0, 1.25D, 0).add(direction.scale(5.5D)));
        float damage = KBladeSaSupport.damage(blade, 54.0F, 14.0F, 8.40F);
        EntitySevenThunders.spawn(player.world, player, target, anchor, direction, damage,
                EntitySevenThunders.MODE_NARUKAMI_DIVINITY);
    }
}

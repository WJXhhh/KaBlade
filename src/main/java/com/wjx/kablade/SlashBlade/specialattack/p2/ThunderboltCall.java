package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntitySevenThunders;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** 涤罪七雷·鸣 SA：唤霆霓。 */
public class ThunderboltCall extends SpecialAttackBase {
    @Override public String toString() { return "thunderbolt_call"; }
    @Override public void doSpacialAttack(ItemStack blade, EntityPlayer player) {
        if (player.world.isRemote) return;
        if (EntitySevenThunders.isCasting(player, EntitySevenThunders.MODE_THUNDERBOLT_CALL)) return;
        Entity target = KBladeSaSupport.target(player, blade, 12.0D);
        Vec3d direction = player.getLookVec().normalize();
        Vec3d anchor = KBladeSaSupport.anchor(target, player.getPositionEyes(1.0F).add(direction.scale(5.0D)));
        float damage = KBladeSaSupport.damage(blade, 50.0F, 12.0F, 5.04F);
        EntitySevenThunders.spawn(player.world, player, target, anchor, direction, damage,
                EntitySevenThunders.MODE_THUNDERBOLT_CALL);
    }
}

package com.wjx.kablade.SlashBlade.specialattack.p2;

import com.wjx.kablade.Entity.EntityConceptualField;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** 澄凝之境 SA：念相了绝。 */
public class ConceptualMetaphor extends SpecialAttackBase {
    @Override public String toString() { return "conceptual_metaphor"; }
    @Override public void doSpacialAttack(ItemStack blade, EntityPlayer player) {
        if (player.world.isRemote) return;
        Entity target = KBladeSaSupport.target(player, blade, 8.0D);
        Vec3d center = KBladeSaSupport.anchor(target, player.getPositionVector());
        float damage = KBladeSaSupport.damage(blade, 20.0F, 5.0F, 2.6F);
        EntityConceptualField.spawn(player.world, player, center, damage, EntityConceptualField.MODE_UNITY);
        blade.damageItem(3, player);
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.SunfireImpactEntity;
import com.wjx.kablade.init.ModComboStates;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaCombatScaling;
import com.wjx.kablade.util.SaTarget;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** A leap followed by a single, owner-attributed impact on landing. */
public final class SunfireImpactArts extends SlashArts {

    public SunfireImpactArts(Function<LivingEntity, ResourceLocation> state) { super(state); }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (type != ArtsType.Fail && SunfireImpactEntity.isCasting(user)) return ModComboStates.SUNFIRE_IMPACT.getId();
        if (!user.level().isClientSide() && type != ArtsType.Fail && user.level() instanceof ServerLevel level) {
            var state = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE);
            Entity locked = state.resolve().map(value -> value.getTargetEntity(user.level())).orElse(null);
            SaTarget target = SaTargeting.findTarget(user, locked, 8.0D).orElse(null);
            float attack = state.map(ISlashBladeState::getBaseAttackModifier).orElse(14.0F);
            // Death Gaze hits at ages 3..24 every three ticks, each for total / 6.5.
            float damage = SaCombatScaling.scaleDamage(user,
                    (16.0F + MathFunc.amplifierCalc(attack, 4.5F)) * 1.5F * (8.0F / 6.5F));
            Vec3 destination = target == null ? user.position().add(SaFx.flatLook(user).scale(4)) : target.anchor();
            if (SunfireImpactEntity.spawn(level, user, destination, damage) == null) return super.doArts(type, user);
        }
        return type == ArtsType.Fail ? super.doArts(type, user) : ModComboStates.SUNFIRE_IMPACT.getId();
    }

}

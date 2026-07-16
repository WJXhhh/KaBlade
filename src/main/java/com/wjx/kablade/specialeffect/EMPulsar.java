package com.wjx.kablade.specialeffect;

import com.wjx.kablade.init.ModSpecialEffects;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** EM Pulsar, MAG-Typhoon's defensive field activated by a successful slash art. */
public final class EMPulsar extends SpecialEffect {

    private static final int RESISTANCE_DURATION = 6 * 20;
    private static final int RESISTANCE_AMPLIFIER = 2;

    public EMPulsar() {
        super(-1, true, true);
    }

    public static void activate(LivingEntity user, ItemStack blade) {
        if (user.level().isClientSide() || !(blade.getItem() instanceof ItemSlashBlade)) {
            return;
        }
        boolean enabled = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.EM_PULSAR.getId()))
                .orElse(false);
        if (enabled) {
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    RESISTANCE_DURATION, RESISTANCE_AMPLIFIER));
        }
    }
}

package com.wjx.kablade.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Damage helpers for Kablade SA/SE multi-hit timing. */
public final class SaDamage {

    private SaDamage() {
    }

    public static boolean hurtNoIFrame(LivingEntity target, DamageSource source, float amount) {
        clearInvulnerability(target);
        boolean hurt = target.hurt(source, amount);
        target.invulnerableTime = 0;
        return hurt;
    }

    public static void clearInvulnerability(LivingEntity target) {
        target.hurtTime = 0;
        target.invulnerableTime = 0;
    }
}

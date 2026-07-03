package com.wjx.kablade.mobeffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Absolute movement lock ported from KaBlade 1.12.2 PotionFreeze.
 */
public class FreezeMobEffect extends MobEffect {

    public static final UUID UUID_FREEZE =
            UUID.fromString("3fccf4fc-3ea9-366b-98e6-607f8dcec98c");

    public FreezeMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xBFEFFF);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                UUID_FREEZE.toString(), -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() + 20));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}

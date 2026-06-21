package com.wjx.kablade.mobeffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 麻痹效果 —— 从 1.12.2 {@code PotionParaly} 移植而来。
 * <p>
 * 负面效果，降低受击者移动速度。减速曲线一比一复刻原版：低等级减速较弱，
 * amplifier ≥ 12 时锁定为 -94%（UUID 与减速值均与 1.12.2 保持一致）。
 */
public class ParalysisMobEffect extends MobEffect {

    private static final UUID UUID_PARALYSIS =
            UUID.fromString("3fccf4fc-3ea9-366b-98e6-607f8dcec98d");

    public ParalysisMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xf3eb20);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                UUID_PARALYSIS.toString(), -0.05D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /**
     * 一比一复刻 1.12.2 {@code PotionParaly.getAttributeModifierAmount}：
     * amplifier &lt; 12 时为 {@code (1 + amplifier * 0.08) * 基础值(-0.05)}，否则固定 -0.94。
     */
    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return amplifier < 12 ? (1.0D + amplifier * 0.08D) * modifier.getAmount() : -0.94D;
    }
}

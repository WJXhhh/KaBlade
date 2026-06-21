package com.wjx.kablade.mobeffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 禁锢标记 —— 从 1.12.2 {@code KaBladeEntityProperties.CONFINEMENT} 移植而来。
 * <p>
 * 无实际属性修改，仅作为「高频坍缩」力场的伤害放大标记。持有该效果时
 * 受到伤害会额外放大（由事件监听处理）。
 */
public class ConfinementMobEffect extends MobEffect {

    public ConfinementMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A5ACD);
    }
}

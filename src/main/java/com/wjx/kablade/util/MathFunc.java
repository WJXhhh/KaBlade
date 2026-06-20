package com.wjx.kablade.util;

/**
 * 数学工具方法。
 */
public final class MathFunc {

    private MathFunc() {
    }

    /**
     * 攻击力补正计算（复刻 1.12.2 KaBlade 的 {@code MathFunc.amplifierCalc}）。
     * <p>
     * 公式：{@code log((|amp| + 0.5) * 4) * factor}（自然对数）。
     * 随攻击力对数增长、不封顶——攻击力越高额外伤害越大，但增速逐渐放缓。
     * 各 SA 的 {@code ATTACK_FACTOR} 即此处的 {@code factor}。
     *
     * @param amp    刀的基础攻击力
     * @param factor 补正系数（沿用 1.12.2 各招的取值）
     * @return 额外伤害
     */
    public static float amplifierCalc(float amp, float factor) {
        if (Math.abs(amp) <= 0.5F) {
            amp = Math.abs(amp) + 0.5F;
        }
        return (float) (Math.log((Math.abs(amp) + 0.5F) * 4.0F) * factor);
    }
}

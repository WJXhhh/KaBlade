package com.wjx.kablade.SlashBlade.specialattack;

/** 天殛之境的权威时间轴；数值与 1.20 版本保持一致。 */
public final class RaizanCleaveTimeline {
    public static final float REFERENCE_FPS = 12.76F;
    public static final int FRAME_COUNT = 67;
    public static final int DURATION_TICKS = 105;
    public static final int[] HIT_TICKS = {30, 34, 45, 50, 55, 60, 64, 69, 74};
    public static final float[] DAMAGE_WEIGHTS = {
            0.18F, 0.07F, 0.06F, 0.07F, 0.08F, 0.10F, 0.10F, 0.14F, 0.20F
    };
    private static final float[] VISUAL_TICKS = {
            0, 6, 13, 19, 29, 30, 34, 39, 45, 50, 55, 60, 64, 69, 74, 79, 89, 96, 105
    };
    private static final float[] VISUAL_FRAMES = {
            0, 4, 8.6F, 12.2F, 18.2F, 19, 22, 25, 29, 32, 35, 38, 41, 44, 47.5F, 50.5F, 56.5F, 60.5F, 66
    };
    private static final float[] TANGENTS = tangents();

    private RaizanCleaveTimeline() {}

    public static float referenceFrame(float ageTicks) {
        float age = Math.max(0, Math.min(DURATION_TICKS, ageTicks));
        for (int i = 1; i < VISUAL_TICKS.length; i++) {
            if (age <= VISUAL_TICKS[i]) {
                float x0 = VISUAL_TICKS[i - 1], x1 = VISUAL_TICKS[i], h = x1 - x0;
                float t = (age - x0) / h, t2 = t * t, t3 = t2 * t;
                return Math.max(0, Math.min(66,
                        (2 * t3 - 3 * t2 + 1) * VISUAL_FRAMES[i - 1]
                                + (t3 - 2 * t2 + t) * h * TANGENTS[i - 1]
                                + (-2 * t3 + 3 * t2) * VISUAL_FRAMES[i]
                                + (t3 - t2) * h * TANGENTS[i]));
            }
        }
        return 66;
    }

    private static float[] tangents() {
        float[] secants = new float[VISUAL_TICKS.length - 1];
        float[] result = new float[VISUAL_TICKS.length];
        for (int i = 0; i < secants.length; i++)
            secants[i] = (VISUAL_FRAMES[i + 1] - VISUAL_FRAMES[i]) / (VISUAL_TICKS[i + 1] - VISUAL_TICKS[i]);
        result[0] = secants[0]; result[result.length - 1] = secants[secants.length - 1];
        for (int i = 1; i < result.length - 1; i++) {
            float hp = VISUAL_TICKS[i] - VISUAL_TICKS[i - 1], hn = VISUAL_TICKS[i + 1] - VISUAL_TICKS[i];
            float w1 = 2 * hn + hp, w2 = hn + 2 * hp;
            result[i] = (w1 + w2) / (w1 / secants[i - 1] + w2 / secants[i]);
        }
        return result;
    }
}

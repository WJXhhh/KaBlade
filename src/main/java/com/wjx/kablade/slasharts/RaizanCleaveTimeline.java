package com.wjx.kablade.slasharts;

/** Authoritative timing and damage data for Raizan Cleave. */
public final class RaizanCleaveTimeline {

    public static final float REFERENCE_FPS = 12.76F;
    public static final int FRAME_COUNT = 67;
    public static final int LAST_FRAME = FRAME_COUNT - 1;
    public static final int DURATION_TICKS = 105;
    public static final int PHASE_TWO_START_TICK = 39;

    public static final float[] HIT_FRAMES = {
            19.0F, 22.0F, 29.0F, 32.0F, 35.0F, 38.0F, 41.0F, 44.0F, 47.5F
    };
    public static final int[] HIT_TICKS = {30, 34, 45, 50, 55, 60, 64, 69, 74};
    public static final float[] DAMAGE_WEIGHTS = {
            0.18F, 0.07F, 0.06F, 0.07F, 0.08F, 0.10F, 0.10F, 0.14F, 0.20F
    };

    private static final float[] VISUAL_TICKS = {
            0.0F, 6.0F, 13.0F, 19.0F, 29.0F, 30.0F, 34.0F, 39.0F,
            45.0F, 50.0F, 55.0F, 60.0F, 64.0F, 69.0F, 74.0F, 79.0F,
            89.0F, 96.0F, 105.0F
    };
    private static final float[] VISUAL_FRAMES = {
            0.0F, 4.0F, 8.6F, 12.2F, 18.2F, 19.0F, 22.0F, 25.0F,
            29.0F, 32.0F, 35.0F, 38.0F, 41.0F, 44.0F, 47.5F, 50.5F,
            56.5F, 60.5F, 66.0F
    };
    private static final float[] VISUAL_TANGENTS = monotoneTangents();

    private RaizanCleaveTimeline() {
    }

    public static float referenceFrame(float ageTicks) {
        float age = Math.min(DURATION_TICKS, Math.max(0.0F, ageTicks));
        if (age <= VISUAL_TICKS[0]) {
            return VISUAL_FRAMES[0];
        }
        for (int i = 1; i < VISUAL_TICKS.length; i++) {
            if (age <= VISUAL_TICKS[i]) {
                float x0 = VISUAL_TICKS[i - 1];
                float x1 = VISUAL_TICKS[i];
                float h = x1 - x0;
                float t = (age - x0) / h;
                float t2 = t * t;
                float t3 = t2 * t;
                float h00 = 2.0F * t3 - 3.0F * t2 + 1.0F;
                float h10 = t3 - 2.0F * t2 + t;
                float h01 = -2.0F * t3 + 3.0F * t2;
                float h11 = t3 - t2;
                return Math.min(LAST_FRAME, Math.max(0.0F,
                        h00 * VISUAL_FRAMES[i - 1]
                                + h10 * h * VISUAL_TANGENTS[i - 1]
                                + h01 * VISUAL_FRAMES[i]
                                + h11 * h * VISUAL_TANGENTS[i]));
            }
        }
        return LAST_FRAME;
    }

    /** Fritsch-Carlson tangents keep the cinematic remap monotonic without hit-stop. */
    private static float[] monotoneTangents() {
        int count = VISUAL_TICKS.length;
        float[] secants = new float[count - 1];
        float[] tangents = new float[count];
        for (int i = 0; i < secants.length; i++) {
            secants[i] = (VISUAL_FRAMES[i + 1] - VISUAL_FRAMES[i])
                    / (VISUAL_TICKS[i + 1] - VISUAL_TICKS[i]);
        }
        tangents[0] = secants[0];
        tangents[count - 1] = secants[secants.length - 1];
        for (int i = 1; i < count - 1; i++) {
            float previous = secants[i - 1];
            float next = secants[i];
            float hPrevious = VISUAL_TICKS[i] - VISUAL_TICKS[i - 1];
            float hNext = VISUAL_TICKS[i + 1] - VISUAL_TICKS[i];
            float w1 = 2.0F * hNext + hPrevious;
            float w2 = hNext + 2.0F * hPrevious;
            tangents[i] = (w1 + w2) / (w1 / previous + w2 / next);
        }
        return tangents;
    }
}

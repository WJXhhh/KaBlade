package com.wjx.kablade.slasharts;

import net.minecraft.util.Mth;

/** Shared 41-frame reference timing mapped onto a 54-tick in-game presentation. */
public final class ThunderboltCallTimeline {

    public static final int DURATION_TICKS = 54;
    public static final float LAST_REFERENCE_FRAME = 40.0F;

    /** One setup impact followed by one simultaneous hit across the complete X volume. */
    public static final int[] HIT_TICKS = {7, 16};
    public static final float[] DAMAGE_WEIGHTS = {0.18F, 0.82F};

    private ThunderboltCallTimeline() {
    }

    public static float referenceFrame(float ageTicks) {
        return Mth.clamp(ageTicks, 0.0F, DURATION_TICKS)
                * LAST_REFERENCE_FRAME / DURATION_TICKS;
    }

    public static float smooth(float value, float from, float to) {
        if (Math.abs(to - from) < 1.0E-6F) {
            return value >= to ? 1.0F : 0.0F;
        }
        float x = Mth.clamp((value - from) / (to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public static float smoother(float value, float from, float to) {
        float x = Mth.clamp((value - from) / Math.max(1.0E-6F, to - from), 0.0F, 1.0F);
        return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
    }

    public static float plateau(float frame, float start, float full, float fade, float end) {
        if (frame < start || frame > end) {
            return 0.0F;
        }
        return Math.min(smooth(frame, start, full), 1.0F - smooth(frame, fade, end));
    }

    public static float gaussian(float value, float center, float width) {
        float x = (value - center) / Math.max(width, 0.001F);
        return (float) Math.exp(-x * x);
    }

    public static float easeOutCubic(float value) {
        float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }

    public static float crossTravelDistance(float frame) {
        float launch = easeOutCubic(smooth(frame, 11.05F, 17.15F));
        float coast = smooth(frame, 17.15F, 24.85F);
        return launch * 4.95F + coast * 1.35F;
    }

    public static float crossScale(float frame) {
        float launch = smooth(frame, 11.05F, 17.15F);
        float coast = smooth(frame, 17.15F, 24.85F);
        float perspective = 1.025F - launch * 0.105F - coast * 0.075F;
        float birthPunch = 1.0F + gaussian(frame, 12.25F, 0.82F) * 0.045F;
        return Math.max(0.78F, perspective * birthPunch);
    }

    public static float impactStrength(float frame) {
        return Mth.clamp(
                gaussian(frame, 3.8F, 0.58F) * 0.58F
                        + gaussian(frame, 5.05F, 0.92F) * 0.86F
                        + gaussian(frame, 11.5F, 0.62F) * 0.72F
                        + gaussian(frame, 12.65F, 0.86F)
                        + gaussian(frame, 14.3F, 1.45F) * 0.36F
                        + gaussian(frame, 22.2F, 0.72F) * 0.12F,
                0.0F, 1.0F);
    }

    public static float fovImpulse(float frame) {
        float first = cinematicFov(frame, 5.0F, 2.25F);
        float cross = cinematicFov(frame, 12.55F, 3.4F);
        return Math.abs(first) > Math.abs(cross) ? first : cross;
    }

    private static float cinematicFov(float frame, float hit, float amplitude) {
        float compression = gaussian(frame, hit - 0.46F, 0.34F);
        float release = gaussian(frame, hit + 0.52F, 0.66F);
        return -amplitude * 0.50F * compression + amplitude * 0.70F * release;
    }
}

package com.wjx.kablade.slasharts;

import net.minecraft.util.Mth;

/**
 * Shared timing for 鸣雷神 / Narukami Divinity.
 *
 * <p>The supplied reference is authored as F0-F40 at 15 FPS. Minecraft advances
 * the timeline at 20 ticks per second, so all visual interpolation keeps the
 * fractional reference frame instead of rounding to GIF frames.
 */
public final class NarukamiDivinityTimeline {

    public static final int DURATION_TICKS = 54;
    public static final float LAST_REFERENCE_FRAME = 40.0F;

    /** Opening, two X cuts, five cage pierces, then the collapsing low orbit. */
    public static final int[] HIT_TICKS = {5, 9, 10, 24, 25, 26, 28, 29, 31};
    public static final float[] DAMAGE_WEIGHTS = {
            0.12F, 0.14F, 0.14F,
            0.08F, 0.08F, 0.08F, 0.08F, 0.08F,
            0.20F
    };

    public static final Cue ENTRY_FRONT =
            new Cue(13.92F, 14.16F, 14.70F, 15.26F, 15.62F, 16.05F, 1.00F, 0.50F);
    public static final Cue ENTRY_UPPER =
            new Cue(14.42F, 14.78F, 15.46F, 16.08F, 16.72F, 17.28F, 0.80F, 0.30F);
    public static final Cue ORBIT_A =
            new Cue(14.92F, 15.28F, 15.92F, 16.48F, 17.12F, 17.76F, 0.46F, 0.05F);
    public static final Cue ORBIT_B =
            new Cue(15.52F, 15.92F, 16.62F, 17.18F, 18.12F, 18.76F, 0.42F, 0.04F);
    public static final Cue ORBIT_C =
            new Cue(16.20F, 16.62F, 17.28F, 17.92F, 18.86F, 19.62F, 0.38F, 0.03F);
    public static final Cue RING_BACK_A =
            new Cue(15.72F, 16.12F, 16.78F, 17.36F, 18.12F, 18.82F, 0.34F, 0.02F);
    public static final Cue RING_BACK_B =
            new Cue(16.20F, 16.58F, 17.24F, 17.86F, 18.78F, 19.50F, 0.30F, 0.02F);
    public static final Cue RING_BACK_C =
            new Cue(17.00F, 17.42F, 18.10F, 18.84F, 20.35F, 21.20F, 0.26F, 0.01F);

    public static final Cue PIERCE_VERTICAL =
            new Cue(16.82F, 17.22F, 17.92F, 18.36F, 18.92F, 20.02F, 1.00F, 0.88F);
    public static final Cue PIERCE_RISING =
            new Cue(17.56F, 17.94F, 18.70F, 19.20F, 19.86F, 21.02F, 0.98F, 0.84F);
    public static final Cue PIERCE_HORIZONTAL =
            new Cue(18.42F, 18.84F, 19.62F, 20.14F, 20.92F, 22.18F, 0.92F, 0.74F);
    public static final Cue PIERCE_FALLING =
            new Cue(19.42F, 19.86F, 20.62F, 21.18F, 21.96F, 23.16F, 0.96F, 0.80F);
    public static final Cue PIERCE_OFFSET =
            new Cue(20.42F, 20.86F, 21.58F, 22.16F, 22.88F, 24.08F, 0.88F, 0.66F);
    public static final Cue COLLAPSE_MAIN =
            new Cue(19.72F, 20.12F, 21.12F, 21.88F, 23.08F, 24.68F, 0.92F, 0.70F);
    public static final Cue COLLAPSE_ECHO =
            new Cue(20.68F, 21.12F, 21.84F, 22.52F, 23.52F, 24.42F, 0.30F, 0.03F);
    public static final Cue RESIDUAL_DIAGONAL =
            new Cue(22.08F, 22.48F, 23.28F, 24.10F, 27.18F, 28.72F, 0.80F, 0.30F);
    public static final Cue RESIDUAL_HOOK =
            new Cue(23.42F, 23.86F, 25.10F, 26.05F, 28.10F, 29.00F, 0.48F, 0.12F);

    private NarukamiDivinityTimeline() {
    }

    public static float referenceFrame(float ageTicks) {
        return Mth.clamp(ageTicks * 15.0F / 20.0F, 0.0F, LAST_REFERENCE_FRAME);
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

    public static float envelope(Cue cue, float frame) {
        return cue.maxOpacity * Math.min(
                smooth(frame, cue.start, cue.impact),
                1.0F - smooth(frame, cue.release, cue.end));
    }

    public static float precursor(Cue cue, float frame) {
        return cue.maxOpacity * 0.30F
                * plateau(frame, cue.anticipate, cue.start, cue.impact, cue.handoff);
    }

    public static float wake(Cue cue, float frame) {
        return cue.maxOpacity * 0.28F
                * plateau(frame, cue.impact, cue.handoff, cue.release, cue.end);
    }

    public static float reveal(Cue cue, float frame) {
        return smooth(frame, cue.start, cue.impact);
    }

    public static float exit(Cue cue, float frame) {
        return 0.94F * smooth(frame, cue.release, cue.end);
    }

    public static float impactStrength(float frame) {
        float opening = gaussian(frame, 0.65F, 0.50F);
        float crossA = gaussian(frame, 6.70F, 0.44F) * 0.76F;
        float crossB = gaussian(frame, 7.35F, 0.46F) * 0.82F;
        float cage = Math.max(
                Math.max(gaussian(frame, 17.92F, 0.42F), gaussian(frame, 18.70F, 0.42F)),
                Math.max(gaussian(frame, 19.62F, 0.44F), gaussian(frame, 20.62F, 0.46F)));
        cage = Math.max(cage, gaussian(frame, 21.58F, 0.48F));
        float collapse = gaussian(frame, 23.08F, 0.70F) * 0.88F;
        return Mth.clamp(Math.max(Math.max(opening, crossA + crossB * 0.55F),
                Math.max(cage * 0.68F, collapse)), 0.0F, 1.0F);
    }

    public static float fovImpulse(float frame) {
        float opening = cinematicFov(frame, 0.65F, 2.4F);
        float cross = cinematicFov(frame, 7.15F, 3.1F);
        float collapse = cinematicFov(frame, 23.08F, 2.7F);
        float strongest = Math.abs(opening) > Math.abs(cross) ? opening : cross;
        return Math.abs(strongest) > Math.abs(collapse) ? strongest : collapse;
    }

    private static float cinematicFov(float frame, float hit, float amplitude) {
        float compression = gaussian(frame, hit - 0.30F, 0.26F);
        float release = gaussian(frame, hit + 0.44F, 0.58F);
        return -amplitude * 0.44F * compression + amplitude * 0.68F * release;
    }

    /**
     * Picks one white-core leader during the cage sequence. Supporting tracks
     * remain purple so the effect never reads as a glowing shell.
     */
    public static Cue dominantCue(float frame) {
        Cue[] candidates = {
                PIERCE_VERTICAL, PIERCE_RISING, PIERCE_HORIZONTAL,
                PIERCE_FALLING, PIERCE_OFFSET, COLLAPSE_MAIN, RESIDUAL_DIAGONAL
        };
        Cue best = null;
        float bestScore = 0.0F;
        for (Cue cue : candidates) {
            float score = envelope(cue, frame) * (0.55F + cue.bloom);
            if (score > bestScore) {
                bestScore = score;
                best = cue;
            }
        }
        return best;
    }

    public record Cue(float anticipate, float start, float impact, float handoff,
                      float release, float end, float maxOpacity, float bloom) {
    }
}

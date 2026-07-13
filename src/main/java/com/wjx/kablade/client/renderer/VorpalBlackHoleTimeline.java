package com.wjx.kablade.client.renderer;

import net.minecraft.util.Mth;

/** Continuous 30 FPS reference clock and envelopes for the Vorpal Hole VFX. */
public final class VorpalBlackHoleTimeline {

    public static final float REFERENCE_FPS = 30.0F;
    public static final float FRAMES_PER_TICK = REFERENCE_FPS / 20.0F;
    public static final float LAST_FRAME = 56.0F;

    private VorpalBlackHoleTimeline() {
    }

    public static float frame(float ageTicks) {
        return Mth.clamp(ageTicks * FRAMES_PER_TICK, 0.0F, LAST_FRAME);
    }

    public static float smooth(float edge0, float edge1, float value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0F : 1.0F;
        }
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float plateau(float frame, float start, float riseEnd, float holdEnd, float end) {
        if (frame < start || frame > end) {
            return 0.0F;
        }
        if (frame < riseEnd) {
            return smooth(start, riseEnd, frame);
        }
        if (frame <= holdEnd) {
            return 1.0F;
        }
        return 1.0F - smooth(holdEnd, end, frame);
    }

    public static float gaussian(float frame, float center, float sigma) {
        float x = (frame - center) / sigma;
        return (float) Math.exp(-0.5F * x * x);
    }

    public static float easeOutBack(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F) - 1.0F;
        float c1 = 1.70158F;
        return 1.0F + (c1 + 1.0F) * t * t * t + c1 * t * t;
    }

    public static float vortexSmoke(float f) { return plateau(f, 19.6F, 21.1F, 31.0F, 35.2F); }
    public static float vortexSpiral(float f) { return plateau(f, 19.8F, 21.0F, 31.5F, 34.2F); }
    public static float vortexCore(float f) { return plateau(f, 19.7F, 20.8F, 32.0F, 34.4F); }
    /** Immediate low-energy singularity that bridges cast start into the full F20 void. */
    public static float vortexSeed(float f) {
        if (f < 19.7F) {
            return 0.38F + smooth(0.0F, 19.7F, f) * 0.10F;
        }
        if (f < 20.8F) {
            return Mth.lerp(smooth(19.7F, 20.8F, f), 0.48F, 1.0F);
        }
        return 0.0F;
    }
    public static float vortexArcs(float f) { return plateau(f, 20.8F, 22.0F, 28.3F, 31.0F); }
    public static float vortexNeedles(float f) { return plateau(f, 19.9F, 21.5F, 33.0F, 35.1F); }
    public static float vortexEmbers(float f) { return plateau(f, 20.1F, 22.0F, 35.2F, 39.2F); }
    public static float entryRibbon(float f) { return plateau(f, 18.2F, 19.5F, 24.2F, 28.4F); }
    public static float weaponTrail(float f) {
        return Math.max(plateau(f, 17.0F, 18.5F, 25.0F, 29.0F),
                plateau(f, 29.0F, 30.0F, 42.0F, 45.0F) * 0.88F);
    }
    public static float preFlash(float f) { return plateau(f, 26.8F, 27.2F, 29.6F, 30.4F); }
    public static float redSpikes(float f) { return plateau(f, 28.7F, 29.3F, 32.8F, 34.5F); }
    public static float blackSpikes(float f) { return plateau(f, 28.6F, 29.2F, 33.2F, 35.2F); }
    public static float impactCore(float f) { return plateau(f, 28.8F, 29.4F, 33.4F, 36.0F); }
    public static float shockwave(float f) { return plateau(f, 29.0F, 29.5F, 34.0F, 37.0F); }
    public static float horizontalSever(float f) { return plateau(f, 29.0F, 29.7F, 33.8F, 35.6F); }
    public static float magentaBeam(float f) { return plateau(f, 30.7F, 31.4F, 34.2F, 36.3F); }
    public static float speedStreaks(float f) { return plateau(f, 33.1F, 34.0F, 37.2F, 39.6F); }
    public static float followupCrescent(float f) { return plateau(f, 33.7F, 34.8F, 40.2F, 43.6F); }
    public static float thinCuts(float f) { return plateau(f, 34.0F, 35.0F, 44.0F, 46.5F); }
    public static float debris(float f) { return plateau(f, 29.0F, 30.5F, 43.0F, 47.5F); }

    public static float cameraImpact(float f) {
        return Mth.clamp(
                gaussian(f, 29.25F, 0.48F) * 0.72F
                        + gaussian(f, 31.1F, 0.95F) * 0.55F
                        + gaussian(f, 33.0F, 1.35F) * 0.24F,
                0.0F, 1.0F);
    }
}

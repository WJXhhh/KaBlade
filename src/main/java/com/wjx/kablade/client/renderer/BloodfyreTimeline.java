package com.wjx.kablade.client.renderer;

import net.minecraft.util.Mth;

/** Shared timing curves for the Bloodfyre Frenzy presentation. */
final class BloodfyreTimeline {
    static final float GUIDE_START = 5.0F;
    static final float GUIDE_END = 9.6F;
    static final float SLASH_START = 8.4F;
    static final float SLASH_END = 13.8F;
    static final float RUPTURE_START = 10.8F;
    static final float RUPTURE_END = 28.0F;
    static final float SMOKE_START = 11.4F;
    static final float SMOKE_END = 35.0F;
    static final float SCAR_START = 8.7F;
    static final float SCAR_END = 52.0F;
    static final float MAIN_FADE_START = 29.5F;
    static final float MAIN_FADE_END = 34.0F;

    private BloodfyreTimeline() {
    }

    static float slashProgress(float age) {
        float raw = Mth.clamp((age - SLASH_START) / (SLASH_END - SLASH_START), 0.0F, 1.0F);
        if (raw < 0.55F) {
            float fast = raw / 0.55F;
            return 0.72F * (1.0F - (float) Math.pow(1.0F - fast, 3.0D));
        }
        return 0.72F + 0.28F * smooth((raw - 0.55F) / 0.45F);
    }

    static float slashBirth(float u) {
        float low = SLASH_START;
        float high = SLASH_END;
        for (int i = 0; i < 16; i++) {
            float middle = (low + high) * 0.5F;
            if (slashProgress(middle) < u) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) * 0.5F;
    }

    static float mainAlpha(float age) {
        return age < SLASH_START ? 0.0F
                : age <= MAIN_FADE_START ? 1.0F
                : 1.0F - smooth((age - MAIN_FADE_START) / (MAIN_FADE_END - MAIN_FADE_START));
    }

    static float bodyErosion(float age) {
        // A true ease-in over the entire remaining lifetime: slow corrosion just after impact,
        // then continuously increasing speed until the exact frame mainAlpha reaches zero.
        // Unlike smoothstep/piecewise curves, pow(t, 1.65) never eases out near the end, so it
        // cannot visually park on a half-eroded silhouette before the alpha cleanup begins.
        float progress = Mth.clamp((age - 14.6F) / (MAIN_FADE_END - 14.6F), 0.0F, 1.0F);
        return (float) Math.pow(progress, 1.65D);
    }

    static float scarAlpha(float age) {
        if (age < SCAR_START) {
            return 0.0F;
        }
        if (age < 13.5F) {
            return smooth((age - SCAR_START) / (13.5F - SCAR_START));
        }
        if (age <= 44.0F) {
            return 1.0F;
        }
        return 1.0F - smooth((age - 44.0F) / (SCAR_END - 44.0F));
    }

    /**
     * Persistent emissive lift with a short overexposure kick at the completed slash.
     * The world-space layers own this pulse so it never turns into a full-screen flash.
     */
    static float glowIntensity(float age) {
        float distance = (age - SLASH_END) / 1.22F;
        return 1.0F + 0.70F * (float) Math.exp(-distance * distance);
    }

    static float window(float age, float start, float end, float fadeIn, float fadeOut) {
        if (age <= start || age >= end) {
            return 0.0F;
        }
        float open = smooth((age - start) / Math.max(fadeIn, 0.001F));
        float close = 1.0F - smooth((age - (end - fadeOut)) / Math.max(fadeOut, 0.001F));
        return open * close;
    }

    static float fastOut(float value) {
        float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse * inverse;
    }

    static float smooth(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }
}

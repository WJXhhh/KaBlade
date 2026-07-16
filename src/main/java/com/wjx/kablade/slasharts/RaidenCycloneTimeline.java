package com.wjx.kablade.slasharts;

import net.minecraft.util.Mth;

/** Shared, deterministic timing data for Raiden's Cyclone. */
public final class RaidenCycloneTimeline {

    public static final float DURATION_SECONDS = 5.04F;
    public static final int DURATION_TICKS = 101;

    public static final float[] HIT_SECONDS = {
            0.13F, 0.39F, 0.72F, 1.05F, 1.39F,
            2.18F, 2.78F, 3.02F, 3.48F, 3.96F, 4.20F
    };
    public static final int[] HIT_TICKS = {3, 8, 14, 21, 28, 44, 56, 60, 70, 79, 84};
    public static final float[] DAMAGE_WEIGHTS = {
            0.04F, 0.04F, 0.04F, 0.04F, 0.04F,
            0.13F, 0.10F, 0.10F, 0.11F, 0.14F, 0.22F
    };

    public static final SlashSpec[] SLASHES = {
            new SlashSpec(0.06F, 0.138F, 0.058F, 0.232F, 0.300F, 0.40F,
                    -2.55F, 1.0F, 3.24F, 1.43F, 0.72F, 0.86F, 0.18F, 0.08F, -0.08F, -0.76F, -0.10F, true),
            new SlashSpec(0.30F, 0.130F, 0.054F, 0.218F, 0.286F, 0.38F,
                    2.70F, -1.0F, 3.50F, 1.66F, 0.68F, 1.08F, 0.22F, -0.05F, 0.14F, -0.74F, -0.08F, true),
            new SlashSpec(0.61F, 0.148F, 0.066F, 0.245F, 0.318F, 0.42F,
                    -2.15F, 1.0F, 3.90F, 1.84F, 0.74F, 1.25F, 0.16F, 0.12F, -0.18F, -0.70F, -0.04F, false),
            new SlashSpec(0.94F, 0.136F, 0.058F, 0.228F, 0.298F, 0.39F,
                    2.82F, -1.0F, 3.72F, 2.02F, 0.69F, 0.98F, 0.21F, -0.08F, 0.18F, -0.69F, -0.02F, false),
            new SlashSpec(1.27F, 0.128F, 0.054F, 0.214F, 0.282F, 0.37F,
                    -2.30F, 1.0F, 3.62F, 2.18F, 0.63F, 1.18F, 0.14F, 0.05F, -0.10F, -0.66F, -0.02F, false),
            new SlashSpec(2.58F, 0.126F, 0.052F, 0.212F, 0.278F, 0.38F,
                    -2.62F, 1.0F, 2.92F, 1.25F, 0.64F, 1.18F, 0.18F, 0.16F, -0.28F, 0.0F, 0.0F, true),
            new SlashSpec(3.30F, 0.118F, 0.050F, 0.202F, 0.265F, 0.36F,
                    2.78F, -1.0F, 3.08F, 1.42F, 0.70F, 1.36F, 0.24F, -0.22F, 0.36F, 0.0F, 0.0F, false),
            new SlashSpec(3.76F, 0.112F, 0.047F, 0.194F, 0.255F, 0.35F,
                    -2.72F, 1.0F, 3.22F, 1.56F, 0.78F, 1.08F, 0.20F, 0.22F, -0.40F, 0.0F, 0.0F, true),
            new SlashSpec(4.04F, 0.108F, 0.045F, 0.188F, 0.248F, 0.34F,
                    2.90F, -1.0F, 3.38F, 1.72F, 0.82F, 1.46F, 0.28F, -0.18F, 0.50F, 0.0F, 0.0F, false)
    };

    private static final float[][] X_KEYS = {
            {0.00F,-0.82F},{0.25F,-0.68F},{0.60F,-0.28F},{0.96F,0.05F},{1.30F,-0.22F},
            {1.64F,-0.56F},{1.84F,-0.82F},{2.12F,-1.02F},{2.52F,-0.96F},{2.78F,-0.20F},
            {3.08F,-0.72F},{3.34F,-0.92F},{3.56F,-0.16F},{3.82F,-0.92F},{4.05F,-0.18F},
            {4.28F,-0.78F},{4.52F,-1.04F},{5.04F,-0.84F}
    };
    private static final float[][] Z_KEYS = {
            {0.00F,-0.28F},{0.25F,-0.18F},{0.60F,-0.54F},{0.96F,-0.24F},{1.30F,0.18F},
            {1.64F,0.05F},{1.84F,-0.12F},{2.12F,-0.22F},{2.52F,-0.14F},{2.78F,0.10F},
            {3.08F,0.28F},{3.34F,0.42F},{3.56F,0.02F},{3.82F,-0.28F},{4.05F,0.25F},
            {4.28F,0.42F},{4.52F,0.05F},{5.04F,-0.12F}
    };
    private static final float[][] YAW_KEYS = {
            {0.00F,-0.45F},{0.20F,-0.85F},{0.55F,1.50F},{0.92F,3.72F},{1.28F,5.74F},
            {1.62F,7.45F},{1.84F,8.24F},{2.04F,8.05F},{2.52F,7.82F},{2.78F,8.72F},
            {3.10F,8.10F},{3.34F,8.88F},{3.58F,8.22F},{3.82F,9.10F},{4.08F,8.42F},
            {4.28F,9.28F},{4.50F,8.52F},{5.04F,8.12F}
    };

    private RaidenCycloneTimeline() {
    }

    /** Local cast-scene position relative to the reference target at (0.72, 0.12). */
    public static LocalPose samplePlayer(float seconds) {
        float t = Mth.clamp(seconds, 0.0F, DURATION_SECONDS);
        return new LocalPose(keyframe(X_KEYS, t) - 0.72F,
                keyframe(Z_KEYS, t) - 0.12F, keyframe(YAW_KEYS, t));
    }

    public static float envelope(float t, float start, float peak, float end) {
        if (t <= start || t >= end) return 0.0F;
        if (t < peak) return smooth((t - start) / Math.max(peak - start, 1.0E-4F));
        return 1.0F - smooth((t - peak) / Math.max(end - peak, 1.0E-4F));
    }

    public static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float gaussian(float t, float center, float width) {
        float x = (t - center) / Math.max(width, 1.0E-4F);
        return (float) Math.exp(-x * x * 0.5F);
    }

    private static float keyframe(float[][] keys, float t) {
        if (t <= keys[0][0]) return keys[0][1];
        for (int i = 1; i < keys.length; i++) {
            if (t <= keys[i][0]) {
                float u = (t - keys[i - 1][0]) / (keys[i][0] - keys[i - 1][0]);
                return Mth.lerp(smooth(u), keys[i - 1][1], keys[i][1]);
            }
        }
        return keys[keys.length - 1][1];
    }

    public record LocalPose(float x, float z, float yaw) {
    }

    public record SlashSpec(float start, float write, float eraseLead, float eraseEnd,
                            float life, float maxSpan, float angle, float direction, float arc,
                            float radius, float width, float y, float lift, float rotateX,
                            float rotateZ, float centerX, float centerZ, boolean dark) {
    }
}

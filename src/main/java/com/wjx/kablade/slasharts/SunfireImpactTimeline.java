package com.wjx.kablade.slasharts;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared 20 TPS choreography; impact is confirmed by a server ground collision. */
public final class SunfireImpactTimeline {
    public static final int WINDUP = 4, APEX = 14, DIVE = 18, EXPECTED_IMPACT = 22;
    public static final int RECOVERY = 4, AFTERGLOW = 20, BURN = 80;
    public static final double RADIUS = 4;
    private SunfireImpactTimeline() { }

    public static Vec3 position(Vec3 origin, Vec3 landing, double apexY, float age) {
        if (age <= WINDUP) return origin;
        if (age < APEX) {
            double t = Mth.clamp((age - WINDUP) / (APEX - WINDUP), 0, 1);
            double horizontal = 1 - (1 - t) * (1 - t);
            return new Vec3(Mth.lerp(horizontal, origin.x, landing.x),
                    Mth.lerp(Math.sin(t * Math.PI / 2), origin.y, apexY),
                    Mth.lerp(horizontal, origin.z, landing.z));
        }
        if (age <= DIVE) return new Vec3(landing.x, apexY, landing.z);
        if (age > EXPECTED_IMPACT) {
            // A floor removed during the leap must not leave the caster hovering at its old height.
            double extra = age - EXPECTED_IMPACT;
            return new Vec3(landing.x, landing.y - 0.08 - 1.6 * extra - 0.08 * extra * extra, landing.z);
        }
        double t = Mth.clamp((age - DIVE) / (EXPECTED_IMPACT - DIVE), 0, 1);
        return new Vec3(landing.x, Mth.lerp(t * t, apexY, landing.y - 0.08D), landing.z);
    }

    public static float smooth(float start, float end, float age) {
        float t = Mth.clamp((age - start) / (end - start), 0, 1);
        return t * t * (3 - 2 * t);
    }
}

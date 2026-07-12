package com.wjx.kablade.client.renderer;

import net.minecraft.util.Mth;

/** Local-space paths shared by every Bloodfyre Frenzy render layer. */
final class BloodfyreGeometry {
    static final float RING_SPAN = 240.0F * Mth.DEG_TO_RAD;
    static final float OPENING_CENTER = -40.0F * Mth.DEG_TO_RAD;
    static final float ARC_MIDDLE = OPENING_CENTER + Mth.PI;
    static final float ARC_START = ARC_MIDDLE - RING_SPAN * 0.5F;
    static final float ARC_END = ARC_MIDDLE + RING_SPAN * 0.5F;
    static final float TILT = 6.0F * Mth.DEG_TO_RAD;
    static final float WAIST_Y = 1.12F;

    private BloodfyreGeometry() {
    }

    static Point arc(float u, float radius, float yOffset) {
        float angle = Mth.lerp(u, ARC_START, ARC_END);
        float x = -Mth.sin(angle) * radius;
        float z = Mth.cos(angle) * radius;
        // Preserve the horizontal arc while making its left end lower and right end higher.
        return new Point(x, WAIST_Y + x * (float) Math.tan(TILT) + yOffset, z);
    }

    static Point groundArc(float u, float radius) {
        float angle = Mth.lerp(u, ARC_START, ARC_END);
        float torn = Mth.sin(u * 37.0F + 0.8F) * 0.075F
                + Mth.sin(u * 91.0F + 2.1F) * 0.034F
                + Mth.sin(u * 173.0F) * 0.014F;
        float actualRadius = radius + torn;
        return new Point(-Mth.sin(angle) * actualRadius, 0.0F, Mth.cos(angle) * actualRadius);
    }

    static Point tangent(float u, float radius) {
        float sample = 0.0025F;
        Point before = arc(Math.max(0.0F, u - sample), radius, 0.0F);
        Point after = arc(Math.min(1.0F, u + sample), radius, 0.0F);
        return after.subtract(before).normalize();
    }

    static Point radial(float u) {
        Point point = groundArc(u, 1.0F);
        return new Point(point.x, 0.0F, point.z).normalize();
    }

    static float deterministic(int index, float salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    record Point(float x, float y, float z) {
        Point add(Point other) {
            return new Point(this.x + other.x, this.y + other.y, this.z + other.z);
        }

        Point subtract(Point other) {
            return new Point(this.x - other.x, this.y - other.y, this.z - other.z);
        }

        Point scale(float amount) {
            return new Point(this.x * amount, this.y * amount, this.z * amount);
        }

        Point normalize() {
            float length = length();
            return length < 1.0E-5F ? new Point(0.0F, 0.0F, 0.0F) : scale(1.0F / length);
        }

        float length() {
            return Mth.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        }
    }
}

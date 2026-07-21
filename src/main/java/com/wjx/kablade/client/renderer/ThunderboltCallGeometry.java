package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Camera-aware ribbon, beam, billboard and deterministic lightning helpers. */
final class ThunderboltCallGeometry {

    @FunctionalInterface
    interface Curve {
        Vec3 point(float u);
    }

    private ThunderboltCallGeometry() {
    }

    static void ribbon(VertexConsumer out, Matrix4f matrix, Curve curve, int segments,
                       float head, float tail, float width, Vec3 camera,
                       int color, float alpha, float erode, long seed, float frame) {
        float start = Mth.clamp(tail, 0.0F, 1.0F);
        float end = Mth.clamp(head, 0.0F, 1.0F);
        if (alpha <= 0.001F || end <= start + 1.0E-4F) {
            return;
        }
        int count = Math.max(2, segments);
        for (int i = 0; i < count; i++) {
            float u0 = i / (float) count;
            float u1 = (i + 1) / (float) count;
            if (u1 < start || u0 > end) {
                continue;
            }
            float a = Math.max(start, u0);
            float b = Math.min(end, u1);
            float middle = (a + b) * 0.5F;
            float erosionNoise = hash01(seed + i * 0x9E3779B97F4A7C15L
                    + (long) (frame * 13.0F) * 0x632BE59BD9B4E019L);
            float edgeErosion = erode * (0.34F + 0.66F * middle);
            if (erosionNoise < edgeErosion * 0.72F) {
                continue;
            }
            Vec3 p0 = curve.point(a);
            Vec3 p1 = curve.point(b);
            float taper0 = (float) Math.pow(Math.max(0.0F, Math.sin(Math.PI * a)), 0.72D);
            float taper1 = (float) Math.pow(Math.max(0.0F, Math.sin(Math.PI * b)), 0.72D);
            float segmentAlpha = alpha * (1.0F - edgeErosion * 0.46F);
            beamQuad(out, matrix, p0, p1, width * (0.12F + 0.88F * taper0),
                    width * (0.12F + 0.88F * taper1), camera, color, segmentAlpha, a, b);
        }
    }

    static void beam(VertexConsumer out, Matrix4f matrix, Vec3 start, Vec3 end,
                     float width, Vec3 camera, int color, float alpha) {
        beamQuad(out, matrix, start, end, width, width, camera, color, alpha, 0.0F, 1.0F);
    }

    static void ring(VertexConsumer out, Matrix4f matrix, Vec3 center,
                     Vec3 axisA, Vec3 axisB, float radius, float width, int segments,
                     float rotation, Vec3 camera, int color, float alpha, long seed,
                     float jitter) {
        if (alpha <= 0.001F) {
            return;
        }
        Vec3 previous = ringPoint(center, axisA, axisB, radius, rotation, 0.0F,
                seed, jitter);
        for (int i = 1; i <= segments; i++) {
            float u = i / (float) segments;
            Vec3 current = ringPoint(center, axisA, axisB, radius, rotation, u, seed, jitter);
            beam(out, matrix, previous, current, width, camera, color, alpha);
            previous = current;
        }
    }

    static void lightning(VertexConsumer outer, VertexConsumer core, Matrix4f matrix,
                          Vec3 start, Vec3 end, int segments, float jitter, float width,
                          Vec3 camera, int outerColor, int coreColor, float alpha,
                          long seed) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D || alpha <= 0.001F) {
            return;
        }
        Vec3 dir = direction.normalize();
        Vec3 reference = Math.abs(dir.y) < 0.86D
                ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 sideA = dir.cross(reference).normalize();
        Vec3 sideB = dir.cross(sideA).normalize();
        Vec3 previous = start;
        int count = Math.max(3, segments);
        for (int i = 1; i <= count; i++) {
            float u = i / (float) count;
            float envelope = (float) Math.sin(Math.PI * u);
            double noiseA = (hash01(seed + i * 31L) - 0.5D) * 2.0D * jitter * envelope;
            double noiseB = (hash01(seed + i * 47L + 19L) - 0.5D) * jitter * envelope;
            Vec3 point = start.lerp(end, u)
                    .add(sideA.scale(noiseA))
                    .add(sideB.scale(noiseB));
            float flicker = 0.66F + hash01(seed + i * 73L) * 0.34F;
            beam(outer, matrix, previous, point, width, camera, outerColor, alpha * 0.58F * flicker);
            beam(core, matrix, previous, point, width * 0.34F, camera,
                    coreColor, alpha * flicker);
            previous = point;
        }
    }

    static void billboard(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera,
                          float halfWidth, float halfHeight, float rotation,
                          int color, float alpha) {
        if (alpha <= 0.001F) {
            return;
        }
        Vec3 view = camera.subtract(center);
        if (view.lengthSqr() < 1.0E-8D) {
            view = new Vec3(0.0D, 0.0D, 1.0D);
        }
        view = view.normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(view);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = view.cross(right).normalize();
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        Vec3 rotatedRight = right.scale(cos).add(up.scale(sin));
        Vec3 rotatedUp = up.scale(cos).subtract(right.scale(sin));
        Vec3 x = rotatedRight.scale(halfWidth);
        Vec3 y = rotatedUp.scale(halfHeight);
        quad(out, matrix, center.subtract(x).subtract(y), center.add(x).subtract(y),
                center.add(x).add(y), center.subtract(x).add(y), color, alpha,
                0.0F, 1.0F, 0.0F, 1.0F);
    }

    /** Camera-facing elliptical fan whose silhouette remains round on shader fallbacks. */
    static void discBillboard(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera,
                              float halfWidth, float halfHeight, float rotation,
                              int color, float alpha) {
        if (alpha <= 0.001F) {
            return;
        }
        Vec3 view = camera.subtract(center);
        if (view.lengthSqr() < 1.0E-8D) {
            view = new Vec3(0.0D, 0.0D, 1.0D);
        }
        view = view.normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(view);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = view.cross(right).normalize();
        double cosRotation = Math.cos(rotation);
        double sinRotation = Math.sin(rotation);
        Vec3 axisX = right.scale(cosRotation).add(up.scale(sinRotation));
        Vec3 axisY = up.scale(cosRotation).subtract(right.scale(sinRotation));

        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int a8 = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double angle0 = i * Mth.TWO_PI / segments;
            double angle1 = (i + 1) * Mth.TWO_PI / segments;
            Vec3 edge0 = center.add(axisX.scale(Math.cos(angle0) * halfWidth))
                    .add(axisY.scale(Math.sin(angle0) * halfHeight));
            Vec3 edge1 = center.add(axisX.scale(Math.cos(angle1) * halfWidth))
                    .add(axisY.scale(Math.sin(angle1) * halfHeight));
            float u0 = 0.5F + (float) Math.cos(angle0) * 0.5F;
            float v0 = 0.5F + (float) Math.sin(angle0) * 0.5F;
            float u1 = 0.5F + (float) Math.cos(angle1) * 0.5F;
            float v1 = 0.5F + (float) Math.sin(angle1) * 0.5F;

            // A QUADS RenderType turns this into one real and one degenerate triangle.
            vertex(out, matrix, center, red, green, blue, a8, 0.5F, 0.5F);
            vertex(out, matrix, edge0, red, green, blue, a8, u0, v0);
            vertex(out, matrix, edge1, red, green, blue, a8, u1, v1);
            vertex(out, matrix, center, red, green, blue, a8, 0.5F, 0.5F);
        }
    }

    static void starBurst(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera,
                          int rays, float minLength, float maxLength, float width,
                          int color, float alpha, long seed, float rotation) {
        Vec3 view = camera.subtract(center);
        if (view.lengthSqr() < 1.0E-8D) {
            view = new Vec3(0.0D, 0.0D, 1.0D);
        }
        view = view.normalize();
        Vec3 axisA = new Vec3(0.0D, 1.0D, 0.0D).cross(view);
        if (axisA.lengthSqr() < 1.0E-8D) {
            axisA = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            axisA = axisA.normalize();
        }
        Vec3 axisB = view.cross(axisA).normalize();
        for (int i = 0; i < rays; i++) {
            double angle = rotation + i * Mth.TWO_PI / rays
                    + (hash01(seed + i * 17L) - 0.5D) * 0.18D;
            float length = Mth.lerp(hash01(seed + i * 29L), minLength, maxLength);
            Vec3 direction = axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
            beam(out, matrix, center.add(direction.scale(0.08D)),
                    center.add(direction.scale(length)), width, camera, color,
                    alpha * (0.62F + hash01(seed + i * 43L) * 0.38F));
        }
    }

    static void humanoidAfterimage(VertexConsumer out, Matrix4f matrix, Vec3 feet,
                                   Vec3 camera, Vec3 right, Vec3 up, float scale,
                                   int color, float alpha) {
        Vec3 hip = feet.add(up.scale(0.78D * scale));
        Vec3 chest = feet.add(up.scale(1.48D * scale));
        Vec3 head = feet.add(up.scale(1.92D * scale));
        beam(out, matrix, hip, chest, 0.20F * scale, camera, color, alpha);
        billboard(out, matrix, head, camera, 0.22F * scale, 0.25F * scale,
                0.0F, color, alpha * 0.92F);
        beam(out, matrix, chest.add(right.scale(-0.12D * scale)),
                chest.add(right.scale(-0.58D * scale)).add(up.scale(-0.50D * scale)),
                0.075F * scale, camera, color, alpha * 0.82F);
        beam(out, matrix, chest.add(right.scale(0.12D * scale)),
                chest.add(right.scale(0.58D * scale)).add(up.scale(-0.44D * scale)),
                0.075F * scale, camera, color, alpha * 0.82F);
        beam(out, matrix, hip.add(right.scale(-0.09D * scale)),
                feet.add(right.scale(-0.34D * scale)), 0.085F * scale,
                camera, color, alpha * 0.78F);
        beam(out, matrix, hip.add(right.scale(0.09D * scale)),
                feet.add(right.scale(0.34D * scale)), 0.085F * scale,
                camera, color, alpha * 0.78F);
    }

    static float hash01(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdl;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53l;
        x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    private static Vec3 ringPoint(Vec3 center, Vec3 axisA, Vec3 axisB, float radius,
                                  float rotation, float u, long seed, float jitter) {
        double angle = rotation + u * Mth.TWO_PI;
        float noise = jitter == 0.0F ? 0.0F
                : ((hash01(seed + (long) (u * 4096.0F)) - 0.5F) * 2.0F
                + (float) Math.sin(angle * 7.0D + seed * 0.01D) * 0.45F) * jitter;
        double r = radius + noise;
        return center.add(axisA.scale(Math.cos(angle) * r))
                .add(axisB.scale(Math.sin(angle) * r));
    }

    private static void beamQuad(VertexConsumer out, Matrix4f matrix, Vec3 start, Vec3 end,
                                 float startWidth, float endWidth, Vec3 camera,
                                 int color, float alpha, float u0, float u1) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-10D || alpha <= 0.001F) {
            return;
        }
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = camera.subtract(midpoint);
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 1.0E-10D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-10D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 s0 = side.scale(startWidth);
        Vec3 s1 = side.scale(endWidth);
        quad(out, matrix, start.subtract(s0), start.add(s0), end.add(s1), end.subtract(s1),
                color, alpha, u0, u1, 0.0F, 1.0F);
    }

    private static void quad(VertexConsumer out, Matrix4f matrix,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             int color, float alpha,
                             float u0, float u1, float v0, float v1) {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int a8 = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        vertex(out, matrix, a, red, green, blue, a8, u0, v1);
        vertex(out, matrix, b, red, green, blue, a8, u0, v0);
        vertex(out, matrix, c, red, green, blue, a8, u1, v0);
        vertex(out, matrix, d, red, green, blue, a8, u1, v1);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 point,
                               int red, int green, int blue, int alpha, float u, float v) {
        out.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).uv(u, v).endVertex();
    }
}

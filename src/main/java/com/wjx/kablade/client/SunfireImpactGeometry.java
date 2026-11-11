package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.entity.SunfireImpactEntity;
import com.wjx.kablade.slasharts.SunfireImpactTimeline;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Camera-relative ribbons, conical flames and ground shock geometry; no spawned block debris. */
final class SunfireImpactGeometry {
    private SunfireImpactGeometry() { }
    private record GeometryFrame(Matrix4f transform, Vec3 origin) { }

    static void emit(RenderLevelStageEvent event, VertexConsumer out, SunfireImpactEntity cast) {
        float age = cast.getAge(event.getPartialTick());
        int impact = cast.getImpactAge();
        var pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(cast.getX() - camera.x, cast.getY() - camera.y, cast.getZ() - camera.z);
        GeometryFrame matrix = new GeometryFrame(pose.last().pose(), cast.position());
        Entity owner = cast.level().getEntity(cast.getOwnerId());
        if (owner != null && (impact < 0 || age < impact + SunfireImpactTimeline.RECOVERY)) {
            Vec3 foot = owner.getPosition(event.getPartialTick());
            Vec3 forward = cast.getLanding().subtract(cast.position()).multiply(1, 0, 1);
            if (forward.lengthSqr() < 0.001) forward = new Vec3(0, 0, 1);
            forward = forward.normalize();
            Vec3 sword = foot.add(forward.scale(0.65)).add(0, 0.85, 0);
            float charge = SunfireImpactTimeline.smooth(0, 4, age);
            if (age <= 5) {
                ring(out, matrix, cast.position().add(0, 0.04, 0), 0.3F + charge * 0.9F, 0.18F, 0.85F);
                flames(out, matrix, foot, 0.5F, 0.8F + charge, 0.8F, age, 7);
            }
            if (age > 4 && age < 19) {
                // Curved wake follows the real interpolated owner at its leading end.
                for (int lane = 0; lane < 3; lane++) {
                    Vec3 side = new Vec3(forward.z, 0, -forward.x).scale((lane - 1) * 0.22);
                    for (int i = 0; i < 14; i++) {
                        float t0 = i / 14F, t1 = (i + 1) / 14F;
                        float past0 = Math.max(4, age - (1 - t0) * 7);
                        float past1 = Math.max(4, age - (1 - t1) * 7);
                        Vec3 a = cast.plannedPosition(past0).add(0, 0.6, 0).add(side);
                        Vec3 b = i == 13 ? foot.add(0, 0.6, 0).add(side)
                                : cast.plannedPosition(past1).add(0, 0.6, 0).add(side);
                        ribbon(out, matrix, a, b, new Vec3(0, 0.20 + lane * 0.05, 0),
                                t0, t1, (0.2F + t0 * 0.65F) * (1 - SunfireImpactTimeline.smooth(14, 19, age)));
                    }
                }
            }
            if (impact < 0 && age >= 14 && age <= 18) {
                float gather = SunfireImpactTimeline.smooth(14, 18, age);
                flames(out, matrix, sword, 0.3F * (1 - gather * 0.4F), 1.7F, 0.9F, age, 8);
                ring(out, matrix, foot.add(0, 1, 0), 1.0F - gather * 0.6F, 0.10F, 0.7F);
            }
            if (impact < 0 && age >= 18) {
                float dive = Mth.clamp((age - 18) / 4, 0, 1);
                flames(out, matrix, foot.add(0, 0.12, 0), 0.6F + dive * 0.3F, 3.0F + dive * 2, 0.95F, age, 12);
                for (int i = 0; i < 9; i++) {
                    double angle = i * Math.PI * 2 / 9;
                    Vec3 side = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                    Vec3 a = foot.add(side.scale(0.8)).add(0, 0.4, 0);
                    ribbon(out, matrix, a, a.add(0, 3.5, 0), side.scale(0.025), 0, 1, 0.8F);
                }
            }
        }
        if (impact >= 0) {
            float t = age - impact;
            if (t >= 0 && t < 20) impact(out, matrix, cast.getImpactCenter(), t);
            for (int id : cast.getBurningIds()) {
                Entity victim = cast.level().getEntity(id);
                if (victim == null || !victim.isAlive()) continue;
                float fade = 1 - SunfireImpactTimeline.smooth(72, 80, t);
                float pulse = 0.78F + 0.22F * Mth.cos(t * 0.45F);
                flames(out, matrix, victim.getPosition(event.getPartialTick()).add(0, 0.08, 0),
                        Math.max(0.25F, victim.getBbWidth() * 0.55F),
                        Math.min(3, victim.getBbHeight() * 1.12F), fade * pulse * 0.65F, age + id, 8);
            }
        }
        pose.popPose();
    }

    private static void impact(VertexConsumer out, GeometryFrame matrix, Vec3 pos, float t) {
        float expand = 1 - (float) Math.pow(1 - Math.min(1, t / 12), 3);
        float fade = 1 - SunfireImpactTimeline.smooth(6, 20, t);
        ring(out, matrix, pos.add(0, 0.07, 0), 0.2F + expand * 3.8F, 0.35F * fade, fade);
        ring(out, matrix, pos.add(0, 0.16, 0), 0.1F + expand * 3.1F, 0.14F, fade * 0.7F);
        if (t < 10) {
            float burst = 1 - SunfireImpactTimeline.smooth(2, 10, t);
            flames(out, matrix, pos, 0.65F + t * 0.06F,
                    (1.8F + SunfireImpactTimeline.smooth(0, 3, t) * 4.5F) * burst, burst, t, 16);
            // Low hemispherical fire front, fully occluded by world depth.
            int rows = 8, segments = 48;
            for (int y = 0; y < rows; y++) {
                double p0 = y * Math.PI / (2 * rows), p1 = (y + 1) * Math.PI / (2 * rows);
                for (int i = 0; i < segments; i++) {
                    double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
                    Vec3 v0 = dome(pos, a, p0, expand * 4), v1 = dome(pos, b, p0, expand * 4);
                    Vec3 v2 = dome(pos, b, p1, expand * 4), v3 = dome(pos, a, p1, expand * 4);
                    quad(out, matrix, v0, v1, v2, v3, i / (float) segments, (i + 1) / (float) segments,
                            burst * 0.22F * (1 - y / (float) rows));
                }
            }
        }
        // Hot fissures and rising sparks are analytic, seeded by fixed spoke indices.
        for (int i = 0; i < 18; i++) {
            double angle = i * 2.39996;
            Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            Vec3 a = pos.add(dir.scale(0.4)).add(0, 0.035, 0);
            Vec3 b = pos.add(dir.scale((2.0 + (i % 4) * 0.5) * expand)).add(0, 0.035, 0);
            ribbon(out, matrix, a, b, side.scale(0.025), 0, 1, fade * 0.7F);
            float travel = t * (0.10F + i % 3 * 0.025F);
            Vec3 spark = pos.add(dir.scale(0.4 + travel)).add(0, Math.max(0.08, travel * 1.8 - t * t * 0.006), 0);
            ribbon(out, matrix, spark, spark.add(dir.scale(0.12)).add(0, 0.2, 0), side.scale(0.035), 0, 1, fade);
        }
    }

    private static Vec3 dome(Vec3 p, double angle, double elevation, double radius) {
        return p.add(Math.cos(angle) * Math.cos(elevation) * radius,
                0.05 + Math.sin(elevation) * radius * 0.32, Math.sin(angle) * Math.cos(elevation) * radius);
    }

    private static void flames(VertexConsumer out, GeometryFrame m, Vec3 base, float radius,
                               float height, float alpha, float time, int count) {
        if (height <= 0.01 || alpha <= 0.001) return;
        for (int lane = 0; lane < count; lane++) {
            double angle = lane * Math.PI * 2 / count;
            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 side = new Vec3(-radial.z, 0, radial.x);
            float phase = time * 0.34F + lane * 1.71F;
            float h = height * (0.72F + 0.28F * Mth.sin(phase));
            for (int s = 0; s < 10; s++) {
                float u = s / 10F, v = (s + 1) / 10F;
                Vec3 a = flamePoint(base, radial, side, radius, h, u, phase);
                Vec3 b = flamePoint(base, radial, side, radius, h, v, phase);
                float w0 = radius * 0.46F * (1 - u) + 0.005F;
                float w1 = radius * 0.46F * (1 - v) + 0.005F;
                quad(out, m, a.subtract(side.scale(w0)), a.add(side.scale(w0)),
                        b.add(side.scale(w1)), b.subtract(side.scale(w1)), u, v, alpha * (1 - u * 0.8F));
            }
        }
    }

    private static Vec3 flamePoint(Vec3 p, Vec3 radial, Vec3 side, float radius, float h, float t, float phase) {
        return p.add(radial.scale(radius * (1 - t * 0.75)))
                .add(side.scale(Mth.sin(t * 7 + phase) * radius * t * 0.35)).add(0, h * t, 0);
    }

    private static void ring(VertexConsumer out, GeometryFrame m, Vec3 center, float radius, float width, float alpha) {
        for (int i = 0; i < 96; i++) {
            double a = i * Math.PI * 2 / 96, b = (i + 1) * Math.PI * 2 / 96;
            Vec3 a0 = center.add(Math.cos(a) * (radius - width), 0, Math.sin(a) * (radius - width));
            Vec3 a1 = center.add(Math.cos(a) * (radius + width), 0, Math.sin(a) * (radius + width));
            Vec3 b0 = center.add(Math.cos(b) * (radius - width), 0, Math.sin(b) * (radius - width));
            Vec3 b1 = center.add(Math.cos(b) * (radius + width), 0, Math.sin(b) * (radius + width));
            quad(out, m, a0, a1, b1, b0, i / 96F, (i + 1) / 96F, alpha);
        }
    }

    private static void ribbon(VertexConsumer out, GeometryFrame m, Vec3 a, Vec3 b, Vec3 width,
                               float u0, float u1, float alpha) {
        quad(out, m, a.subtract(width), a.add(width), b.add(width), b.subtract(width), u0, u1, alpha);
    }

    private static void quad(VertexConsumer out, GeometryFrame m, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float u0, float u1, float alpha) {
        vertex(out, m, a, u0, 0, alpha); vertex(out, m, b, u0, 1, alpha);
        vertex(out, m, c, u1, 1, alpha); vertex(out, m, d, u1, 0, alpha);
    }
    private static void vertex(VertexConsumer out, GeometryFrame m, Vec3 p, float u, float v, float alpha) {
        // Subtract in double precision before emitting floats, including far from world origin.
        Vec3 local = p.subtract(m.origin());
        out.vertex(m.transform(), (float) local.x, (float) local.y, (float) local.z)
                .color(1F, 0.43F, 0.055F, Mth.clamp(alpha, 0, 1)).uv(u, v).endVertex();
    }
}

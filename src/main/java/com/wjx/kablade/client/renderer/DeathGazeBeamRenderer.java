package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.DeathGazeBeamEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 灼热重斩 (Death Gaze) 高能穿透激光与等离子粒子流渲染器。
 * <p>
 * 包含 8 层特效渲染：
 * 1. 剑尖发射源高能闪光火球与放射尖刺 (Muzzle Blast & Radial Spikes)
 * 2. 核心白炽强光激光柱 (White-Hot Core Beam)
 * 3. 内层高能灼热品红等离子鞘 (Inner Searing Magenta Sheath)
 * 4. 外层深紫崩坏星云光晕柱 (Outer Violet Nebula Glow)
 * 5. 双螺旋缠绕高速旋转能量光带 (Dual Helical Energy Ribbons)
 * 6. 向前高速穿梭膨胀脉冲环 (Forward Pulse Energy Rings)
 * 7. 终点爆破喷溅等离子盘与飞溅尖刺 (Impact Detonation Disc & Splash Spikes)
 * 8. 96 个沿光束轨迹螺旋飞舞的 3D 紫色等离子粒子 (Procedural 3D Plasma Sparks)
 */
public class DeathGazeBeamRenderer extends EntityRenderer<DeathGazeBeamEntity> {

    private static final int SPARK_COUNT = 96;

    public DeathGazeBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DeathGazeBeamEntity entity) {
        return KabladeRenderTypes.FALLBACK_TEXTURE;
    }

    @Override
    public void render(DeathGazeBeamEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.getAge() + partialTick;
        float maxLife = (float) DeathGazeBeamEntity.MAX_LIFETIME;
        if (age < 0.0F || age > maxLife) {
            return;
        }

        // Alpha envelope: fast ramp up, sustained blast, smooth fade out
        float fadeIn = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((maxLife - age) / 5.0F, 0.0F, 1.0F);
        float alpha = fadeIn * fadeOut;
        if (alpha <= 0.005F) {
            return;
        }

        float beamLength = entity.getBeamLength();
        if (beamLength <= 0.1F) {
            return;
        }

        poseStack.pushPose();

        // Rotate poseStack to align +Z with laser shooting direction
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        VertexConsumer consumer = buffer.getBuffer(KabladeRenderTypes.deathGazeBeam());
        Matrix4f mat = poseStack.last().pose();

        // 1. 剑尖发射源高能闪光火球与放射尖刺
        renderMuzzleFlare(consumer, mat, age, alpha);

        // 2. 核心白炽强光激光柱 (White-Hot Lavender Core)
        renderPulsingCylinder(consumer, mat, 0.14F, beamLength, 12, age,
                1.0F, 0.94F, 1.0F, alpha * 0.98F, 0.03F, 3.5F);

        // 3. 内层高能灼热品红等离子鞘 (Inner Magenta Sheath)
        renderPulsingCylinder(consumer, mat, 0.38F, beamLength, 16, age,
                0.96F, 0.16F, 0.88F, alpha * 0.85F, 0.05F, 2.8F);

        // 4. 外层深紫崩坏星云光晕柱 (Outer Violet Nebula Sheath)
        renderPulsingCylinder(consumer, mat, 0.85F, beamLength, 16, age,
                0.66F, 0.06F, 0.96F, alpha * 0.45F, 0.07F, 1.8F);

        // 5. 双螺旋缠绕高速旋转能量光带 (Dual Helical Ribbons)
        renderHelicalRibbons(consumer, mat, 0.52F, beamLength, age, alpha);

        // 6. 向前高速穿梭膨胀脉冲环 (Forward Pulse Energy Rings)
        renderPulseRings(consumer, mat, beamLength, age, alpha);

        // 7. 终点爆破喷溅等离子盘与飞溅尖刺 (Impact Burst)
        renderImpactBurst(consumer, mat, beamLength, age, alpha);

        // 8. 96 个沿光束轨迹螺旋飞舞的 3D 紫色等离子粒子 (Procedural Sparks)
        renderProceduralSparks(consumer, mat, beamLength, age, alpha);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /* -------------------------------------------------------------
       1. Muzzle Flare & Radial Spikes at Sword Emitter (z = 0)
       ------------------------------------------------------------- */
    private static void renderMuzzleFlare(VertexConsumer consumer, Matrix4f mat, float age, float alpha) {
        float discRadius = 0.55F * (0.9F + 0.1F * Mth.sin(age * 4.0F));
        int segments = 16;
        float z = 0.05F;

        // Central emitter glow disc
        for (int j = 0; j < segments; j++) {
            float th1 = ((float) j / segments) * Mth.TWO_PI;
            float th2 = ((float) (j + 1) / segments) * Mth.TWO_PI;

            consumer.vertex(mat, 0.0F, 0.0F, z).color(1.0F, 0.95F, 1.0F, alpha * 0.95F).endVertex();
            consumer.vertex(mat, Mth.cos(th1) * discRadius, Mth.sin(th1) * discRadius, z)
                    .color(0.9F, 0.2F, 0.95F, 0.0F).endVertex();
            consumer.vertex(mat, Mth.cos(th2) * discRadius, Mth.sin(th2) * discRadius, z)
                    .color(0.9F, 0.2F, 0.95F, 0.0F).endVertex();
            consumer.vertex(mat, 0.0F, 0.0F, z).color(1.0F, 0.95F, 1.0F, alpha * 0.95F).endVertex();
        }

        // 20 Radial Emitter Energy Spikes
        int spikeCount = 20;
        for (int s = 0; s < spikeCount; s++) {
            float angle = ((float) s / spikeCount) * Mth.TWO_PI + age * 1.5F;
            float len = 0.65F + 0.25F * Mth.sin(age * 5.0F + s * 1.3F);
            float width = 0.055F;

            float cosA = Mth.cos(angle);
            float sinA = Mth.sin(angle);
            float cosPerp = -sinA * width;
            float sinPerp = cosA * width;

            float tipX = cosA * len;
            float tipY = sinA * len;

            consumer.vertex(mat, -cosPerp, -sinPerp, z).color(1.0F, 0.85F, 1.0F, alpha * 0.90F).endVertex();
            consumer.vertex(mat, cosPerp, sinPerp, z).color(1.0F, 0.85F, 1.0F, alpha * 0.90F).endVertex();
            consumer.vertex(mat, tipX, tipY, z).color(0.85F, 0.15F, 0.95F, 0.0F).endVertex();
            consumer.vertex(mat, tipX, tipY, z).color(0.85F, 0.15F, 0.95F, 0.0F).endVertex();
        }
    }

    /* -------------------------------------------------------------
       2, 3, 4. Multi-Segment Pulsing Cylinder Generator
       ------------------------------------------------------------- */
    private static void renderPulsingCylinder(VertexConsumer consumer, Matrix4f mat,
                                              float baseRadius, float length, int radialSegs,
                                              float age, float r, float g, float b, float a,
                                              float pulseAmp, float pulseSpeed) {
        int zSlices = Math.max(1, (int) (length / 2.0F));
        float dz = length / zSlices;

        for (int k = 0; k < zSlices; k++) {
            float z1 = k * dz;
            float z2 = (k + 1) * dz;

            float r1 = baseRadius + pulseAmp * Mth.sin(age * pulseSpeed + z1 * 0.6F);
            float r2 = baseRadius + pulseAmp * Mth.sin(age * pulseSpeed + z2 * 0.6F);

            for (int j = 0; j < radialSegs; j++) {
                float th1 = ((float) j / radialSegs) * Mth.TWO_PI;
                float th2 = ((float) (j + 1) / radialSegs) * Mth.TWO_PI;

                float c1 = Mth.cos(th1); float s1 = Mth.sin(th1);
                float c2 = Mth.cos(th2); float s2 = Mth.sin(th2);

                consumer.vertex(mat, c1 * r1, s1 * r1, z1).color(r, g, b, a).endVertex();
                consumer.vertex(mat, c2 * r1, s2 * r1, z1).color(r, g, b, a).endVertex();
                consumer.vertex(mat, c2 * r2, s2 * r2, z2).color(r, g, b, a).endVertex();
                consumer.vertex(mat, c1 * r2, s1 * r2, z2).color(r, g, b, a).endVertex();
            }
        }
    }

    /* -------------------------------------------------------------
       5. Dual Helical Swirling Energy Ribbons
       ------------------------------------------------------------- */
    private static void renderHelicalRibbons(VertexConsumer consumer, Matrix4f mat,
                                            float helixRadius, float length, float age, float alpha) {
        float ribbonWidth = 0.10F;
        float dz = 0.45F;
        int steps = Math.max(2, (int) (length / dz));

        for (int h = 0; h < 2; h++) {
            float angleOffset = h * Mth.PI;

            for (int k = 0; k < steps; k++) {
                float z1 = k * dz;
                float z2 = (k + 1) * dz;

                float theta1 = age * 8.0F + z1 * 0.65F + angleOffset;
                float theta2 = age * 8.0F + z2 * 0.65F + angleOffset;

                float x1 = Mth.cos(theta1) * helixRadius;
                float y1 = Mth.sin(theta1) * helixRadius;
                float x2 = Mth.cos(theta2) * helixRadius;
                float y2 = Mth.sin(theta2) * helixRadius;

                // Perpendicular offset for ribbon thickness
                float px1 = -Mth.sin(theta1) * ribbonWidth;
                float py1 = Mth.cos(theta1) * ribbonWidth;
                float px2 = -Mth.sin(theta2) * ribbonWidth;
                float py2 = Mth.cos(theta2) * ribbonWidth;

                consumer.vertex(mat, x1 - px1, y1 - py1, z1).color(0.92F, 0.32F, 1.0F, alpha * 0.75F).endVertex();
                consumer.vertex(mat, x1 + px1, y1 + py1, z1).color(0.92F, 0.32F, 1.0F, alpha * 0.75F).endVertex();
                consumer.vertex(mat, x2 + px2, y2 + py2, z2).color(0.92F, 0.32F, 1.0F, alpha * 0.75F).endVertex();
                consumer.vertex(mat, x2 - px2, y2 - py2, z2).color(0.92F, 0.32F, 1.0F, alpha * 0.75F).endVertex();
            }
        }
    }

    /* -------------------------------------------------------------
       6. Forward-Traveling Pulse Energy Rings
       ------------------------------------------------------------- */
    private static void renderPulseRings(VertexConsumer consumer, Matrix4f mat,
                                         float length, float age, float alpha) {
        int ringCount = 4;
        int ringSegs = 16;
        float rInner = 0.40F;
        float rOuter = 0.95F;

        for (int r = 0; r < ringCount; r++) {
            float ringZ = ((age * 15.0F + r * 6.5F) % length);
            if (ringZ < 0.2F || ringZ > length - 0.2F) {
                continue;
            }

            for (int j = 0; j < ringSegs; j++) {
                float th1 = ((float) j / ringSegs) * Mth.TWO_PI;
                float th2 = ((float) (j + 1) / ringSegs) * Mth.TWO_PI;

                float c1 = Mth.cos(th1); float s1 = Mth.sin(th1);
                float c2 = Mth.cos(th2); float s2 = Mth.sin(th2);

                consumer.vertex(mat, c1 * rInner, s1 * rInner, ringZ)
                        .color(0.98F, 0.45F, 1.0F, alpha * 0.70F).endVertex();
                consumer.vertex(mat, c2 * rInner, s2 * rInner, ringZ)
                        .color(0.98F, 0.45F, 1.0F, alpha * 0.70F).endVertex();
                consumer.vertex(mat, c2 * rOuter, s2 * rOuter, ringZ)
                        .color(0.70F, 0.10F, 0.95F, 0.0F).endVertex();
                consumer.vertex(mat, c1 * rOuter, s1 * rOuter, ringZ)
                        .color(0.70F, 0.10F, 0.95F, 0.0F).endVertex();
            }
        }
    }

    /* -------------------------------------------------------------
       7. Impact Burst: Splash Disc & Radial Spikes at Beam End
       ------------------------------------------------------------- */
    private static void renderImpactBurst(VertexConsumer consumer, Matrix4f mat,
                                          float length, float age, float alpha) {
        float z = length - 0.05F;
        float burstRadius = 1.35F * (0.90F + 0.10F * Mth.sin(age * 5.0F));
        int segments = 16;

        // Circular impact shock disc
        for (int j = 0; j < segments; j++) {
            float th1 = ((float) j / segments) * Mth.TWO_PI;
            float th2 = ((float) (j + 1) / segments) * Mth.TWO_PI;

            consumer.vertex(mat, 0.0F, 0.0F, z).color(1.0F, 0.90F, 1.0F, alpha * 0.90F).endVertex();
            consumer.vertex(mat, Mth.cos(th1) * burstRadius, Mth.sin(th1) * burstRadius, z)
                    .color(0.95F, 0.25F, 0.90F, 0.0F).endVertex();
            consumer.vertex(mat, Mth.cos(th2) * burstRadius, Mth.sin(th2) * burstRadius, z)
                    .color(0.95F, 0.25F, 0.90F, 0.0F).endVertex();
            consumer.vertex(mat, 0.0F, 0.0F, z).color(1.0F, 0.90F, 1.0F, alpha * 0.90F).endVertex();
        }

        // 24 Radial Impact Splash Spikes
        int spikeCount = 24;
        for (int s = 0; s < spikeCount; s++) {
            float angle = ((float) s / spikeCount) * Mth.TWO_PI + age * 2.5F;
            float spikeLen = 1.15F + 0.45F * Mth.sin(age * 6.0F + s * 1.5F);
            float width = 0.065F;

            float cosA = Mth.cos(angle);
            float sinA = Mth.sin(angle);
            float cosP = -sinA * width;
            float sinP = cosA * width;

            float tipX = cosA * spikeLen;
            float tipY = sinA * spikeLen;

            consumer.vertex(mat, -cosP, -sinP, z).color(1.0F, 0.70F, 1.0F, alpha * 0.85F).endVertex();
            consumer.vertex(mat, cosP, sinP, z).color(1.0F, 0.70F, 1.0F, alpha * 0.85F).endVertex();
            consumer.vertex(mat, tipX, tipY, z).color(0.80F, 0.10F, 0.90F, 0.0F).endVertex();
            consumer.vertex(mat, tipX, tipY, z).color(0.80F, 0.10F, 0.90F, 0.0F).endVertex();
        }
    }

    /* -------------------------------------------------------------
       8. Procedural 3D Plasma Sparks & Particles Orbiting Beam
       ------------------------------------------------------------- */
    private static void renderProceduralSparks(VertexConsumer consumer, Matrix4f mat,
                                               float length, float age, float alpha) {
        for (int i = 0; i < SPARK_COUNT; i++) {
            float seed = i * 23.456F;
            float z = ((seed * 47.0F + age * 18.0F) % length);
            float orbitSpeed = 4.5F + (seed % 4.0F) * 1.8F;
            float angle = seed * 6.28F + age * orbitSpeed + z * 0.4F;
            float r = 0.22F + (Mth.sin(seed * 5.7F) * 0.5F + 0.5F) * 0.80F;

            float x = Mth.cos(angle) * r;
            float y = Mth.sin(angle) * r;
            float sz = (i % 2 == 0) ? 0.055F : 0.038F;

            boolean isCoreSpark = (i % 3 == 0);
            float cr = isCoreSpark ? 1.0F : 0.88F;
            float cg = isCoreSpark ? 0.90F : 0.22F;
            float cb = 1.0F;
            float ca = alpha * (isCoreSpark ? 0.95F : 0.75F);

            consumer.vertex(mat, x - sz, y - sz, z).color(cr, cg, cb, ca).endVertex();
            consumer.vertex(mat, x + sz, y - sz, z).color(cr, cg, cb, ca).endVertex();
            consumer.vertex(mat, x + sz, y + sz, z).color(cr, cg, cb, ca).endVertex();
            consumer.vertex(mat, x - sz, y + sz, z).color(cr, cg, cb, ca).endVertex();
        }
    }
}

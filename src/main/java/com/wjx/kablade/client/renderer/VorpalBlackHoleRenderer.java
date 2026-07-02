package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusSkillRenderer;
import com.wjx.kablade.entity.VorpalBlackHoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * World-space red-black vortex disk for Vorpal Hole.
 */
public final class VorpalBlackHoleRenderer extends EntityRenderer<VorpalBlackHoleEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");
    private static final int STREAK_COUNT = 28;
    private static final int FIBER_COUNT = 52;
    private static final int RING_SEGMENTS = 96;

    public VorpalBlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(VorpalBlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (OculusSkillRenderer.runIfNeeded(immediate ->
                render(entity, entityYaw, partialTick, poseStack, immediate, packedLight))) {
            return;
        }

        float age = entity.tickCount + partialTick;
        float activeEnd = Math.max(1.0F, entity.getLifetime());
        float visualEnd = Math.max(activeEnd, entity.getVisualLifetime());
        float intro = smootherStep(Mth.clamp(age / 13.0F, 0.0F, 1.0F));
        float outro = smootherStep(Mth.clamp((activeEnd - age) / 15.0F, 0.0F, 1.0F));
        float alpha = intro * outro;
        float finale = finalePresence(age, activeEnd, visualEnd);
        float finaleAlpha = intro * finale;
        float impact = impactPresence(age, activeEnd, visualEnd);
        float impactAlpha = intro * impact;
        if (alpha <= 0.004F && finaleAlpha <= 0.004F && impactAlpha <= 0.004F) {
            return;
        }

        float burst = burstPresence(age) * outro;
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.38F);
        float exitShrink = Mth.lerp(outro, 0.72F, 1.0F);
        float radius = Mth.lerp(intro, 0.48F, 2.75F)
                * (0.97F + pulse * 0.035F + burst * 0.13F + finale * 0.08F)
                * exitShrink;
        float flow = Mth.sin(age * 0.115F) * 18.0F + Mth.sin(age * 0.047F + 1.7F) * 11.0F;
        float roll = age * 11.0F + flow + burst * 42.0F + finale * 84.0F;

        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.vorpalBlackHole());
        poseStack.pushPose();
        // Local XY is the vortex plane; local +Z is its normal. Keep the disk
        // facing the camera while it still lives at the entity position/depth.
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        Matrix4f mat = poseStack.last().pose();

        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            renderOculusFallback(vc, mat, age, activeEnd, visualEnd, alpha, burst, finale, impact, radius);
            poseStack.popPose();
            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        vortexQuad(vc, mat, radius * 1.42F,
                0.40F, 0.005F, 0.004F, alpha * 0.34F, -0.004F);
        vortexQuad(vc, mat, radius * 1.16F,
                0.74F, 0.014F, 0.010F, alpha * 0.55F, 0.0F);
        vortexQuad(vc, mat, radius,
                0.95F, 0.035F, 0.018F, alpha * 0.96F, 0.0F);

        if (burst > 0.02F) {
            radialStreaks(vc, mat, age, burst, radius, alpha);
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(-roll * 0.62F));
            Matrix4f burstMat = poseStack.last().pose();
            slashBand(vc, burstMat, age, alpha * burst, 0.92F, 1.0F, 0.05F, 0.035F);
            slashBand(vc, burstMat, age + 4.5F, alpha * burst * 0.72F, -0.68F, 1.0F, 0.14F, 0.08F);
            poseStack.popPose();
        }
        poseStack.popPose();

        if (finale > 0.015F) {
            Matrix4f finaleMat = poseStack.last().pose();
            coarseFibers(vc, finaleMat, age, activeEnd, visualEnd, radius, finaleAlpha);
            finaleRings(vc, finaleMat, age, activeEnd, visualEnd, finale, radius, finaleAlpha);
            if (impact > 0.012F) {
                impactShock(vc, finaleMat, age, activeEnd, visualEnd, impact, radius, impactAlpha);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderOculusFallback(VertexConsumer vc, Matrix4f mat, float age,
                                             float activeEnd, float visualEnd,
                                             float alpha, float burst, float finale, float impact,
                                             float radius) {
        float pulse = 0.74F + 0.26F * Mth.sin(age * 0.30F);

        colorDisc(vc, mat, radius * 0.52F,
                0.0F, 0.0F, 0.0F, alpha * 0.88F);
        colorRingBand(vc, mat, radius * 0.30F, radius * 0.58F, 0.003F,
                0.16F, 0.0F, 0.0F, alpha * 0.56F);
        colorRingBand(vc, mat, radius * 0.52F, radius * 0.70F, 0.007F,
                1.0F, 0.030F, 0.012F, alpha * (0.62F + pulse * 0.32F));
        colorRingBand(vc, mat, radius * 0.72F, radius * 1.08F, 0.002F,
                0.36F, 0.0F, 0.0F, alpha * 0.32F);
        colorRingBand(vc, mat, radius * 0.96F, radius * 1.12F, 0.010F,
                0.95F, 0.020F, 0.010F, alpha * 0.34F);

        for (int i = 0; i < 6; i++) {
            float angle = i * Mth.TWO_PI / 3.0F + age * 0.060F + (i % 2) * 0.72F;
            float armAlpha = alpha * (0.48F + deterministic(i, 2.7F) * 0.22F);
            spiralArm(vc, mat, angle, radius * 0.24F, radius * (1.14F + deterministic(i, 4.1F) * 0.18F),
                    radius * (0.070F + deterministic(i, 5.2F) * 0.030F),
                    1.0F, 0.045F + deterministic(i, 6.3F) * 0.035F, 0.018F,
                    armAlpha);
        }
        colorDisc(vc, mat, radius * 0.28F,
                0.0F, 0.0F, 0.0F, alpha * 0.96F);

        if (burst > 0.02F) {
            for (int i = 0; i < 24; i++) {
                float angle = i * Mth.TWO_PI / 18.0F + (deterministic(i, 7.2F) - 0.5F) * 0.24F + age * 0.018F;
                float inner = radius * (0.34F + deterministic(i, 8.1F) * 0.20F);
                float outer = radius * (1.24F + deterministic(i, 9.4F) * 0.66F);
                float width = radius * (0.010F + deterministic(i, 10.6F) * 0.026F) * (1.0F + burst * 0.6F);
                colorRadialRibbon(vc, mat, angle, inner, outer, width, 0.010F,
                        1.0F, 0.060F, 0.025F, alpha * burst * (0.16F + deterministic(i, 11.8F) * 0.34F));
            }
            colorSlashBand(vc, mat, age, radius, alpha * burst * 0.45F, 0.92F,
                    1.0F, 0.060F, 0.030F);
            colorSlashBand(vc, mat, age + 4.5F, radius, alpha * burst * 0.32F, -0.68F,
                    1.0F, 0.14F, 0.08F);
        }

        if (finale > 0.015F) {
            float origin = activeEnd - 17.0F;
            for (int i = 0; i < 5; i++) {
                float progress = smootherStep(Mth.clamp((age - origin - i * 1.05F) / 12.0F, 0.0F, 1.0F));
                if (progress > 0.001F && progress < 0.995F) {
                    float peak = Mth.sin(progress * Mth.PI);
                    float ringRadius = radius * (0.36F + progress * (1.80F + i * 0.20F));
                    colorRingBand(vc, mat, ringRadius - radius * 0.030F, ringRadius + radius * 0.030F, 0.070F + i * 0.006F,
                            1.0F, 0.035F + i * 0.014F, 0.014F,
                            alpha * finale * (0.18F + peak * 0.36F));
                }
            }
            float fiberStart = activeEnd - 11.0F;
            for (int i = 0; i < 34; i++) {
                float raw = Mth.clamp((age - fiberStart - deterministic(i, 16.2F) * 2.2F) / 20.0F, 0.0F, 1.0F);
                if (raw <= 0.001F || raw >= 0.995F) {
                    continue;
                }
                float launch = easeOutQuint(Mth.clamp(raw / 0.30F, 0.0F, 1.0F));
                float fade = 1.0F - smootherStep(Mth.clamp(raw * 0.92F, 0.0F, 1.0F));
                float angle = deterministic(i, 17.6F) * Mth.TWO_PI;
                float travel = radius * (0.28F + launch * (2.35F + deterministic(i, 18.4F) * 1.55F));
                float length = radius * (0.56F + deterministic(i, 19.7F) * 1.20F);
                float inner = Math.max(radius * 0.10F, travel - length);
                float outer = travel + length * 0.32F;
                float width = radius * (0.022F + deterministic(i, 20.5F) * 0.070F);
                colorRadialRibbon(vc, mat, angle, inner, outer, width, 0.105F,
                        1.0F, 0.034F, 0.014F, alpha * finale * fade * 0.52F);
            }
        }

        if (impact > 0.012F) {
            float shock = Mth.sin(Mth.clamp(impact, 0.0F, 1.0F) * Mth.PI);
            float ringRadius = radius * (0.82F + impact * 1.75F);
            colorRingBand(vc, mat, ringRadius - radius * 0.035F, ringRadius + radius * 0.035F, 0.112F,
                    1.0F, 0.070F, 0.030F, alpha * impact * (0.24F + shock * 0.32F));
        }
    }

    private static void colorDisc(VertexConsumer vc, Matrix4f mat, float radius,
                                  float r, float g, float b, float alpha) {
        colorRingBand(vc, mat, 0.01F, radius * 0.45F, 0.0F, r, g, b, alpha * 0.72F);
        colorRingBand(vc, mat, radius * 0.45F, radius, 0.001F, r, g, b, alpha);
    }

    private static void colorRingBand(VertexConsumer vc, Matrix4f mat, float inner, float outer, float z,
                                      float r, float g, float b, float alpha) {
        int segments = RING_SEGMENTS;
        for (int i = 0; i < segments; i++) {
            float a0 = i / (float) segments * Mth.TWO_PI;
            float a1 = (i + 1) / (float) segments * Mth.TWO_PI;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            colorQuad(vc, mat,
                    c0 * inner, s0 * inner, z,
                    c1 * inner, s1 * inner, z,
                    c1 * outer, s1 * outer, z,
                    c0 * outer, s0 * outer, z,
                    r, g, b, alpha);
        }
    }

    private static void spiralArm(VertexConsumer vc, Matrix4f mat, float baseAngle,
                                  float inner, float outer, float width,
                                  float r, float g, float b, float alpha) {
        int segments = 14;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float radius0 = Mth.lerp(t0, inner, outer);
            float radius1 = Mth.lerp(t1, inner, outer);
            float angle0 = baseAngle + t0 * 1.72F;
            float angle1 = baseAngle + t1 * 1.72F;
            float alphaMid = alpha * smootherStep(Mth.clamp(t0 / 0.22F, 0.0F, 1.0F))
                    * (1.0F - smootherStep(Mth.clamp((t0 - 0.76F) / 0.24F, 0.0F, 1.0F)));
            colorRibbonSegment(vc, mat, angle0, radius0, angle1, radius1,
                    width * (0.72F + t0 * 0.58F), 0.012F, r, g, b, alphaMid);
        }
    }

    private static void colorRadialRibbon(VertexConsumer vc, Matrix4f mat, float angle,
                                          float inner, float outer, float width, float z,
                                          float r, float g, float b, float alpha) {
        colorRibbonSegment(vc, mat, angle, inner, angle, outer, width, z, r, g, b, alpha);
    }

    private static void colorRibbonSegment(VertexConsumer vc, Matrix4f mat,
                                           float angle0, float radius0, float angle1, float radius1,
                                           float width, float z,
                                           float r, float g, float b, float alpha) {
        float x0 = Mth.cos(angle0) * radius0;
        float y0 = Mth.sin(angle0) * radius0;
        float x1 = Mth.cos(angle1) * radius1;
        float y1 = Mth.sin(angle1) * radius1;
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001F) {
            return;
        }
        float px = -dy / len * width;
        float py = dx / len * width;
        colorQuad(vc, mat,
                x0 + px, y0 + py, z,
                x1 + px, y1 + py, z,
                x1 - px, y1 - py, z,
                x0 - px, y0 - py, z,
                r, g, b, alpha);
    }

    private static void colorSlashBand(VertexConsumer vc, Matrix4f mat, float age, float radius,
                                       float alpha, float angle, float r, float g, float b) {
        float local = 1.0F - Math.abs(age - 29.0F) / 7.5F;
        float presence = smootherStep(Mth.clamp(local, 0.0F, 1.0F));
        if (presence <= 0.01F) {
            return;
        }
        float length = radius * 1.95F;
        float width = radius * (0.045F + presence * 0.035F);
        float dx = Mth.cos(angle);
        float dy = Mth.sin(angle);
        float px = -dy * width;
        float py = dx * width;
        float hx = dx * length * 0.5F;
        float hy = dy * length * 0.5F;
        colorQuad(vc, mat,
                -hx + px, -hy + py, 0.016F,
                hx + px, hy + py, 0.016F,
                hx - px, hy - py, 0.016F,
                -hx - px, -hy - py, 0.016F,
                r, g, b, alpha * presence);
    }

    private static void colorQuad(VertexConsumer vc, Matrix4f mat,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float r, float g, float b, float alpha) {
        colorVertex(vc, mat, x0, y0, z0, r, g, b, alpha);
        colorVertex(vc, mat, x1, y1, z1, r, g, b, alpha);
        colorVertex(vc, mat, x2, y2, z2, r, g, b, alpha);
        colorVertex(vc, mat, x3, y3, z3, r, g, b, alpha);
    }

    private static void colorVertex(VertexConsumer vc, Matrix4f mat,
                                    float x, float y, float z,
                                    float r, float g, float b, float alpha) {
        vc.vertex(mat, x, y, z).color(r, g, b, alpha).endVertex();
    }

    private static float burstPresence(float age) {
        float in = smootherStep(Mth.clamp((age - 18.0F) / 10.0F, 0.0F, 1.0F));
        float out = smootherStep(Mth.clamp((43.0F - age) / 11.0F, 0.0F, 1.0F));
        return in * out;
    }

    private static float finalePresence(float age, float activeEnd, float visualEnd) {
        float start = activeEnd - 18.0F;
        float in = smootherStep(Mth.clamp((age - start) / 4.5F, 0.0F, 1.0F));
        float out = 1.0F - smootherStep(Mth.clamp((age - (visualEnd - 5.0F)) / 5.0F, 0.0F, 1.0F));
        return in * out;
    }

    private static float impactPresence(float age, float activeEnd, float visualEnd) {
        float start = activeEnd - 11.0F;
        float local = Mth.clamp((age - start) / 12.0F, 0.0F, 1.0F);
        float attack = smootherStep(Mth.clamp(local / 0.12F, 0.0F, 1.0F));
        float release = 1.0F - smootherStep(Mth.clamp((local - 0.36F) / 0.64F, 0.0F, 1.0F));
        return attack * release;
    }

    private static void radialStreaks(VertexConsumer vc, Matrix4f mat, float age, float burst,
                                      float radius, float alpha) {
        for (int i = 0; i < STREAK_COUNT; i++) {
            float f = i / (float) STREAK_COUNT;
            float jitter = deterministic(i, 0.37F) - 0.5F;
            float angle = f * Mth.TWO_PI + jitter * 0.34F + age * 0.018F;
            float lengthMul = 1.25F + deterministic(i, 1.91F) * 1.15F;
            float inner = radius * (0.36F + deterministic(i, 3.17F) * 0.18F);
            float outer = radius * lengthMul;
            float width = radius * (0.018F + deterministic(i, 5.43F) * 0.034F) * (1.0F + burst * 0.9F);
            float warm = deterministic(i, 7.29F);
            float r = warm > 0.42F ? 1.0F : 0.05F;
            float g = warm > 0.42F ? 0.025F + warm * 0.075F : 0.0F;
            float b = warm > 0.42F ? 0.015F : 0.0F;
            float a = alpha * burst * (0.20F + deterministic(i, 9.61F) * 0.58F);
            radialRibbon(vc, mat, angle, inner, outer, width, r, g, b, a);
        }
    }

    private static void finaleRings(VertexConsumer vc, Matrix4f mat, float age, float activeEnd, float visualEnd,
                                    float finale, float radius, float alpha) {
        float origin = activeEnd - 17.0F;
        for (int i = 0; i < 7; i++) {
            float progress = smootherStep(Mth.clamp((age - origin - i * 1.05F) / 12.0F, 0.0F, 1.0F));
            if (progress <= 0.001F || progress >= 0.995F) {
                continue;
            }

            float peak = Mth.sin(progress * Mth.PI);
            float broken = 0.86F + deterministic(i, 11.4F) * 0.18F;
            float ringRadius = radius * (0.14F + progress * (1.18F + i * 0.18F));
            float width = radius * (0.045F + i * 0.010F) * (1.0F - progress * 0.18F);
            float ringAlpha = alpha * (0.16F + peak * 0.42F) * broken * (0.82F + i * 0.055F);
            float red = 0.88F + i * 0.030F;
            float green = 0.018F + i * 0.012F;
            ring(vc, mat, ringRadius, width, 0.030F + i * 0.006F, red, green, 0.012F, ringAlpha);
        }

    }

    private static void impactShock(VertexConsumer vc, Matrix4f mat, float age, float activeEnd, float visualEnd,
                                    float impact, float radius, float alpha) {
        float progress = smootherStep(Mth.clamp((age - (activeEnd - 11.0F)) / 8.0F, 0.0F, 1.0F));
        float flash = Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * Mth.PI);
        flashQuad(vc, mat, radius * (0.34F + progress * 0.58F), 0.090F,
                1.0F, 0.060F, 0.025F, alpha * (1.20F + flash * 1.60F));

        for (int i = 0; i < 3; i++) {
            float p = smootherStep(Mth.clamp((age - (activeEnd - 11.0F) - i * 1.4F) / 8.5F, 0.0F, 1.0F));
            if (p <= 0.001F || p >= 0.995F) {
                continue;
            }
            float peak = Mth.sin(p * Mth.PI);
            float shockRadius = radius * (0.42F + p * (1.95F + i * 0.42F));
            float shockWidth = radius * (0.042F + peak * 0.038F);
            ring(vc, mat, shockRadius, shockWidth, 0.076F + i * 0.012F,
                    1.0F, 0.050F + i * 0.018F, 0.014F,
                    alpha * (0.30F + peak * 0.56F));
        }
    }

    private static void coarseFibers(VertexConsumer vc, Matrix4f mat, float age,
                                     float activeEnd, float visualEnd, float radius, float alpha) {
        float start = activeEnd - 11.0F;
        float duration = 20.0F;
        for (int i = 0; i < FIBER_COUNT; i++) {
            float layer = i % 3;
            float delay = deterministic(i, 33.1F) * 2.2F + layer * 0.45F;
            float raw = Mth.clamp((age - start - delay) / duration, 0.0F, 1.0F);
            if (raw <= 0.001F || raw >= 0.995F) {
                continue;
            }

            float launch = easeOutQuint(Mth.clamp(raw / 0.30F, 0.0F, 1.0F));
            float eraseTravel = Mth.clamp((raw - 0.42F) / 0.58F, 0.0F, 1.0F);
            float blast = Mth.clamp(launch * 0.76F + eraseTravel * 0.24F, 0.0F, 1.0F);
            float erase = smootherStep(Mth.clamp((raw - 0.42F) / 0.58F, 0.0F, 1.0F));
            float fade = 1.0F - smootherStep(Mth.clamp(raw * 0.92F, 0.0F, 1.0F));
            float seed = deterministic(i, 35.7F);
            float cluster = (float) Math.floor(seed * 8.0F) / 8.0F;
            float angle = (cluster + (deterministic(i, 37.3F) - 0.5F) * 0.12F) * Mth.TWO_PI;
            float layerScale = 0.82F + layer * 0.18F;
            float travel = radius * (0.22F
                    + blast * (3.78F + deterministic(i, 39.6F) * 2.22F)) * layerScale;
            float length = radius * (0.80F + deterministic(i, 41.2F) * 1.75F) * (0.72F + blast * 0.42F);
            float tail = Math.max(radius * 0.02F, travel - length);
            float head = travel + length * 0.36F;
            float inner = Mth.lerp(erase, tail, head - radius * 0.035F);
            float outer = head;
            if (outer <= inner + 0.04F) {
                continue;
            }

            float width = radius * (0.060F + deterministic(i, 43.8F) * 0.150F)
                    * (1.10F + (2.0F - layer) * 0.18F)
                    * (1.0F - erase * 0.42F);
            float a = alpha * fade * (0.72F + deterministic(i, 45.9F) * 0.80F)
                    * (1.0F - layer * 0.16F);
            float red = deterministic(i, 47.4F) > 0.22F ? 1.0F : 0.045F;
            float green = red > 0.1F ? 0.025F + deterministic(i, 49.2F) * 0.095F : 0.0F;
            float blue = red > 0.1F ? 0.015F : 0.0F;
            fiberRibbon(vc, mat, angle, inner, outer, width, 0.108F + layer * 0.016F,
                    red, green, blue, a);
        }
    }

    private static float deterministic(int index, float salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float easeOutQuint(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv * inv * inv;
    }

    private static void vortexQuad(VertexConsumer vc, Matrix4f mat, float radius,
                                   float r, float g, float b, float alpha, float z) {
        quad(vc, mat,
                -radius, -radius, z, 0.0F, 1.0F,
                radius, -radius, z, 1.0F, 1.0F,
                radius, radius, z, 1.0F, 0.0F,
                -radius, radius, z, 0.0F, 0.0F,
                r, g, b, alpha);
    }

    private static void flashQuad(VertexConsumer vc, Matrix4f mat, float radius, float z,
                                  float r, float g, float b, float alpha) {
        quad(vc, mat,
                -radius, -radius, z, 7.0F, 1.0F,
                radius, -radius, z, 8.0F, 1.0F,
                radius, radius, z, 8.0F, 0.0F,
                -radius, radius, z, 7.0F, 0.0F,
                r, g, b, alpha);
    }

    private static void slashBand(VertexConsumer vc, Matrix4f mat, float age, float alpha,
                                  float angle, float r, float g, float b) {
        float local = 1.0F - Math.abs(age - 29.0F) / 7.5F;
        float presence = smootherStep(Mth.clamp(local, 0.0F, 1.0F));
        if (presence <= 0.01F) {
            return;
        }

        float length = 5.4F;
        float width = 0.18F + presence * 0.08F;
        float dx = Mth.cos(angle);
        float dy = Mth.sin(angle);
        float px = -dy * width;
        float py = dx * width;
        float hx = dx * length * 0.5F;
        float hy = dy * length * 0.5F;
        quad(vc, mat,
                -hx + px, -hy + py, 0.015F, 1.0F, 0.0F,
                hx + px, hy + py, 0.015F, 2.0F, 0.0F,
                hx - px, hy - py, 0.015F, 2.0F, 1.0F,
                -hx - px, -hy - py, 0.015F, 1.0F, 1.0F,
                r, g, b, alpha * presence);
    }

    private static void radialRibbon(VertexConsumer vc, Matrix4f mat, float angle,
                                     float inner, float outer, float width,
                                     float r, float g, float b, float alpha) {
        float dx = Mth.cos(angle);
        float dy = Mth.sin(angle);
        float px = -dy * width;
        float py = dx * width;
        quad(vc, mat,
                dx * inner + px, dy * inner + py, 0.01F, 3.0F, 0.0F,
                dx * outer + px, dy * outer + py, 0.01F, 4.0F, 0.0F,
                dx * outer - px, dy * outer - py, 0.01F, 4.0F, 1.0F,
                dx * inner - px, dy * inner - py, 0.01F, 3.0F, 1.0F,
                r, g, b, alpha);
    }

    private static void fiberRibbon(VertexConsumer vc, Matrix4f mat, float angle,
                                    float inner, float outer, float width, float z,
                                    float r, float g, float b, float alpha) {
        float dx = Mth.cos(angle);
        float dy = Mth.sin(angle);
        float px = -dy * width;
        float py = dx * width;
        quad(vc, mat,
                dx * inner + px * 0.42F, dy * inner + py * 0.42F, z, 9.0F, 0.0F,
                dx * outer + px, dy * outer + py, z, 10.0F, 0.0F,
                dx * outer - px, dy * outer - py, z, 10.0F, 1.0F,
                dx * inner - px * 0.42F, dy * inner - py * 0.42F, z, 9.0F, 1.0F,
                r, g, b, alpha);
    }

    private static void ring(VertexConsumer vc, Matrix4f mat, float radius, float width, float z,
                             float r, float g, float b, float alpha) {
        float inner = Math.max(0.01F, radius - width);
        float outer = radius + width;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a0 = i / (float) RING_SEGMENTS * Mth.TWO_PI;
            float a1 = (i + 1) / (float) RING_SEGMENTS * Mth.TWO_PI;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            float u0 = 5.0F + i / (float) RING_SEGMENTS;
            float u1 = 5.0F + (i + 1) / (float) RING_SEGMENTS;
            quad(vc, mat,
                    c0 * inner, s0 * inner, z, u0, 0.0F,
                    c1 * inner, s1 * inner, z, u1, 0.0F,
                    c1 * outer, s1 * outer, z, u1, 1.0F,
                    c0 * outer, s0 * outer, z, u0, 1.0F,
                    r, g, b, alpha);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float r, float g, float b, float alpha) {
        vertex(vc, mat, x0, y0, z0, u0, v0, r, g, b, alpha);
        vertex(vc, mat, x1, y1, z1, u1, v1, r, g, b, alpha);
        vertex(vc, mat, x2, y2, z2, u2, v2, r, g, b, alpha);
        vertex(vc, mat, x3, y3, z3, u3, v3, r, g, b, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z, float u, float v,
                               float r, float g, float b, float alpha) {
        vc.vertex(mat, x, y, z).color(r, g, b, KabladeRenderTypes.fallbackAlpha(alpha, 0.45F))
                .uv(KabladeRenderTypes.vorpalBlackHoleU(u), v).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(VorpalBlackHoleEntity entity) {
        return EMPTY_TEXTURE;
    }
}

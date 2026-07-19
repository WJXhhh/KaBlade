package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.ConceptualMetaphorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Sword Enlightenment plus Domain of Unity's ordered three-wing convergence. */
public final class ConceptualMetaphorRenderer
        extends SwordEnlightenmentRenderer<ConceptualMetaphorEntity> {

    private static final int RING_SEGMENTS = 72;
    private static final float CENTER_Z = 2.15F;

    private static final float GOLD_R = 0.95F;
    private static final float GOLD_G = 0.86F;
    private static final float GOLD_B = 0.63F;
    private static final float LAVENDER_R = 0.74F;
    private static final float LAVENDER_G = 0.76F;
    private static final float LAVENDER_B = 1.0F;

    public ConceptualMetaphorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderVariant(ConceptualMetaphorEntity entity, float age,
                                 PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.conceptualMetaphor());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        renderUnityGeometry(age, poseStack.last().pose(), vc, true);
        poseStack.popPose();
    }

    static void renderOculusColor(PoseStack poseStack,
                                  BloodfyreOculusPipeline.DrawContext context,
                                  float age, float yaw) {
        context.draw(BloodfyreOculusPipeline.AnalyticShader.CONCEPTUAL_METAPHOR,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE, vc -> {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
                    renderUnityGeometry(age, poseStack.last().pose(), vc, true);
                    poseStack.popPose();
                });
    }

    static void renderOculusGlow(PoseStack poseStack,
                                 BloodfyreOculusPipeline.DrawContext context,
                                 float age, float yaw) {
        context.draw(BloodfyreOculusPipeline.AnalyticShader.CONCEPTUAL_METAPHOR,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE, vc -> {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
                    renderUnityGeometry(age, poseStack.last().pose(), vc, false);
                    poseStack.popPose();
                });
    }

    private static void renderUnityGeometry(float age, Matrix4f mat, VertexConsumer vc,
                                            boolean includeMandala) {
        if (includeMandala) {
            renderMandala(age, mat, vc);
        }
        renderWingSigils(age, mat, vc);
        renderUnityFinisher(age, mat, vc);
        renderAfterglowShards(age, mat, vc);
    }

    private static void renderMandala(float age, Matrix4f mat, VertexConsumer vc) {
        float open = smootherStep(stage(age, 7.0F, 8.0F));
        float fade = 1.0F - smootherStep(stage(age, 39.0F, 12.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        float rotation = age * 0.022F;
        drawSegmentedRing(vc, mat, 0.09F, CENTER_Z, 3.65F + open * 0.22F,
                0.075F, rotation, 6, GOLD_R, GOLD_G, GOLD_B, alpha * 0.50F);
        drawSegmentedRing(vc, mat, 0.115F, CENTER_Z, 4.42F,
                0.045F, -rotation * 1.35F, 9,
                LAVENDER_R, LAVENDER_G, LAVENDER_B, alpha * 0.34F);

        float triRadius = 3.28F;
        for (int i = 0; i < 3; i++) {
            float a0 = rotation + i * Mth.TWO_PI / 3.0F;
            float a1 = rotation + (i + 1) * Mth.TWO_PI / 3.0F;
            drawHorizontalRibbon(vc, mat,
                    Mth.cos(a0) * triRadius, 0.13F, CENTER_Z + Mth.sin(a0) * triRadius,
                    Mth.cos(a1) * triRadius, 0.13F, CENTER_Z + Mth.sin(a1) * triRadius,
                    0.035F, GOLD_R, GOLD_G, GOLD_B, alpha * 0.28F, 4.0F);
        }
    }

    private static void renderWingSigils(float age, Matrix4f mat, VertexConsumer vc) {
        float appear = smootherStep(stage(age, 7.0F, 4.0F));
        float disappear = 1.0F - smootherStep(stage(age, 36.0F, 7.0F));
        float alpha = appear * disappear;
        if (alpha <= 0.01F) {
            return;
        }

        float gather = smootherStep(stage(age, 30.0F, 6.0F));
        float radius = Mth.lerp(gather, 4.25F, 0.42F);
        float spin = age * (0.24F + gather * 0.20F);
        float scale = 0.86F + appear * 0.26F + gather * 0.18F;

        for (int i = 0; i < 3; i++) {
            float angle = spin + i * Mth.TWO_PI / 3.0F;
            float x = Mth.cos(angle) * radius;
            float z = CENTER_Z + Mth.sin(angle) * radius;
            float y = 1.05F + Mth.sin(angle * 2.0F) * 0.22F;
            drawWingGlyph(vc, mat, x, y, z, angle + Mth.HALF_PI, scale,
                    alpha * (0.72F + gather * 0.28F));

            float trailAngle = angle - 0.34F - gather * 0.18F;
            drawHorizontalRibbon(vc, mat,
                    Mth.cos(trailAngle) * radius, y - 0.02F,
                    CENTER_Z + Mth.sin(trailAngle) * radius,
                    x, y, z, 0.075F + gather * 0.035F,
                    LAVENDER_R, LAVENDER_G, LAVENDER_B,
                    alpha * 0.52F, 2.0F);
        }
    }

    private static void drawWingGlyph(VertexConsumer vc, Matrix4f mat,
                                      float x, float y, float z, float angle,
                                      float scale, float alpha) {
        float fx = Mth.cos(angle);
        float fz = Mth.sin(angle);
        float sx = -fz;
        float sz = fx;
        float front = 1.18F * scale;
        float back = 0.72F * scale;
        float width = 0.34F * scale;

        quad(vc, mat,
                x + fx * front, y, z + fz * front, 8.0F, 0.5F,
                x + sx * width, y + 0.12F * scale, z + sz * width, 8.5F, 0.0F,
                x - fx * back, y, z - fz * back, 9.0F, 0.5F,
                x - sx * width, y - 0.12F * scale, z - sz * width, 8.5F, 1.0F,
                GOLD_R, GOLD_G, GOLD_B, alpha);

        drawHorizontalRibbon(vc, mat,
                x - fx * 0.18F * scale, y - 0.025F, z - fz * 0.18F * scale,
                x - fx * 0.74F * scale + sx * 0.66F * scale, y - 0.025F,
                z - fz * 0.74F * scale + sz * 0.66F * scale,
                0.095F * scale, LAVENDER_R, LAVENDER_G, LAVENDER_B, alpha * 0.82F, 2.0F);
        drawHorizontalRibbon(vc, mat,
                x - fx * 0.18F * scale, y + 0.025F, z - fz * 0.18F * scale,
                x - fx * 0.74F * scale - sx * 0.66F * scale, y + 0.025F,
                z - fz * 0.74F * scale - sz * 0.66F * scale,
                0.095F * scale, LAVENDER_R, LAVENDER_G, LAVENDER_B, alpha * 0.82F, 2.0F);
    }

    private static void renderUnityFinisher(float age, Matrix4f mat, VertexConsumer vc) {
        float charge = smootherStep(stage(age, 32.5F, 3.5F));
        float fade = 1.0F - smootherStep(stage(age, 36.0F, 9.0F));
        float alpha = charge * fade;
        if (alpha <= 0.01F) {
            return;
        }

        float halfLength = 5.15F * charge;
        for (int i = 0; i < 3; i++) {
            float angle = i * Mth.TWO_PI / 3.0F + 0.12F;
            float fx = Mth.cos(angle);
            float fz = Mth.sin(angle);
            drawHorizontalRibbon(vc, mat,
                    -fx * halfLength, 1.12F, CENTER_Z - fz * halfLength,
                    fx * halfLength, 1.12F, CENTER_Z + fz * halfLength,
                    0.34F + charge * 0.22F,
                    GOLD_R, GOLD_G, GOLD_B, alpha * 0.82F, 0.0F);
            drawHorizontalRibbon(vc, mat,
                    -fx * halfLength, 1.16F, CENTER_Z - fz * halfLength,
                    fx * halfLength, 1.16F, CENTER_Z + fz * halfLength,
                    0.095F,
                    1.0F, 0.98F, 0.90F, alpha, 2.0F);
        }

        float shock = smootherStep(stage(age, 35.5F, 6.5F));
        float shockFade = 1.0F - smootherStep(stage(age, 40.0F, 7.0F));
        drawSegmentedRing(vc, mat, 0.16F, CENTER_Z,
                Mth.lerp(shock, 0.38F, 6.15F),
                Mth.lerp(shock, 0.34F, 0.055F), age * 0.015F, 12,
                GOLD_R, GOLD_G, GOLD_B, shockFade * (1.0F - shock * 0.52F));

        float column = smootherStep(stage(age, 35.0F, 1.0F))
                * (1.0F - smootherStep(stage(age, 36.0F, 5.0F)));
        drawLightColumn(vc, mat, 0.0F, CENTER_Z, 0.18F, 5.15F,
                0.44F, 1.0F, 0.96F, 0.82F, column * 0.78F);
        drawLightColumn(vc, mat, 0.0F, CENTER_Z, 0.42F, 4.65F,
                0.13F, 1.0F, 1.0F, 0.96F, column);
    }

    private static void renderAfterglowShards(float age, Matrix4f mat, VertexConsumer vc) {
        float open = smootherStep(stage(age, 35.5F, 2.0F));
        float fade = 1.0F - smootherStep(stage(age, 45.0F, 10.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        for (int i = 0; i < 18; i++) {
            float seed = deterministic(i, 4.71F);
            float angle = seed * Mth.TWO_PI + age * (0.012F + (i % 3) * 0.003F);
            float radius = 0.65F + deterministic(i, 8.13F) * 4.7F
                    + stage(age, 36.0F, 14.0F) * 1.2F;
            float x = Mth.cos(angle) * radius;
            float z = CENTER_Z + Mth.sin(angle) * radius;
            float y = 0.35F + deterministic(i, 2.27F) * 2.8F
                    + stage(age, 36.0F, 12.0F) * 0.65F;
            drawDiamond(vc, mat, x, y, z,
                    0.10F + deterministic(i, 6.41F) * 0.20F,
                    angle + age * 0.025F,
                    i % 3 == 0 ? GOLD_R : LAVENDER_R,
                    i % 3 == 0 ? GOLD_G : LAVENDER_G,
                    i % 3 == 0 ? GOLD_B : LAVENDER_B,
                    alpha * (0.35F + seed * 0.38F));
        }
    }

    private static void drawSegmentedRing(VertexConsumer vc, Matrix4f mat,
                                          float y, float centerZ, float radius, float width,
                                          float rotation, int gapRate,
                                          float red, float green, float blue, float alpha) {
        float inner = Math.max(0.02F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            if (i % gapRate == gapRate - 1) {
                continue;
            }
            float t0 = i / (float) RING_SEGMENTS;
            float t1 = (i + 1) / (float) RING_SEGMENTS;
            float a0 = rotation + t0 * Mth.TWO_PI;
            float a1 = rotation + t1 * Mth.TWO_PI;
            quad(vc, mat,
                    Mth.cos(a0) * outer, y, centerZ + Mth.sin(a0) * outer, 4.0F + t0, 0.0F,
                    Mth.cos(a1) * outer, y, centerZ + Mth.sin(a1) * outer, 4.0F + t1, 0.0F,
                    Mth.cos(a1) * inner, y, centerZ + Mth.sin(a1) * inner, 4.0F + t1, 1.0F,
                    Mth.cos(a0) * inner, y, centerZ + Mth.sin(a0) * inner, 4.0F + t0, 1.0F,
                    red, green, blue, alpha);
        }
    }

    private static void drawHorizontalRibbon(VertexConsumer vc, Matrix4f mat,
                                             float x0, float y0, float z0,
                                             float x1, float y1, float z1,
                                             float width,
                                             float red, float green, float blue, float alpha,
                                             float uBase) {
        Vector3f direction = new Vector3f(x1 - x0, 0.0F, z1 - z0);
        if (direction.lengthSquared() <= 1.0E-5F) {
            return;
        }
        direction.normalize();
        float sx = -direction.z * width;
        float sz = direction.x * width;
        quad(vc, mat,
                x0 + sx, y0, z0 + sz, uBase, 0.0F,
                x1 + sx, y1, z1 + sz, uBase + 1.0F, 0.0F,
                x1 - sx, y1, z1 - sz, uBase + 1.0F, 1.0F,
                x0 - sx, y0, z0 - sz, uBase, 1.0F,
                red, green, blue, alpha);
    }

    private static void drawLightColumn(VertexConsumer vc, Matrix4f mat,
                                        float x, float z, float y0, float y1, float halfWidth,
                                        float red, float green, float blue, float alpha) {
        quad(vc, mat,
                x - halfWidth, y0, z, 2.0F, 1.0F,
                x - halfWidth * 0.45F, y1, z, 2.0F, 0.0F,
                x + halfWidth * 0.45F, y1, z, 3.0F, 0.0F,
                x + halfWidth, y0, z, 3.0F, 1.0F,
                red, green, blue, alpha);
        quad(vc, mat,
                x, y0, z - halfWidth, 2.0F, 1.0F,
                x, y1, z - halfWidth * 0.45F, 2.0F, 0.0F,
                x, y1, z + halfWidth * 0.45F, 3.0F, 0.0F,
                x, y0, z + halfWidth, 3.0F, 1.0F,
                red, green, blue, alpha);
    }

    private static void drawDiamond(VertexConsumer vc, Matrix4f mat,
                                    float x, float y, float z, float size, float rotation,
                                    float red, float green, float blue, float alpha) {
        float c = Mth.cos(rotation);
        float s = Mth.sin(rotation);
        float mx = c * size;
        float mz = s * size;
        float sx = -s * size * 0.42F;
        float sz = c * size * 0.42F;
        quad(vc, mat,
                x - mx, y, z - mz, 8.0F, 0.5F,
                x + sx, y + size * 1.55F, z + sz, 8.5F, 0.0F,
                x + mx, y, z + mz, 9.0F, 0.5F,
                x - sx, y - size * 1.55F, z - sz, 8.5F, 1.0F,
                red, green, blue, alpha);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float red, float green, float blue, float alpha) {
        vertex(vc, mat, x0, y0, z0, u0, v0, red, green, blue, alpha);
        vertex(vc, mat, x1, y1, z1, u1, v1, red, green, blue, alpha);
        vertex(vc, mat, x2, y2, z2, u2, v2, red, green, blue, alpha);
        vertex(vc, mat, x3, y3, z3, u3, v3, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z, float u, float v,
                               float red, float green, float blue, float alpha) {
        boolean legacyFallback = KabladeRenderTypes.useShaderFallbackTextures()
                && !BloodfyreOculusPipeline.isPrivateGeometryPass();
        float safeAlpha = Mth.clamp(alpha * (legacyFallback ? 1.22F : 1.0F), 0.0F, 1.0F);
        if (legacyFallback) {
            vc.vertex(mat, x, y, z).color(red, green, blue, safeAlpha).endVertex();
            return;
        }
        vc.vertex(mat, x, y, z)
                .color(red, green, blue, safeAlpha)
                .uv(u, v)
                .endVertex();
    }

    private static float stage(float age, float start, float duration) {
        return Mth.clamp((age - start) / duration, 0.0F, 1.0F);
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float deterministic(int index, float salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }
}

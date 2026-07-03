package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusSkillRenderer;
import com.wjx.kablade.entity.StageLightEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 「聚光舞台」渲染器。几何只提供光带轮廓和 UV；柔边、白热核心与沿环移动的亮斑由
 * {@code kablade:stage_light} 着色器生成。
 */
public final class StageLightRenderer extends EntityRenderer<StageLightEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int RING_SEGMENTS = 96;
    private static final float STAGE_RADIUS = 5.85F;
    private static final int SPOT_COUNT = 6;
    private static final int EDGE_SPARK_COUNT = 24;

    public StageLightRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(StageLightEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!KabladeRenderTypes.useShaderFallbackTextures()
                && OculusSkillRenderer.runIfNeeded(immediate ->
                render(entity, entityYaw, partialTick, poseStack, immediate, packedLight))) {
            return;
        }

        float age = entity.tickCount + partialTick;
        float life = entity.getLifetime();
        float intro = smootherStep(Mth.clamp(age / 10.0F, 0.0F, 1.0F));
        float outro = smootherStep(Mth.clamp((life - age) / 16.0F, 0.0F, 1.0F));
        float alpha = intro * outro;
        if (alpha <= 0.005F) {
            return;
        }

        float radiusEase = 1.0F - (1.0F - intro) * (1.0F - intro);
        float collapse = Mth.lerp(outro, 0.04F, 1.0F);
        float widthScale = 0.25F + collapse * 0.75F;
        float heightScale = 0.18F + collapse * 0.82F;
        float radius = Mth.lerp(radiusEase, 1.05F, STAGE_RADIUS) * collapse;
        float openSpan = 356.0F * intro;
        float goldR = 1.0F;
        float goldG = 0.72F;
        float goldB = 0.22F;
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.stageLight());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(0.0, 0.025, 0.0);
        Matrix4f mat = poseStack.last().pose();

        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            renderOculusFallback(vc, mat, age, intro, alpha, radius, openSpan, collapse, widthScale, heightScale);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        // 主舞台环：宽暖光、金色主体、白热核心三层叠加。
        ring(vc, mat, radius, 0.88F * widthScale, -90.0F, openSpan,
                goldR, goldG, goldB, alpha * 0.13F, 0.0F);
        ring(vc, mat, radius, 0.34F * widthScale, -90.0F, openSpan,
                1.0F, 0.78F, 0.30F, alpha * 0.70F, 0.0F);
        ring(vc, mat, radius + 0.03F * collapse, 0.105F * widthScale, -90.0F, openSpan,
                1.0F, 0.96F, 0.78F, alpha, 0.0F);

        // 轻微反向漂移的内圈让舞台不显得像静态贴地圆环。
        if (intro > 0.55F) {
            float innerAlpha = alpha * Mth.clamp((intro - 0.55F) / 0.45F, 0.0F, 1.0F);
            ring(vc, mat, radius * 0.92F, 0.16F * widthScale,
                    75.0F - age * 1.25F, 310.0F,
                    1.0F, 0.82F, 0.42F, innerAlpha * 0.34F, age * 0.004F);
        }

        // 起手旋斩：快速追上主环，随后在 18 tick 内收掉。
        if (age < 18.0F) {
            float slashT = Mth.clamp(age / 18.0F, 0.0F, 1.0F);
            float slashAlpha = (1.0F - smootherStep(slashT)) * outro;
            float slashRadius = Mth.lerp(smootherStep(Mth.clamp(age / 8.0F, 0.0F, 1.0F)),
                    1.35F, STAGE_RADIUS * 1.02F);
            ring(vc, mat, slashRadius, 0.64F,
                    -125.0F + age * 17.0F, 255.0F,
                    1.0F, 0.88F, 0.55F, slashAlpha * 0.74F, age * 0.012F);
        }

        // 六束低透明度的地面径向追光。
        if (intro > 0.65F) {
            float beamAlpha = alpha * Mth.clamp((intro - 0.65F) / 0.35F, 0.0F, 1.0F);
            for (int i = 0; i < SPOT_COUNT; i++) {
                float angle = (float) (Math.PI * 2.0 * i / SPOT_COUNT) + age * 0.012F;
                radialRibbon(vc, mat, angle, 0.75F * collapse, radius * 0.93F,
                        0.31F * widthScale,
                        1.0F, 0.77F, 0.31F, beamAlpha * 0.12F,
                        i / (float) SPOT_COUNT + age * 0.002F);
            }
        }

        // 环上的移动追光点与短灯柱；相邻点有轻微错相，复现参考里的舞台灯轮转。
        for (int i = 0; i < SPOT_COUNT; i++) {
            float phase = i / (float) SPOT_COUNT;
            float angle = (float) (Math.PI * 2.0 * phase) + age * 0.035F;
            float x = Mth.cos(angle) * radius;
            float z = Mth.sin(angle) * radius;
            float pulse = 0.58F + 0.42F * Mth.sin(age * 0.32F + i * 1.7F);
            float spotAlpha = alpha * Mth.clamp(pulse, 0.15F, 1.0F);
            verticalCross(vc, mat, x, z, angle, 0.16F * widthScale,
                    (1.35F + pulse * 0.55F) * heightScale,
                    1.0F, 0.88F, 0.56F, spotAlpha * 0.38F, phase);
            verticalCross(vc, mat, x, z, angle, 0.055F * widthScale, 0.72F * heightScale,
                    1.0F, 0.98F, 0.88F, spotAlpha, phase + 0.17F);
        }

        // 环缘跳动星点：错相的高度、尺寸和亮度让它们像舞台灯泡一样轮流闪烁。
        float sparkPresence = Mth.clamp((intro - 0.42F) / 0.58F, 0.0F, 1.0F);
        for (int i = 0; i < EDGE_SPARK_COUNT; i++) {
            float phase = i / (float) EDGE_SPARK_COUNT;
            float angle = (float) (Math.PI * 2.0 * phase) - age * 0.006F;
            float jump = 0.5F + 0.5F * Mth.sin(age * 0.48F + i * 2.17F);
            float blinkWave = 0.5F + 0.5F * Mth.sin(age * (0.61F + (i % 4) * 0.045F)
                    + i * 3.41F);
            float blink = 0.12F + 0.88F * blinkWave * blinkWave * blinkWave;
            float sparkRadius = radius + ((i % 3) - 1) * 0.075F * collapse;
            float x = Mth.cos(angle) * sparkRadius;
            float z = Mth.sin(angle) * sparkRadius;
            float y = (0.10F + jump * 0.25F) * collapse;
            float size = (0.075F + jump * 0.075F) * (0.30F + collapse * 0.70F);
            float sparkAlpha = alpha * sparkPresence * blink;
            sparkleCross(vc, mat, x, y, z, angle, size,
                    1.0F, 0.76F, 0.08F, sparkAlpha);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static float smootherStep(float t) {
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static void renderOculusFallback(VertexConsumer vc, Matrix4f mat, float age, float intro,
                                             float alpha, float radius, float openSpan, float collapse,
                                             float widthScale, float heightScale) {
        softRing(vc, mat, radius, 0.70F * widthScale, -90.0F, openSpan,
                1.0F, 0.62F, 0.16F, alpha * 0.34F);
        softRing(vc, mat, radius, 0.34F * widthScale, -90.0F, openSpan,
                1.0F, 0.74F, 0.26F, alpha * 0.88F);
        softRing(vc, mat, radius + 0.020F * collapse, 0.075F * widthScale, -90.0F, openSpan,
                1.0F, 0.98F, 0.84F, alpha * 1.34F);
        ringHighlights(vc, mat, age, radius + 0.018F * collapse, openSpan,
                1.0F, 0.96F, 0.72F, alpha * 1.02F);

        if (intro > 0.55F) {
            float innerAlpha = alpha * Mth.clamp((intro - 0.55F) / 0.45F, 0.0F, 1.0F);
            softRing(vc, mat, radius * 0.92F, 0.08F * widthScale,
                    75.0F - age * 1.25F, 310.0F,
                    1.0F, 0.78F, 0.34F, innerAlpha * 0.40F);
        }

        if (age < 18.0F) {
            float slashT = Mth.clamp(age / 18.0F, 0.0F, 1.0F);
            float slashAlpha = 1.0F - smootherStep(slashT);
            float slashRadius = Mth.lerp(smootherStep(Mth.clamp(age / 8.0F, 0.0F, 1.0F)),
                    1.35F, STAGE_RADIUS * 1.02F);
            softRing(vc, mat, slashRadius, 0.22F,
                    -125.0F + age * 17.0F, 255.0F,
                    1.0F, 0.78F, 0.36F, alpha * slashAlpha * 0.52F);
        }

        if (intro > 0.65F) {
            float beamAlpha = alpha * Mth.clamp((intro - 0.65F) / 0.35F, 0.0F, 1.0F);
            for (int i = 0; i < SPOT_COUNT; i++) {
                float angle = (float) (Math.PI * 2.0 * i / SPOT_COUNT) + age * 0.012F;
                softRadialRibbon(vc, mat, angle, 0.72F * collapse, radius * 0.92F,
                        0.22F * widthScale,
                        1.0F, 0.68F, 0.22F, beamAlpha * 0.28F);
                softRadialRibbon(vc, mat, angle, 1.10F * collapse, radius * 0.88F,
                        0.038F * widthScale,
                        1.0F, 0.97F, 0.76F, beamAlpha * 0.88F);
            }
        }

        for (int i = 0; i < SPOT_COUNT; i++) {
            float phase = i / (float) SPOT_COUNT;
            float angle = (float) (Math.PI * 2.0 * phase) + age * 0.035F;
            float x = Mth.cos(angle) * radius;
            float z = Mth.sin(angle) * radius;
            float pulse = 0.58F + 0.42F * Mth.sin(age * 0.32F + i * 1.7F);
            float spotAlpha = alpha * Mth.clamp(pulse, 0.15F, 1.0F);
            colorVerticalCross(vc, mat, x, z, angle, 0.045F * widthScale,
                    (0.92F + pulse * 0.34F) * heightScale,
                    1.0F, 0.86F, 0.48F, spotAlpha * 0.55F);
        }

        float sparkPresence = Mth.clamp((intro - 0.42F) / 0.58F, 0.0F, 1.0F);
        for (int i = 0; i < EDGE_SPARK_COUNT; i += 2) {
            float phase = i / (float) EDGE_SPARK_COUNT;
            float angle = (float) (Math.PI * 2.0 * phase) - age * 0.006F;
            float jump = 0.5F + 0.5F * Mth.sin(age * 0.48F + i * 2.17F);
            float blinkWave = Mth.sin(age * 0.61F + i * 3.41F);
            float blink = 0.36F + 0.64F * blinkWave * blinkWave;
            float sparkRadius = radius + ((i % 3) - 1) * 0.045F * collapse;
            float x = Mth.cos(angle) * sparkRadius;
            float z = Mth.sin(angle) * sparkRadius;
            float y = (0.10F + jump * 0.20F) * collapse;
            float size = (0.050F + jump * 0.040F) * (0.30F + collapse * 0.70F);
            colorSparkleCross(vc, mat, x, y, z, angle, size,
                    1.0F, 0.86F, 0.44F, alpha * sparkPresence * blink);
        }
    }

    private static void softRing(VertexConsumer vc, Matrix4f mat, float radius, float width,
                                 float startDeg, float spanDeg,
                                 float r, float g, float b, float alpha) {
        ringBand(vc, mat, radius - width * 0.50F, radius - width * 0.34F,
                startDeg, spanDeg, r, g, b, alpha * 0.18F);
        ringBand(vc, mat, radius - width * 0.34F, radius - width * 0.16F,
                startDeg, spanDeg, r, g, b, alpha * 0.50F);
        ringBand(vc, mat, radius - width * 0.16F, radius + width * 0.16F,
                startDeg, spanDeg, r, g, b, alpha);
        ringBand(vc, mat, radius + width * 0.16F, radius + width * 0.34F,
                startDeg, spanDeg, r, g, b, alpha * 0.50F);
        ringBand(vc, mat, radius + width * 0.34F, radius + width * 0.50F,
                startDeg, spanDeg, r, g, b, alpha * 0.18F);
    }

    private static void ringHighlights(VertexConsumer vc, Matrix4f mat, float age, float radius, float openSpan,
                                       float r, float g, float b, float alpha) {
        if (openSpan < 24.0F) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            float phase = Mth.frac(i / 5.0F + age * 0.010F);
            float center = -90.0F + phase * openSpan;
            float pulse = 0.62F + 0.38F * Mth.sin(age * 0.34F + i * 1.91F);
            float span = 12.0F + pulse * 8.0F;
            float start = Mth.clamp(center - span * 0.5F, -90.0F, -90.0F + openSpan);
            float end = Mth.clamp(center + span * 0.5F, -90.0F, -90.0F + openSpan);
            if (end <= start + 1.0F) {
                continue;
            }
            ringBand(vc, mat, radius - 0.040F, radius + 0.040F, start, end - start,
                    r, g, b, alpha * pulse);
            ringBand(vc, mat, radius - 0.105F, radius - 0.066F, start + 2.0F, Math.max(1.0F, end - start - 4.0F),
                    1.0F, 0.70F, 0.20F, alpha * pulse * 0.38F);
            ringBand(vc, mat, radius + 0.066F, radius + 0.105F, start + 2.0F, Math.max(1.0F, end - start - 4.0F),
                    1.0F, 0.70F, 0.20F, alpha * pulse * 0.38F);
        }
    }

    private static void ringBand(VertexConsumer vc, Matrix4f mat, float inner, float outer,
                                 float startDeg, float spanDeg,
                                 float r, float g, float b, float alpha) {
        int segments = Math.max(1, Mth.ceil(96.0F * spanDeg / 360.0F));
        for (int i = 0; i < segments; i++) {
            float f0 = i / (float) segments;
            float f1 = (i + 1) / (float) segments;
            float a0 = (float) Math.toRadians(startDeg + spanDeg * f0);
            float a1 = (float) Math.toRadians(startDeg + spanDeg * f1);
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);

            quadAlpha(vc, mat,
                    c0 * outer, 0.0F, s0 * outer, f0, 0.0F, alpha,
                    c1 * outer, 0.0F, s1 * outer, f1, 0.0F, alpha,
                    c1 * inner, 0.0F, s1 * inner, f1, 1.0F, alpha,
                    c0 * inner, 0.0F, s0 * inner, f0, 1.0F, alpha,
                    r, g, b);
        }
    }

    private static void softRadialRibbon(VertexConsumer vc, Matrix4f mat, float angle,
                                         float startRadius, float endRadius, float width,
                                         float r, float g, float b, float alpha) {
        float dx = Mth.cos(angle);
        float dz = Mth.sin(angle);
        float px = -dz * width * 0.5F;
        float pz = dx * width * 0.5F;
        float pxInner = px * 0.38F;
        float pzInner = pz * 0.38F;
        quadAlpha(vc, mat,
                dx * startRadius + px, 0.006F, dz * startRadius + pz, 0.0F, 0.0F, alpha * 0.24F,
                dx * endRadius + px, 0.006F, dz * endRadius + pz, 1.0F, 0.0F, alpha * 0.24F,
                dx * endRadius + pxInner, 0.006F, dz * endRadius + pzInner, 1.0F, 0.5F, alpha * 0.24F,
                dx * startRadius + pxInner, 0.006F, dz * startRadius + pzInner, 0.0F, 0.5F, alpha * 0.24F,
                r, g, b);
        quadAlpha(vc, mat,
                dx * startRadius + pxInner, 0.006F, dz * startRadius + pzInner, 0.0F, 0.5F, alpha,
                dx * endRadius + pxInner, 0.006F, dz * endRadius + pzInner, 1.0F, 0.5F, alpha,
                dx * endRadius - pxInner, 0.006F, dz * endRadius - pzInner, 1.0F, 0.5F, alpha,
                dx * startRadius - pxInner, 0.006F, dz * startRadius - pzInner, 0.0F, 0.5F, alpha,
                r, g, b);
        quadAlpha(vc, mat,
                dx * startRadius - pxInner, 0.006F, dz * startRadius - pzInner, 0.0F, 0.5F, alpha * 0.24F,
                dx * endRadius - pxInner, 0.006F, dz * endRadius - pzInner, 1.0F, 0.5F, alpha * 0.24F,
                dx * endRadius - px, 0.006F, dz * endRadius - pz, 1.0F, 1.0F, alpha * 0.24F,
                dx * startRadius - px, 0.006F, dz * startRadius - pz, 0.0F, 1.0F, alpha * 0.24F,
                r, g, b);
    }

    private static void colorVerticalCross(VertexConsumer vc, Matrix4f mat, float x, float z,
                                           float angle, float width, float height,
                                           float r, float g, float b, float alpha) {
        colorVerticalPlane(vc, mat, x, z, Mth.cos(angle), Mth.sin(angle), width, height,
                r, g, b, alpha);
        colorVerticalPlane(vc, mat, x, z, -Mth.sin(angle), Mth.cos(angle), width, height,
                r, g, b, alpha);
    }

    private static void colorVerticalPlane(VertexConsumer vc, Matrix4f mat, float x, float z,
                                           float dx, float dz, float width, float height,
                                           float r, float g, float b, float alpha) {
        float hx = dx * width * 0.5F;
        float hz = dz * width * 0.5F;
        quadAlpha(vc, mat,
                x - hx, 0.02F, z - hz, 0.0F, 0.0F, alpha * 0.50F,
                x - hx, height, z - hz, 0.0F, 0.0F, alpha,
                x + hx, height, z + hz, 0.0F, 0.0F, alpha,
                x + hx, 0.02F, z + hz, 0.0F, 0.0F, alpha * 0.50F,
                r, g, b);
    }

    private static void colorSparkleCross(VertexConsumer vc, Matrix4f mat,
                                          float x, float y, float z, float angle, float size,
                                          float r, float g, float b, float alpha) {
        colorSparklePlane(vc, mat, x, y, z, Mth.cos(angle), Mth.sin(angle), size,
                r, g, b, alpha);
        colorSparklePlane(vc, mat, x, y, z, -Mth.sin(angle), Mth.cos(angle), size,
                r, g, b, alpha);
    }

    private static void colorSparklePlane(VertexConsumer vc, Matrix4f mat,
                                          float x, float y, float z, float dx, float dz, float size,
                                          float r, float g, float b, float alpha) {
        float hx = dx * size;
        float hz = dz * size;
        quadAlpha(vc, mat,
                x - hx, y - size, z - hz, 0.0F, 0.0F, 0.0F,
                x - hx, y + size, z - hz, 0.0F, 0.0F, alpha,
                x + hx, y + size, z + hz, 0.0F, 0.0F, alpha,
                x + hx, y - size, z + hz, 0.0F, 0.0F, 0.0F,
                r, g, b);
    }

    private static void ring(VertexConsumer vc, Matrix4f mat, float radius, float width,
                             float startDeg, float spanDeg,
                             float r, float g, float b, float alpha, float uOffset) {
        int segments = Math.max(1, Mth.ceil(RING_SEGMENTS * spanDeg / 360.0F));
        for (int i = 0; i < segments; i++) {
            float f0 = i / (float) segments;
            float f1 = (i + 1) / (float) segments;
            float a0 = (float) Math.toRadians(startDeg + spanDeg * f0);
            float a1 = (float) Math.toRadians(startDeg + spanDeg * f1);
            float inner = radius - width * 0.5F;
            float outer = radius + width * 0.5F;
            float u0 = uOffset + f0;
            float u1 = uOffset + f1;

            quad(vc, mat,
                    Mth.cos(a0) * outer, 0.0F, Mth.sin(a0) * outer, u0, 0.0F,
                    Mth.cos(a1) * outer, 0.0F, Mth.sin(a1) * outer, u1, 0.0F,
                    Mth.cos(a1) * inner, 0.0F, Mth.sin(a1) * inner, u1, 1.0F,
                    Mth.cos(a0) * inner, 0.0F, Mth.sin(a0) * inner, u0, 1.0F,
                    r, g, b, alpha);
        }
    }

    private static void radialRibbon(VertexConsumer vc, Matrix4f mat, float angle,
                                     float startRadius, float endRadius, float width,
                                     float r, float g, float b, float alpha, float uOffset) {
        float dx = Mth.cos(angle);
        float dz = Mth.sin(angle);
        float px = -dz * width * 0.5F;
        float pz = dx * width * 0.5F;
        quad(vc, mat,
                dx * startRadius + px, 0.006F, dz * startRadius + pz, uOffset, 0.0F,
                dx * endRadius + px, 0.006F, dz * endRadius + pz, uOffset + 1.0F, 0.0F,
                dx * endRadius - px, 0.006F, dz * endRadius - pz, uOffset + 1.0F, 1.0F,
                dx * startRadius - px, 0.006F, dz * startRadius - pz, uOffset, 1.0F,
                r, g, b, alpha);
    }

    private static void verticalCross(VertexConsumer vc, Matrix4f mat, float x, float z,
                                      float angle, float width, float height,
                                      float r, float g, float b, float alpha, float uOffset) {
        verticalPlane(vc, mat, x, z, Mth.cos(angle), Mth.sin(angle), width, height,
                r, g, b, alpha, uOffset);
        verticalPlane(vc, mat, x, z, -Mth.sin(angle), Mth.cos(angle), width, height,
                r, g, b, alpha, uOffset + 0.33F);
    }

    private static void verticalPlane(VertexConsumer vc, Matrix4f mat, float x, float z,
                                      float dx, float dz, float width, float height,
                                      float r, float g, float b, float alpha, float uOffset) {
        float hx = dx * width * 0.5F;
        float hz = dz * width * 0.5F;
        quad(vc, mat,
                x - hx, 0.02F, z - hz, uOffset, 0.0F,
                x - hx, height, z - hz, uOffset + 1.0F, 0.0F,
                x + hx, height, z + hz, uOffset + 1.0F, 1.0F,
                x + hx, 0.02F, z + hz, uOffset, 1.0F,
                r, g, b, alpha);
    }

    private static void sparkleCross(VertexConsumer vc, Matrix4f mat,
                                     float x, float y, float z, float angle, float size,
                                     float r, float g, float b, float alpha) {
        sparklePlane(vc, mat, x, y, z, Mth.cos(angle), Mth.sin(angle), size,
                r, g, b, alpha);
        sparklePlane(vc, mat, x, y, z, -Mth.sin(angle), Mth.cos(angle), size,
                r, g, b, alpha);
    }

    private static void sparklePlane(VertexConsumer vc, Matrix4f mat,
                                     float x, float y, float z, float dx, float dz, float size,
                                     float r, float g, float b, float alpha) {
        float hx = dx * size;
        float hz = dz * size;
        quad(vc, mat,
                x - hx, y - size, z - hz, -2.0F, 0.0F,
                x - hx, y + size, z - hz, -2.0F, 1.0F,
                x + hx, y + size, z + hz, -1.0F, 1.0F,
                x + hx, y - size, z + hz, -1.0F, 0.0F,
                r, g, b, alpha);
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

    private static void quadAlpha(VertexConsumer vc, Matrix4f mat,
                                  float x0, float y0, float z0, float u0, float v0, float a0,
                                  float x1, float y1, float z1, float u1, float v1, float a1,
                                  float x2, float y2, float z2, float u2, float v2, float a2,
                                  float x3, float y3, float z3, float u3, float v3, float a3,
                                  float r, float g, float b) {
        colorVertex(vc, mat, x0, y0, z0, r, g, b, a0);
        colorVertex(vc, mat, x1, y1, z1, r, g, b, a1);
        colorVertex(vc, mat, x2, y2, z2, r, g, b, a2);
        colorVertex(vc, mat, x3, y3, z3, r, g, b, a3);
    }

    private static void colorVertex(VertexConsumer vc, Matrix4f mat,
                                    float x, float y, float z,
                                    float r, float g, float b, float alpha) {
        vc.vertex(mat, x, y, z).color(r, g, b, alpha).endVertex();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z, float u, float v,
                               float r, float g, float b, float alpha) {
        vc.vertex(mat, x, y, z).color(r, g, b, KabladeRenderTypes.fallbackAlpha(alpha, 0.34F))
                .uv(KabladeRenderTypes.stageLightU(u), v).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(StageLightEntity entity) {
        return EMPTY_TEXTURE;
    }
}

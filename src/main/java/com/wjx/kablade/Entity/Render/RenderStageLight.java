package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityStageLight;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * 「聚光舞台」1.12.2 渲染器。
 * <p>
 * 1.20 版本使用 {@code kablade:stage_light} core shader；1.12.2 的渲染管线没有
 * RenderType / MultiBufferSource / ShaderInstance，因此这里把 1.20 的无着色器回退路径
 * 移植到兼容管线：优先使用 GLSL 1.20 程序化辉光，着色器不可用或已有外部光影程序时
 * 自动回退到 Tessellator + BufferBuilder(POSITION_COLOR) + 加色混合。
 */
public class RenderStageLight extends Render<EntityStageLight> {

    private static final int RING_SEGMENTS = 96;
    private static final float STAGE_RADIUS = 5.85F;
    private static final int SPOT_COUNT = 6;
    private static final int EDGE_SPARK_COUNT = 24;

    public RenderStageLight(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityStageLight entity) {
        return null;
    }

    @Override
    public void doRender(EntityStageLight entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        float life = entity.getLifetime();
        float intro = smootherStep(MathHelper.clamp(age / 10.0F, 0.0F, 1.0F));
        float outro = smootherStep(MathHelper.clamp((life - age) / 16.0F, 0.0F, 1.0F));
        float alpha = intro * outro;
        if (alpha <= 0.005F) {
            return;
        }

        float radiusEase = 1.0F - (1.0F - intro) * (1.0F - intro);
        float collapse = lerp(outro, 0.04F, 1.0F);
        float widthScale = 0.25F + collapse * 0.75F;
        float heightScale = 0.18F + collapse * 0.82F;
        float radius = lerp(radiusEase, 1.05F, STAGE_RADIUS) * collapse;
        float openSpan = 356.0F * intro;

        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;

        int previousProgram = StageLightShader.bind(age, alpha);
        boolean matrixPushed = false;
        try {
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.translate(x, y + 0.025D, z);
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        softRing(buffer, radius, 0.70F * widthScale, -90.0F, openSpan,
                1.0F, 0.62F, 0.16F, alpha * 0.34F);
        softRing(buffer, radius, 0.34F * widthScale, -90.0F, openSpan,
                1.0F, 0.74F, 0.26F, alpha * 0.88F);
        softRing(buffer, radius + 0.020F * collapse, 0.075F * widthScale, -90.0F, openSpan,
                1.0F, 0.98F, 0.84F, alpha * 1.34F);
        ringHighlights(buffer, age, radius + 0.018F * collapse, openSpan,
                1.0F, 0.96F, 0.72F, alpha * 1.02F);

        if (intro > 0.55F) {
            float innerAlpha = alpha * MathHelper.clamp((intro - 0.55F) / 0.45F, 0.0F, 1.0F);
            softRing(buffer, radius * 0.92F, 0.08F * widthScale,
                    75.0F - age * 1.25F, 310.0F,
                    1.0F, 0.78F, 0.34F, innerAlpha * 0.40F);
        }

        if (age < 18.0F) {
            float slashT = MathHelper.clamp(age / 18.0F, 0.0F, 1.0F);
            float slashAlpha = 1.0F - smootherStep(slashT);
            float slashRadius = lerp(smootherStep(MathHelper.clamp(age / 8.0F, 0.0F, 1.0F)),
                    1.35F, STAGE_RADIUS * 1.02F);
            softRing(buffer, slashRadius, 0.22F,
                    -125.0F + age * 17.0F, 255.0F,
                    1.0F, 0.78F, 0.36F, alpha * slashAlpha * 0.52F);
        }

        if (intro > 0.65F) {
            float beamAlpha = alpha * MathHelper.clamp((intro - 0.65F) / 0.35F, 0.0F, 1.0F);
            for (int i = 0; i < SPOT_COUNT; i++) {
                float angle = (float) (Math.PI * 2.0 * i / SPOT_COUNT) + age * 0.012F;
                softRadialRibbon(buffer, angle, 0.72F * collapse, radius * 0.92F,
                        0.22F * widthScale,
                        1.0F, 0.68F, 0.22F, beamAlpha * 0.28F);
                softRadialRibbon(buffer, angle, 1.10F * collapse, radius * 0.88F,
                        0.038F * widthScale,
                        1.0F, 0.97F, 0.76F, beamAlpha * 0.88F);
            }
        }

        for (int i = 0; i < SPOT_COUNT; i++) {
            float phase = i / (float) SPOT_COUNT;
            float angle = (float) (Math.PI * 2.0 * phase) + age * 0.035F;
            float xp = MathHelper.cos(angle) * radius;
            float zp = MathHelper.sin(angle) * radius;
            float pulse = 0.58F + 0.42F * MathHelper.sin(age * 0.32F + i * 1.7F);
            float spotAlpha = alpha * MathHelper.clamp(pulse, 0.15F, 1.0F);
            colorVerticalCross(buffer, xp, zp, angle, 0.045F * widthScale,
                    (0.92F + pulse * 0.34F) * heightScale,
                    1.0F, 0.86F, 0.48F, spotAlpha * 0.55F);
        }

        float sparkPresence = MathHelper.clamp((intro - 0.42F) / 0.58F, 0.0F, 1.0F);
        for (int i = 0; i < EDGE_SPARK_COUNT; i += 2) {
            float phase = i / (float) EDGE_SPARK_COUNT;
            float angle = (float) (Math.PI * 2.0 * phase) - age * 0.006F;
            float jump = 0.5F + 0.5F * MathHelper.sin(age * 0.48F + i * 2.17F);
            float blinkWave = MathHelper.sin(age * 0.61F + i * 3.41F);
            float blink = 0.36F + 0.64F * blinkWave * blinkWave;
            float sparkRadius = radius + ((i % 3) - 1) * 0.045F * collapse;
            float xp = MathHelper.cos(angle) * sparkRadius;
            float zp = MathHelper.sin(angle) * sparkRadius;
            float yp = (0.10F + jump * 0.20F) * collapse;
            float size = (0.050F + jump * 0.040F) * (0.30F + collapse * 0.70F);
            colorSparkleCross(buffer, xp, yp, zp, angle, size,
                    1.0F, 0.86F, 0.44F, alpha * sparkPresence * blink);
        }

            tessellator.draw();
        } finally {
            if (matrixPushed) {
                GlStateManager.popMatrix();
            }
            StageLightShader.restore(previousProgram);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static float smootherStep(float t) {
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float lerp(float t, float min, float max) {
        return min + (max - min) * t;
    }

    private static void softRing(BufferBuilder buffer, float radius, float width,
                                 float startDeg, float spanDeg,
                                 float r, float g, float b, float alpha) {
        ringBand(buffer, radius - width * 0.50F, radius - width * 0.34F,
                startDeg, spanDeg, r, g, b, alpha * 0.18F);
        ringBand(buffer, radius - width * 0.34F, radius - width * 0.16F,
                startDeg, spanDeg, r, g, b, alpha * 0.50F);
        ringBand(buffer, radius - width * 0.16F, radius + width * 0.16F,
                startDeg, spanDeg, r, g, b, alpha);
        ringBand(buffer, radius + width * 0.16F, radius + width * 0.34F,
                startDeg, spanDeg, r, g, b, alpha * 0.50F);
        ringBand(buffer, radius + width * 0.34F, radius + width * 0.50F,
                startDeg, spanDeg, r, g, b, alpha * 0.18F);
    }

    private static void ringHighlights(BufferBuilder buffer, float age, float radius, float openSpan,
                                       float r, float g, float b, float alpha) {
        if (openSpan < 24.0F) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            float phase = frac(i / 5.0F + age * 0.010F);
            float center = -90.0F + phase * openSpan;
            float pulse = 0.62F + 0.38F * MathHelper.sin(age * 0.34F + i * 1.91F);
            float span = 12.0F + pulse * 8.0F;
            float start = MathHelper.clamp(center - span * 0.5F, -90.0F, -90.0F + openSpan);
            float end = MathHelper.clamp(center + span * 0.5F, -90.0F, -90.0F + openSpan);
            if (end <= start + 1.0F) {
                continue;
            }
            ringBand(buffer, radius - 0.040F, radius + 0.040F, start, end - start,
                    r, g, b, alpha * pulse);
            ringBand(buffer, radius - 0.105F, radius - 0.066F, start + 2.0F,
                    Math.max(1.0F, end - start - 4.0F),
                    1.0F, 0.70F, 0.20F, alpha * pulse * 0.38F);
            ringBand(buffer, radius + 0.066F, radius + 0.105F, start + 2.0F,
                    Math.max(1.0F, end - start - 4.0F),
                    1.0F, 0.70F, 0.20F, alpha * pulse * 0.38F);
        }
    }

    private static void ringBand(BufferBuilder buffer, float inner, float outer,
                                 float startDeg, float spanDeg,
                                 float r, float g, float b, float alpha) {
        int segments = Math.max(1, MathHelper.ceil(RING_SEGMENTS * spanDeg / 360.0F));
        for (int i = 0; i < segments; i++) {
            float f0 = i / (float) segments;
            float f1 = (i + 1) / (float) segments;
            float a0 = (float) Math.toRadians(startDeg + spanDeg * f0);
            float a1 = (float) Math.toRadians(startDeg + spanDeg * f1);
            float c0 = MathHelper.cos(a0);
            float s0 = MathHelper.sin(a0);
            float c1 = MathHelper.cos(a1);
            float s1 = MathHelper.sin(a1);

            quad(buffer,
                    c0 * outer, 0.0F, s0 * outer,
                    c1 * outer, 0.0F, s1 * outer,
                    c1 * inner, 0.0F, s1 * inner,
                    c0 * inner, 0.0F, s0 * inner,
                    r, g, b, alpha);
        }
    }

    private static void softRadialRibbon(BufferBuilder buffer, float angle,
                                         float startRadius, float endRadius, float width,
                                         float r, float g, float b, float alpha) {
        float dx = MathHelper.cos(angle);
        float dz = MathHelper.sin(angle);
        float px = -dz * width * 0.5F;
        float pz = dx * width * 0.5F;
        float pxInner = px * 0.38F;
        float pzInner = pz * 0.38F;

        quadAlpha(buffer,
                dx * startRadius + px, 0.006F, dz * startRadius + pz, alpha * 0.24F,
                dx * endRadius + px, 0.006F, dz * endRadius + pz, alpha * 0.24F,
                dx * endRadius + pxInner, 0.006F, dz * endRadius + pzInner, alpha * 0.24F,
                dx * startRadius + pxInner, 0.006F, dz * startRadius + pzInner, alpha * 0.24F,
                r, g, b);
        quadAlpha(buffer,
                dx * startRadius + pxInner, 0.006F, dz * startRadius + pzInner, alpha,
                dx * endRadius + pxInner, 0.006F, dz * endRadius + pzInner, alpha,
                dx * endRadius - pxInner, 0.006F, dz * endRadius - pzInner, alpha,
                dx * startRadius - pxInner, 0.006F, dz * startRadius - pzInner, alpha,
                r, g, b);
        quadAlpha(buffer,
                dx * startRadius - pxInner, 0.006F, dz * startRadius - pzInner, alpha * 0.24F,
                dx * endRadius - pxInner, 0.006F, dz * endRadius - pzInner, alpha * 0.24F,
                dx * endRadius - px, 0.006F, dz * endRadius - pz, alpha * 0.24F,
                dx * startRadius - px, 0.006F, dz * startRadius - pz, alpha * 0.24F,
                r, g, b);
    }

    private static void colorVerticalCross(BufferBuilder buffer, float x, float z,
                                           float angle, float width, float height,
                                           float r, float g, float b, float alpha) {
        colorVerticalPlane(buffer, x, z, MathHelper.cos(angle), MathHelper.sin(angle),
                width, height, r, g, b, alpha);
        colorVerticalPlane(buffer, x, z, -MathHelper.sin(angle), MathHelper.cos(angle),
                width, height, r, g, b, alpha);
    }

    private static void colorVerticalPlane(BufferBuilder buffer, float x, float z,
                                           float dx, float dz, float width, float height,
                                           float r, float g, float b, float alpha) {
        float hx = dx * width * 0.5F;
        float hz = dz * width * 0.5F;
        quadAlpha(buffer,
                x - hx, 0.02F, z - hz, alpha * 0.50F,
                x - hx, height, z - hz, alpha,
                x + hx, height, z + hz, alpha,
                x + hx, 0.02F, z + hz, alpha * 0.50F,
                r, g, b);
    }

    private static void colorSparkleCross(BufferBuilder buffer,
                                          float x, float y, float z, float angle, float size,
                                          float r, float g, float b, float alpha) {
        colorSparklePlane(buffer, x, y, z, MathHelper.cos(angle), MathHelper.sin(angle),
                size, r, g, b, alpha);
        colorSparklePlane(buffer, x, y, z, -MathHelper.sin(angle), MathHelper.cos(angle),
                size, r, g, b, alpha);
    }

    private static void colorSparklePlane(BufferBuilder buffer,
                                          float x, float y, float z, float dx, float dz, float size,
                                          float r, float g, float b, float alpha) {
        float hx = dx * size;
        float hz = dz * size;
        quadAlpha(buffer,
                x - hx, y - size, z - hz, 0.0F,
                x - hx, y + size, z - hz, alpha,
                x + hx, y + size, z + hz, alpha,
                x + hx, y - size, z + hz, 0.0F,
                r, g, b);
    }

    private static void quad(BufferBuilder buffer,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r, float g, float b, float alpha) {
        vertex(buffer, x0, y0, z0, r, g, b, alpha);
        vertex(buffer, x1, y1, z1, r, g, b, alpha);
        vertex(buffer, x2, y2, z2, r, g, b, alpha);
        vertex(buffer, x3, y3, z3, r, g, b, alpha);
    }

    private static void quadAlpha(BufferBuilder buffer,
                                  float x0, float y0, float z0, float a0,
                                  float x1, float y1, float z1, float a1,
                                  float x2, float y2, float z2, float a2,
                                  float x3, float y3, float z3, float a3,
                                  float r, float g, float b) {
        vertex(buffer, x0, y0, z0, r, g, b, a0);
        vertex(buffer, x1, y1, z1, r, g, b, a1);
        vertex(buffer, x2, y2, z2, r, g, b, a2);
        vertex(buffer, x3, y3, z3, r, g, b, a3);
    }

    private static void vertex(BufferBuilder buffer,
                               float x, float y, float z,
                               float r, float g, float b, float alpha) {
        buffer.pos(x, y, z)
                .color(clamp(r), clamp(g), clamp(b), clamp(alpha))
                .endVertex();
    }

    private static float clamp(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }

    private static float frac(float value) {
        return value - (float) Math.floor(value);
    }
}

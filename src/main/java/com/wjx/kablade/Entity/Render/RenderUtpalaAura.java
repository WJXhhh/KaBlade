package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityUtpalaAura;
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
 * 寒狱冰天的 1.12.2 原生渲染器：以动态几何复刻 1.20 版的冰环、旋涡、冰爆和幻影刀光，
 * 可用时由 GLSL 增加流动噪声与高亮内芯；无 shader 时顶点渐变仍保持完整轮廓。
 */
public class RenderUtpalaAura extends Render<EntityUtpalaAura> {
    private static final float TWO_PI = (float) Math.PI * 2.0F;

    public RenderUtpalaAura(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityUtpalaAura entity) {
        return null;
    }

    @Override
    public void doRender(EntityUtpalaAura entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        float lifeFade = 1.0F - smootherStep(stage(age, 68.0F, 10.0F));
        if (lifeFade <= 0.003F) return;

        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        int previousProgram = UtpalaAuraShader.bind(age, lifeFade);
        boolean pushed = false;
        try {
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

            GlStateManager.pushMatrix();
            pushed = true;
            GlStateManager.translate(x, y + 0.025D, z);
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            renderGroundAura(buffer, age, lifeFade);
            renderVortex(buffer, age, lifeFade);
            renderIceBurstQuads(buffer, age, lifeFade);
            renderPhantomBlade(buffer, age, lifeFade);
            renderResidualStream(buffer, age, lifeFade);
            tessellator.draw();

            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_COLOR);
            renderIceShards(buffer, age, lifeFade);
            tessellator.draw();
        } finally {
            if (pushed) GlStateManager.popMatrix();
            UtpalaAuraShader.restore(previousProgram);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                    lastBrightnessX, lastBrightnessY);
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

    private static void renderGroundAura(BufferBuilder buffer, float age, float lifeFade) {
        float open = smootherStep(stage(age, 1.0F, 8.0F));
        float fade = 1.0F - smootherStep(stage(age, 35.0F, 12.0F));
        float alpha = open * fade * lifeFade;
        if (alpha <= 0.003F) return;
        float pulse = 0.88F + MathHelper.sin(age * 0.34F) * 0.12F;
        disc(buffer, 0.025F, 0.55F, 4.9F * open, 0.02F, 0.16F, 0.34F, alpha * 0.18F, 4.0F);
        ring(buffer, 0.045F, 2.45F + open * 1.35F, 0.20F,
                0.08F, 0.62F, 1.0F, alpha * 0.72F * pulse, 0.0F);
        ring(buffer, 0.055F, 4.15F + open * 0.42F, 0.075F,
                0.72F, 0.96F, 1.0F, alpha * 0.94F, 1.0F);
        ring(buffer, 0.07F, 5.25F * open, 0.14F,
                0.05F, 0.48F, 1.0F, alpha * 0.62F, 0.0F);
    }

    private static void renderVortex(BufferBuilder buffer, float age, float lifeFade) {
        float build = smootherStep(stage(age, 4.0F, 16.0F));
        float snap = smootherStep(stage(age, 18.0F, 8.0F));
        float fade = 1.0F - smootherStep(stage(age, 34.0F, 9.0F));
        float alpha = build * fade * lifeFade;
        if (alpha <= 0.003F) return;

        for (int arm = 0; arm < 7; arm++) {
            float phase = arm * TWO_PI / 7.0F + age * (0.115F + arm * 0.002F);
            spiral(buffer, phase, 2.3F + snap * 2.8F, 0.20F + snap * 0.17F,
                    0.12F + arm * 0.045F, alpha * (0.36F + snap * 0.30F), arm);
        }
        for (int layer = 0; layer < 4; layer++) {
            float radius = 1.1F + layer * 0.92F + snap * (0.5F + layer * 0.25F);
            float y = 0.30F + layer * 0.46F;
            ring(buffer, y, radius, 0.10F + layer * 0.025F,
                    0.12F, 0.72F, 1.0F,
                    alpha * (0.38F - layer * 0.040F), 0.0F);
        }
    }

    private static void renderIceBurstQuads(BufferBuilder buffer, float age, float lifeFade) {
        float burst = smootherStep(stage(age, 26.0F, 3.5F))
                * (1.0F - smootherStep(stage(age, 37.0F, 12.0F))) * lifeFade;
        if (burst <= 0.003F) return;
        float expansion = fastOut(stage(age, 27.0F, 8.0F));
        // 少量主冰棱组成清晰的爆发轮廓，避免近距离被大量半透明三角面遮满。
        for (int i = 0; i < 7; i++) {
            float angle = (i - 3) * 0.31F;
            float radius = 1.5F + expansion * (1.5F + i * 0.48F);
            float height = 0.72F + (3 - Math.abs(i - 3)) * 0.24F;
            float width = 0.10F + (3 - Math.abs(i - 3)) * 0.018F;
            crystal(buffer, MathHelper.sin(angle) * radius, 0.02F,
                    MathHelper.cos(angle) * radius, angle, height, width,
                    burst * (0.34F + (3 - Math.abs(i - 3)) * 0.055F));
        }
        ring(buffer, 0.09F, 1.0F + expansion * 6.0F, 0.16F + expansion * 0.36F,
                0.54F, 0.94F, 1.0F, burst * (1.0F - expansion) * 0.92F, 3.0F);
    }

    private static void renderPhantomBlade(BufferBuilder buffer, float age, float lifeFade) {
        float enter = smootherStep(stage(age, 34.0F, 3.0F));
        float fade = 1.0F - smootherStep(stage(age, 46.0F, 12.0F));
        float alpha = enter * fade * lifeFade;
        if (alpha <= 0.003F) return;
        float thrust = fastOut(stage(age, 36.0F, 7.0F));
        float head = 2.2F + thrust * 10.8F;
        float tail = -0.4F + thrust * 3.4F;
        bladeRibbon(buffer, tail, head, 1.12F, 0.74F, alpha * 0.48F,
                0.04F, 0.35F, 1.0F, 1.0F);
        bladeRibbon(buffer, tail + 0.25F, head + 0.25F, 1.14F, 0.27F, alpha,
                0.80F, 0.98F, 1.0F, 1.0F);
        for (int i = 0; i < 5; i++) {
            float side = (i - 2) * 0.62F;
            bladeRibbon(buffer, tail - i * 0.12F, head - 1.0F - i * 0.30F,
                    0.70F + Math.abs(side) * 0.16F, 0.07F,
                    alpha * (0.26F - Math.abs(i - 2) * 0.025F),
                    0.06F, 0.58F, 1.0F, 1.0F, side);
        }
    }

    private static void renderResidualStream(BufferBuilder buffer, float age, float lifeFade) {
        float enter = smootherStep(stage(age, 40.0F, 4.0F));
        float fade = 1.0F - smootherStep(stage(age, 64.0F, 10.0F));
        float alpha = enter * fade * lifeFade;
        if (alpha <= 0.003F) return;
        for (int i = 0; i < 9; i++) {
            float side = (random(i, 12.7F) - 0.5F) * 5.8F;
            float y = 0.28F + random(i, 15.2F) * 2.1F;
            float drift = frac(age * (0.025F + random(i, 18.1F) * 0.018F) + random(i, 20.0F));
            float z0 = 1.2F + drift * 10.0F;
            float length = 1.4F + random(i, 21.8F) * 2.8F;
            softQuad(buffer, new Vec(side - 0.025F, y, z0 - length),
                    new Vec(side + 0.025F, y, z0 - length),
                    new Vec(side + 0.08F, y + 0.04F, z0),
                    new Vec(side - 0.08F, y - 0.04F, z0),
                    0.10F, 0.68F, 1.0F, alpha * 0.30F, 1.0F);
        }
    }

    private static void renderIceShards(BufferBuilder buffer, float age, float lifeFade) {
        float burst = smootherStep(stage(age, 27.0F, 2.0F))
                * (1.0F - smootherStep(stage(age, 48.0F, 17.0F))) * lifeFade;
        if (burst <= 0.003F) return;
        float expansion = fastOut(stage(age, 28.0F, 15.0F));
        for (int i = 0; i < 8; i++) {
            float angle = i * 2.399963F + random(i, 25.1F);
            float radius = 1.0F + expansion * (2.0F + random(i, 27.4F) * 7.2F);
            float y = 0.22F + random(i, 29.2F) * 3.0F + expansion * random(i, 30.7F) * 1.8F;
            float size = 0.035F + random(i, 32.6F) * 0.065F;
            Vec center = new Vec(MathHelper.sin(angle) * radius, y, MathHelper.cos(angle) * radius);
            triangle(buffer, center.add(-size, -size, 0.0F), center.add(size, -size * 0.35F, 0.0F),
                    center.add(0.0F, size * 2.4F, 0.0F),
                    0.48F, 0.93F, 1.0F, burst * (0.14F + random(i, 34.1F) * 0.24F), 2.0F);
        }
    }

    private static void spiral(BufferBuilder buffer, float phase, float radius, float width,
                               float lift, float alpha, int seed) {
        int segments = 34;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = phase + t0 * (TWO_PI * 1.45F);
            float a1 = phase + t1 * (TWO_PI * 1.45F);
            float r0 = radius * (0.20F + t0 * 0.80F);
            float r1 = radius * (0.20F + t1 * 0.80F);
            Vec p0 = new Vec(MathHelper.sin(a0) * r0, 0.10F + t0 * lift * 8.0F,
                    MathHelper.cos(a0) * r0);
            Vec p1 = new Vec(MathHelper.sin(a1) * r1, 0.10F + t1 * lift * 8.0F,
                    MathHelper.cos(a1) * r1);
            float w0 = width * (0.24F + MathHelper.sin(t0 * (float) Math.PI) * 0.76F);
            float w1 = width * (0.24F + MathHelper.sin(t1 * (float) Math.PI) * 0.76F);
            float a = alpha * edgeFade((t0 + t1) * 0.5F);
            quad(buffer, p0.add(0.0F, w0, 0.0F), p1.add(0.0F, w1, 0.0F),
                    p1.add(0.0F, -w1, 0.0F), p0.add(0.0F, -w0, 0.0F),
                    0.08F, 0.70F, 1.0F, a, 0.0F, t0, t1);
        }
    }

    private static void ring(BufferBuilder buffer, float y, float radius, float width,
                             float r, float g, float b, float alpha, float kind) {
        int segments = 72;
        float outer = radius + width * 0.5F;
        float inner = Math.max(0.01F, radius - width * 0.5F);
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments, t1 = (i + 1) / (float) segments;
            float a0 = t0 * TWO_PI, a1 = t1 * TWO_PI;
            quad(buffer, new Vec(MathHelper.cos(a0) * outer, y, MathHelper.sin(a0) * outer),
                    new Vec(MathHelper.cos(a1) * outer, y, MathHelper.sin(a1) * outer),
                    new Vec(MathHelper.cos(a1) * inner, y, MathHelper.sin(a1) * inner),
                    new Vec(MathHelper.cos(a0) * inner, y, MathHelper.sin(a0) * inner),
                    r, g, b, alpha, kind, t0, t1);
        }
    }

    private static void disc(BufferBuilder buffer, float y, float inner, float outer,
                             float r, float g, float b, float alpha, float kind) {
        int bands = 7;
        for (int band = 0; band < bands; band++) {
            float p0 = band / (float) bands, p1 = (band + 1) / (float) bands;
            float radius = inner + (outer - inner) * (p0 + p1) * 0.5F;
            float width = (outer - inner) / bands;
            float edge = MathHelper.sin((p0 + p1) * 0.5F * (float) Math.PI);
            ring(buffer, y, radius, width, r, g, b, alpha * edge, kind);
        }
    }

    private static void crystal(BufferBuilder buffer, float x, float y, float z, float angle,
                                float height, float width, float alpha) {
        Vec right = new Vec(MathHelper.cos(angle) * width, 0.0F, -MathHelper.sin(angle) * width);
        Vec base = new Vec(x, y, z);
        Vec tip = new Vec(x + MathHelper.sin(angle) * height * 0.18F, y + height,
                z + MathHelper.cos(angle) * height * 0.18F);
        Vec mid = base.add(0.0F, height * 0.15F, 0.0F);
        quad(buffer, mid.add(right.scale(-1.0F)), tip, mid.add(right), base,
                0.34F, 0.88F, 1.0F, alpha, 2.0F, 0.0F, 1.0F);
    }

    private static void bladeRibbon(BufferBuilder buffer, float tail, float head, float y,
                                    float halfWidth, float alpha, float r, float g, float b, float kind) {
        bladeRibbon(buffer, tail, head, y, halfWidth, alpha, r, g, b, kind, 0.0F);
    }

    private static void bladeRibbon(BufferBuilder buffer, float tail, float head, float y,
                                    float halfWidth, float alpha, float r, float g, float b,
                                    float kind, float side) {
        Vec a = new Vec(side - halfWidth * 0.10F, y - halfWidth, tail);
        Vec b0 = new Vec(side, y, head);
        Vec c = new Vec(side + halfWidth * 0.10F, y + halfWidth, tail);
        Vec d = new Vec(side, y, tail - 0.25F);
        quad(buffer, a, b0, c, d, r, g, b, alpha, kind, 0.0F, 1.0F);
    }

    private static void softQuad(BufferBuilder buffer, Vec a, Vec b, Vec c, Vec d,
                                 float r, float g, float blue, float alpha, float kind) {
        quad(buffer, a, b, c, d, r, g, blue, alpha, kind, 0.0F, 1.0F);
    }

    private static void quad(BufferBuilder buffer, Vec a, Vec b, Vec c, Vec d,
                             float r, float g, float blue, float alpha,
                             float kind, float u0, float u1) {
        // 四点使用同一透明度，让相邻几何段无缝衔接；横向羽化交给 UV/GLSL 完成。
        vertex(buffer, a, r, g, blue, alpha, kind * 2.0F + u0 * 2.0F, 0.0F);
        vertex(buffer, b, r, g, blue, alpha, kind * 2.0F + u1 * 2.0F, 0.0F);
        vertex(buffer, c, 0.86F, 0.98F, 1.0F, alpha, kind * 2.0F + u1 * 2.0F, 1.0F);
        vertex(buffer, d, 0.86F, 0.98F, 1.0F, alpha, kind * 2.0F + u0 * 2.0F, 1.0F);
    }

    private static void triangle(BufferBuilder buffer, Vec a, Vec b, Vec c,
                                 float r, float g, float blue, float alpha, float kind) {
        vertex(buffer, a, r, g, blue, alpha * 0.20F, kind * 2.0F, 0.0F);
        vertex(buffer, b, r, g, blue, alpha * 0.72F, kind * 2.0F + 2.0F, 0.0F);
        vertex(buffer, c, 0.92F, 1.0F, 1.0F, alpha, kind * 2.0F + 1.0F, 1.0F);
    }

    private static void vertex(BufferBuilder buffer, Vec value, float r, float g, float b,
                               float alpha, float u, float v) {
        buffer.pos(value.x, value.y, value.z).tex(u, v)
                .color(clamp(r), clamp(g), clamp(b), clamp(alpha)).endVertex();
    }

    private static float stage(float age, float start, float duration) {
        return MathHelper.clamp((age - start) / duration, 0.0F, 1.0F);
    }

    private static float smootherStep(float value) {
        value = MathHelper.clamp(value, 0.0F, 1.0F);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    private static float fastOut(float value) {
        value = MathHelper.clamp(value, 0.0F, 1.0F);
        float inv = 1.0F - value;
        return 1.0F - inv * inv * inv * inv;
    }

    private static float edgeFade(float value) {
        return smootherStep(MathHelper.clamp(value / 0.14F, 0.0F, 1.0F))
                * (1.0F - smootherStep(MathHelper.clamp((value - 0.82F) / 0.18F, 0.0F, 1.0F)));
    }

    private static float random(int index, float salt) {
        return frac(MathHelper.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    private static float frac(float value) {
        return value - (float) Math.floor(value);
    }

    private static float clamp(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }

    private static final class Vec {
        final float x, y, z;
        Vec(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        Vec add(float dx, float dy, float dz) { return new Vec(x + dx, y + dy, z + dz); }
        Vec add(Vec value) { return add(value.x, value.y, value.z); }
        Vec scale(float value) { return new Vec(x * value, y * value, z * value); }
    }
}

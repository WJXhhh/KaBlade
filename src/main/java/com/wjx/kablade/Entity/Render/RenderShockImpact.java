package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityShockImpact;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * 震荡冲击的 1.12.2 渲染器。
 * 复刻 1.20 版的三维扭转弧带、残影、速度线和能量碎片；绘制接口换成
 * Tessellator/BufferBuilder，并在可用时叠加 GLSL 1.20 青色辉光。
 */
public class RenderShockImpact extends Render<EntityShockImpact> {
    private static final int TRAIL_SEGMENTS = 56;
    private static final float ARC_RADIUS = 3.05F;
    private static final float ARC_CENTER_Z = -0.05F;
    private static final float ARC_START = (float) Math.toRadians(54.0D);
    private static final float ARC_END = (float) Math.toRadians(-140.0D);
    private static final float ARC_START_Y = -0.95F;
    private static final float ARC_END_Y = 1.24F;
    private static final float ARC_LIFT = 0.48F;
    private static final float RIBBON_HALFWIDTH = 0.66F;
    private static final float TWIST_AMP = 0.70F;

    public RenderShockImpact(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityShockImpact entity) { return null; }

    @Override
    public void doRender(EntityShockImpact entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        float life = Math.max(1.0F, entity.getLifetime());
        float progress = MathHelper.clamp(age / life, 0.0F, 1.0F);
        float open = smootherStep(MathHelper.clamp(age / 2.6F, 0.0F, 1.0F));
        float fade = 1.0F - smootherStep(MathHelper.clamp((progress - 0.50F) / 0.50F, 0.0F, 1.0F));
        float alpha = open * fade;
        if (alpha <= 0.004F) return;

        float sweep = smootherStep(MathHelper.clamp((age - 0.35F) / 6.4F, 0.0F, 1.0F));
        float spark = MathHelper.sin(MathHelper.clamp((age - 1.0F) / 9.0F, 0.0F, 1.0F)
                * (float) Math.PI);
        float scale = entity.getScale() * (0.94F + sweep * 0.14F);
        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        int previousProgram = ShockImpactShader.bind(age, alpha);
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
            Entity owner = entity.getOwner();
            if (owner != null) {
                double entX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
                double entY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
                double entZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
                double ownX = owner.prevPosX + (owner.posX - owner.prevPosX) * partialTicks;
                double ownY = owner.prevPosY + (owner.posY - owner.prevPosY) * partialTicks;
                double ownZ = owner.prevPosZ + (owner.posZ - owner.prevPosZ) * partialTicks;
                float yaw = entity.rotationYaw * 0.017453292F;
                double fx = -MathHelper.sin(yaw);
                double fz = MathHelper.cos(yaw);
                x += ownX + fx * entity.getForwardOffset() - entX;
                y += ownY + entity.getUpOffset() - entY;
                z += ownZ + fz * entity.getForwardOffset() - entZ;
            }
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0D, -0.18D + spark * 0.08D, 0.18D);
            GlStateManager.scale(scale, scale, scale);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            for (int i = 2; i >= 0; i--) {
                float imageAlpha = alpha * (0.050F + i * 0.020F) * (1.0F - progress * 0.58F);
                trail(buffer, sweep, 0.090F + i * 0.009F, -0.05F - i * 0.040F,
                        -0.030F - i * 0.020F, 0.04F, 0.48F, 1.0F, imageAlpha);
            }
            trail(buffer, sweep, 0.16F, -0.02F, 0.0F,
                    0.05F, 0.58F, 1.0F, alpha * 0.34F);
            trail(buffer, sweep, 0.105F, 0.0F, 0.0F,
                    0.12F, 0.90F, 1.0F, alpha * 0.86F);
            trail(buffer, sweep, 0.040F, 0.0F, 0.025F,
                    0.92F, 1.0F, 1.0F, alpha);
            edgeFlare(buffer, sweep, alpha * (0.78F + spark * 0.34F));
            speedLines(buffer, age, sweep, alpha, spark);
            tessellator.draw();

            // 裂片使用真正的三角形，避免无纹理回退时暴露方形 QUAD 轮廓。
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            fragments(buffer, age, sweep, alpha, spark);
            tessellator.draw();
        } finally {
            if (pushed) GlStateManager.popMatrix();
            ShockImpactShader.restore(previousProgram);
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

    private static void trail(BufferBuilder buffer, float sweep, float width,
                              float sideOffset, float depthOffset,
                              float r, float g, float b, float alpha) {
        float end = visibleEnd(sweep);
        int count = Math.max(2, MathHelper.ceil(TRAIL_SEGMENTS * end));
        Vec w0 = new Vec(), b0 = new Vec(), w1 = new Vec(), b1 = new Vec();
        for (int i = 0; i < count; i++) {
            float t0 = i / (float) TRAIL_SEGMENTS;
            float t1 = Math.min((i + 1) / (float) TRAIL_SEGMENTS, end);
            if (t0 > end) break;
            Vec c0 = center(t0), c1 = center(t1);
            frame(t0, w0, b0); frame(t1, w1, b1);
            float width0 = width * (0.35F + trailWidth(t0) * 0.65F);
            float width1 = width * (0.35F + trailWidth(t1) * 0.65F);
            float a0 = alpha * trailAlpha(t0, end), a1 = alpha * trailAlpha(t1, end);
            Vec p0 = c0.add(w0.scale(sideOffset)).add(b0.scale(depthOffset));
            Vec p1 = c1.add(w1.scale(sideOffset)).add(b1.scale(depthOffset));
            // 四条同心带从透明外缘过渡到明亮内芯。即使 GLSL 不可用，
            // 固定管线的顶点 alpha 插值也不会显示出实心矩形边界。
            softBand(buffer, p0, p1, b0, b1, width0, width1, a0, a1, r, g, b);
        }
    }

    private static void softBand(BufferBuilder buffer, Vec p0, Vec p1, Vec b0, Vec b1,
                                 float width0, float width1, float a0, float a1,
                                 float r, float g, float blue) {
        float[] offsets = {-1.0F, -0.38F, 0.38F, 1.0F};
        float[] opacity = {0.0F, 1.0F, 1.0F, 0.0F};
        for (int i = 0; i < offsets.length - 1; i++) {
            Vec a = p0.add(b0.scale(width0 * offsets[i]));
            Vec b = p1.add(b1.scale(width1 * offsets[i]));
            Vec c = p1.add(b1.scale(width1 * offsets[i + 1]));
            Vec d = p0.add(b0.scale(width0 * offsets[i + 1]));
            quad(buffer, a, a0 * opacity[i], b, a1 * opacity[i],
                    c, a1 * opacity[i + 1], d, a0 * opacity[i + 1], r, g, blue);
        }
    }

    private static void edgeFlare(BufferBuilder buffer, float sweep, float alpha) {
        float t = MathHelper.clamp(visibleEnd(sweep) - 0.025F, 0.0F, 1.0F);
        Vec w = new Vec(), b = new Vec(); frame(t, w, b);
        Vec c = center(t), tangent = tangent(t);
        Vec p = c.add(w.scale(RIBBON_HALFWIDTH * trailWidth(t)));
        float len = 0.86F + sweep * 0.68F, width = 0.052F;
        quad(buffer, p.add(tangent.scale(-len * 0.32F)).add(b.scale(width)), alpha * 0.34F,
                p.add(tangent.scale(len)).add(b.scale(width * 0.35F)), alpha,
                p.add(tangent.scale(len)).add(b.scale(-width * 0.35F)), alpha,
                p.add(tangent.scale(-len * 0.32F)).add(b.scale(-width)), alpha * 0.34F,
                0.70F, 0.96F, 1.0F);
    }

    private static void speedLines(BufferBuilder buffer, float age, float sweep,
                                   float alpha, float spark) {
        float end = visibleEnd(sweep);
        Vec w = new Vec(), b = new Vec();
        for (int i = 0; i < 26; i++) {
            float t = MathHelper.clamp(0.20F + random(i, 2.4F) * 0.74F, 0.0F, end);
            frame(t, w, b);
            Vec tangent = tangent(t);
            float side = (random(i, 3.8F) - 0.34F) * RIBBON_HALFWIDTH * 1.30F;
            float drift = frac(age * 0.090F + random(i, 7.1F));
            float length = 0.74F + random(i, 8.9F) * 1.42F + spark * 0.52F;
            float width = 0.014F + random(i, 10.6F) * 0.018F;
            Vec p = center(t).add(w.scale(side)).add(tangent.scale(drift * 0.48F));
            float a = alpha * (0.30F + spark * 0.22F);
            quad(buffer, p.add(tangent.scale(-length)).add(b.scale(width)), a * 0.20F,
                    p.add(tangent.scale(length * 0.20F)).add(b.scale(width)), a,
                    p.add(tangent.scale(length * 0.20F)).add(b.scale(-width)), a,
                    p.add(tangent.scale(-length)).add(b.scale(-width)), a * 0.20F,
                    0.10F, 0.70F, 1.0F);
        }
    }

    private static void fragments(BufferBuilder buffer, float age, float sweep,
                                  float alpha, float spark) {
        float end = visibleEnd(sweep);
        Vec w = new Vec(), b = new Vec();
        for (int i = 0; i < 11; i++) {
            float t = MathHelper.clamp(0.40F + random(i, 12.3F) * 0.54F, 0.0F, end);
            float life = frac(age * 0.052F + random(i, 14.7F));
            frame(t, w, b);
            Vec tangent = tangent(t);
            float sign = (i & 1) == 0 ? 1.0F : -1.0F;
            float side = sign * (RIBBON_HALFWIDTH * (0.72F + random(i, 16.2F) * 0.68F)
                    + life * (0.16F + spark * 0.20F));
            float depth = (random(i, 17.4F) - 0.5F) * 0.30F;
            Vec p = center(t).add(w.scale(side)).add(b.scale(depth))
                    .add(tangent.scale(life * (0.30F + random(i, 20.1F) * 0.45F)));
            float length = 0.11F + random(i, 18.5F) * 0.19F;
            float width = 0.018F + random(i, 19.1F) * 0.030F;
            float a = alpha * (1.0F - smootherStep(life))
                    * (0.28F + random(i, 19.7F) * 0.38F);
            triangle(buffer,
                    p.add(tangent.scale(-length)), a * 0.06F,
                    p.add(tangent.scale(length * 0.72F)).add(b.scale(width)), a,
                    p.add(tangent.scale(length * 0.34F)).add(b.scale(-width)), a * 0.54F,
                    0.42F, 0.92F, 1.0F);
        }
    }

    private static float visibleEnd(float sweep) {
        return MathHelper.clamp(0.10F + sweep * 0.98F, 0.0F, 1.0F);
    }

    private static Vec center(float t) {
        float angle = ARC_START + (ARC_END - ARC_START) * t;
        float y = ARC_START_Y + (ARC_END_Y - ARC_START_Y) * t
                + MathHelper.sin(t * (float) Math.PI) * ARC_LIFT;
        return new Vec(MathHelper.sin(angle) * ARC_RADIUS, y,
                MathHelper.cos(angle) * ARC_RADIUS + ARC_CENTER_Z);
    }

    private static void frame(float t, Vec outW, Vec outB) {
        Vec tan = tangent(t);
        float angle = ARC_START + (ARC_END - ARC_START) * t;
        Vec w = new Vec(MathHelper.sin(angle), 0.0F, MathHelper.cos(angle)).normalize();
        Vec b = tan.cross(w).normalize();
        float phi = TWIST_AMP * MathHelper.sin((t - 0.5F) * (float) Math.PI);
        float cp = MathHelper.cos(phi), sp = MathHelper.sin(phi);
        outW.set(w.scale(cp).add(b.scale(-sp)));
        outB.set(w.scale(sp).add(b.scale(cp)));
    }

    private static Vec tangent(float t) {
        return center(MathHelper.clamp(t + 0.012F, 0.0F, 1.0F))
                .add(center(MathHelper.clamp(t - 0.012F, 0.0F, 1.0F)).scale(-1.0F)).normalize();
    }

    private static float trailWidth(float t) {
        float body = MathHelper.sin(t * (float) Math.PI);
        float tail = 1.0F - smootherStep(MathHelper.clamp((t - 0.82F) / 0.18F, 0.0F, 1.0F));
        return (0.18F + (float) Math.pow(body, 0.58D) * 0.68F) * (0.72F + tail * 0.28F);
    }

    private static float trailAlpha(float t, float end) {
        float head = smootherStep(MathHelper.clamp(t / Math.max(0.001F, end), 0.0F, 1.0F));
        float tail = smootherStep(MathHelper.clamp(t / 0.22F, 0.0F, 1.0F));
        float endFade = 1.0F - smootherStep(MathHelper.clamp((t - 0.88F) / 0.12F, 0.0F, 1.0F));
        return (0.35F + head * 0.65F) * tail * endFade;
    }

    private static void quad(BufferBuilder buffer, Vec p0, float a0, Vec p1, float a1,
                             Vec p2, float a2, Vec p3, float a3,
                             float r, float g, float b) {
        vertex(buffer, p0, r, g, b, a0); vertex(buffer, p1, r, g, b, a1);
        vertex(buffer, p2, r, g, b, a2); vertex(buffer, p3, r, g, b, a3);
    }

    private static void triangle(BufferBuilder buffer, Vec p0, float a0, Vec p1, float a1,
                                 Vec p2, float a2, float r, float g, float b) {
        vertex(buffer, p0, r, g, b, a0); vertex(buffer, p1, r, g, b, a1);
        vertex(buffer, p2, r, g, b, a2);
    }

    private static void vertex(BufferBuilder buffer, Vec p, float r, float g, float b, float a) {
        buffer.pos(p.x, p.y, p.z).color(clamp(r), clamp(g), clamp(b), clamp(a)).endVertex();
    }

    private static float smootherStep(float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float random(int index, float salt) {
        return frac(MathHelper.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }
    private static float frac(float value) { return value - (float) Math.floor(value); }
    private static float clamp(float value) { return MathHelper.clamp(value, 0.0F, 1.0F); }

    private static final class Vec {
        float x, y, z;
        Vec() {}
        Vec(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        Vec set(Vec value) { x = value.x; y = value.y; z = value.z; return this; }
        Vec add(Vec value) { return new Vec(x + value.x, y + value.y, z + value.z); }
        Vec scale(float value) { return new Vec(x * value, y * value, z * value); }
        Vec cross(Vec value) { return new Vec(y * value.z - z * value.y,
                z * value.x - x * value.z, x * value.y - y * value.x); }
        Vec normalize() {
            float length = MathHelper.sqrt(x * x + y * y + z * z);
            return length > 1.0E-5F ? scale(1.0F / length) : new Vec();
        }
    }
}

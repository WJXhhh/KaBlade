package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityZaizan;
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
 * 罪斩的 1.12.2 程序化渲染器。
 * 以 Tessellator 复刻 1.20 的红白横斩、羽状裂光、充能火星与碎片；支持 GLSL 1.20，
 * 不支持 shader 或外部光影已占用 program 时则使用较收敛的固定管线几何。
 */
public class RenderZaizan extends Render<EntityZaizan> {
    private static final int SHARD_COUNT = 30;
    private static final int STREAK_COUNT = 18;
    private static final int CHARGE_DOT_COUNT = 10;

    public RenderZaizan(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityZaizan entity) { return null; }

    @Override
    public void doRender(EntityZaizan entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        float life = Math.max(1.0F, entity.getLifetime());
        float progress = MathHelper.clamp(age / life, 0.0F, 1.0F);
        float intro = smootherStep(MathHelper.clamp(age / 3.0F, 0.0F, 1.0F));
        float outro = 1.0F - smootherStep(MathHelper.clamp((progress - 0.78F) / 0.22F, 0.0F, 1.0F));
        float alpha = intro * outro;
        if (alpha <= 0.004F) return;

        float slashT = MathHelper.clamp((age - 5.0F) / 9.0F, 0.0F, 1.0F);
        float burst = smootherStep(slashT)
                * (1.0F - smootherStep(MathHelper.clamp((slashT - 0.74F) / 0.26F, 0.0F, 1.0F)));
        float flash = MathHelper.sin(MathHelper.clamp((age - 6.0F) / 7.0F, 0.0F, 1.0F)
                * (float) Math.PI);
        float charge = alpha
                * (1.0F - smootherStep(MathHelper.clamp((age - 8.0F) / 5.0F, 0.0F, 1.0F)));
        float after = alpha * smootherStep(MathHelper.clamp((age - 11.0F) / 5.0F, 0.0F, 1.0F))
                * (1.0F - smootherStep(MathHelper.clamp((progress - 0.78F) / 0.18F, 0.0F, 1.0F)));
        float scale = entity.getScale() * (1.34F + burst * 0.36F);

        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        float shaderTime = (float) (((entity.world.getTotalWorldTime() + partialTicks) % 24000.0D)
                / 24000.0D * 210.0D);
        int previousProgram = ZaizanShader.bind(shaderTime);
        boolean shader = ZaizanShader.isBound();
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
                double entX = lerp(partialTicks, entity.prevPosX, entity.posX);
                double entY = lerp(partialTicks, entity.prevPosY, entity.posY);
                double entZ = lerp(partialTicks, entity.prevPosZ, entity.posZ);
                double ownX = lerp(partialTicks, owner.prevPosX, owner.posX);
                double ownY = lerp(partialTicks, owner.prevPosY, owner.posY);
                double ownZ = lerp(partialTicks, owner.prevPosZ, owner.posZ);
                float yaw = entity.rotationYaw * 0.017453292F;
                x += ownX - MathHelper.sin(yaw) * entity.getForwardOffset() - entX;
                y += ownY + entity.getUpOffset() - entY;
                z += ownZ + MathHelper.cos(yaw) * entity.getForwardOffset() - entZ;
            }
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0D, 0.0D, 0.55D);
            GlStateManager.scale(scale, scale, scale);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            preFlash(buffer, age, charge);
            impactEmbers(buffer, age, charge + after * 0.48F, flash);
            if (burst > 0.001F) {
                if (shader) {
                    screenWash(buffer, burst, flash, alpha);
                    mainBladeFlash(buffer, slashT, burst, flash, alpha);
                    redFeatherSlashes(buffer, slashT, burst, alpha);
                } else {
                    fallbackBladeBurst(buffer, slashT, burst, flash, alpha);
                }
            }
            sparksAndShards(buffer, age, progress, alpha, burst * 0.55F + after * 0.40F, flash);
            tessellator.draw();
        } finally {
            if (pushed) GlStateManager.popMatrix();
            ZaizanShader.restore(previousProgram);
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

    private static void preFlash(BufferBuilder buffer, float age, float alpha) {
        if (alpha <= 0.003F) return;
        float pulse = 0.64F + 0.36F * MathHelper.sin(age * 0.72F);
        for (int i = 0; i < CHARGE_DOT_COUNT; i++) {
            float t = i / (float) (CHARGE_DOT_COUNT - 1);
            float side = random(i, 4.8F) * 2.0F - 1.0F;
            diamond(buffer, -0.44F + t * 0.96F + side * 0.06F,
                    -0.18F + random(i, 8.3F) * 0.72F,
                    0.78F + random(i, 9.6F) * 0.42F,
                    0.030F + random(i, 11.1F) * 0.044F, age * 0.14F + i * 0.37F,
                    1.0F, 0.04F, 0.035F, alpha * pulse * (0.32F + random(i, 12.7F) * 0.42F));
        }
        for (int i = 0; i < 7; i++) {
            float yp = -0.28F + random(i, 17.2F) * 1.04F;
            float zp = 0.70F + random(i, 19.7F) * 0.68F;
            float xp = -1.04F + random(i, 21.5F) * 2.10F;
            float len = 0.34F + random(i, 23.1F) * 0.72F;
            ribbon(buffer, xp - 0.18F, yp - 0.014F, zp, xp + len, yp + 0.045F, zp + 0.035F,
                    0.018F + random(i, 24.9F) * 0.015F,
                    1.0F, 0.09F, 0.04F, alpha * (0.26F + random(i, 26.2F) * 0.24F), 0.0F);
        }
    }

    private static void impactEmbers(BufferBuilder buffer, float age, float alpha, float flash) {
        if (alpha <= 0.003F) return;
        for (int i = 0; i < 9; i++) {
            float angle = i * (float) Math.PI * 2.0F / 9.0F + age * 0.035F;
            float radius = 0.36F + random(i, 61.3F) * 0.58F + flash * 0.16F;
            diamond(buffer, MathHelper.cos(angle) * radius,
                    -0.12F + MathHelper.sin(angle) * radius * 0.54F + random(i, 62.9F) * 0.28F,
                    0.88F + random(i, 64.4F) * 0.54F,
                    0.035F + random(i, 65.8F) * 0.075F + flash * 0.025F,
                    -age * 0.16F + i * 0.72F, 1.0F, 0.05F, 0.04F,
                    alpha * (0.72F + flash * 0.52F) * (0.28F + random(i, 66.7F) * 0.44F));
        }
    }

    private static void screenWash(BufferBuilder b, float burst, float flash, float alpha) {
        float a = alpha * burst * (0.42F + flash * 0.42F);
        ribbon(b, -12.0F, 2.72F, 1.72F, 12.5F, 3.00F, 1.42F,
                2.65F, 1.0F, 0.10F, 0.08F, a, 8.0F);
        ribbon(b, -11.4F, 1.88F, 1.32F, 11.8F, 2.36F, 1.04F,
                1.75F, 1.0F, 0.05F, 0.045F, a * 0.76F, 8.0F);
        ribbon(b, -10.2F, 0.74F, 0.92F, 10.8F, 1.40F, 0.76F,
                1.10F, 1.0F, 0.035F, 0.035F, a * 0.48F, 8.0F);
    }

    private static void fallbackBladeBurst(BufferBuilder b, float slashT, float burst,
                                           float flash, float alpha) {
        float travel = smootherStep(slashT);
        float shift = -1.62F + travel * 2.70F;
        float lift = 0.08F + flash * 0.18F;
        float a = alpha * burst;
        ribbon(b, -8.6F + shift, -0.10F + lift, 1.05F, 8.9F + shift, 0.92F + lift, 1.06F,
                0.46F + flash * 0.10F, 1.0F, 0.06F, 0.035F, a * 0.32F, 2.0F);
        ribbon(b, -8.2F + shift, 0.02F + lift, 1.09F, 8.4F + shift, 0.80F + lift, 1.10F,
                0.18F + flash * 0.05F, 1.0F, 0.44F, 0.20F, a * 0.52F, 2.0F);
        ribbon(b, -7.6F + shift, 0.13F + lift, 1.13F, 7.8F + shift, 0.68F + lift, 1.14F,
                0.055F + flash * 0.025F, 1.0F, 0.88F, 0.62F, a * 0.76F, 2.0F);
    }

    private static void mainBladeFlash(BufferBuilder b, float slashT, float burst,
                                       float flash, float alpha) {
        float travel = smootherStep(slashT);
        float shift = -1.70F + travel * 2.95F;
        float lift = 0.12F + flash * 0.30F;
        float z = 1.06F + flash * 0.10F;
        float a = alpha * burst;
        ribbon(b, -12.6F + shift, -0.38F + lift, z, 13.2F + shift, 1.26F + lift, z + 0.02F,
                2.28F + flash * 0.70F, 1.0F, 0.035F, 0.028F, a * (0.72F + flash * 0.44F), 2.0F);
        ribbon(b, -11.9F + shift, -0.23F + lift, z + 0.03F, 12.8F + shift, 1.12F + lift, z + 0.05F,
                1.24F + flash * 0.36F, 1.0F, 0.26F, 0.16F, a * (0.92F + flash * 0.54F), 2.0F);
        ribbon(b, -11.0F + shift, -0.10F + lift, z + 0.06F, 12.0F + shift, 0.99F + lift, z + 0.08F,
                0.52F + flash * 0.20F, 1.0F, 0.86F, 0.72F, a * (1.18F + flash * 0.86F), 2.0F);
        ribbon(b, -10.4F + shift, 0.03F + lift, z + 0.10F, 11.3F + shift, 0.86F + lift, z + 0.12F,
                0.18F + flash * 0.08F, 1.0F, 0.98F, 0.92F, a * (1.36F + flash * 1.08F), 2.0F);
        for (int i = 0; i < STREAK_COUNT; i++) {
            float lane = random(i, 30.1F) * 2.0F - 1.0F;
            float x0 = -7.6F + random(i, 31.4F) * 14.0F + travel * 2.2F;
            float len = 0.8F + random(i, 33.2F) * 2.8F + flash;
            float yp = -0.74F + random(i, 34.8F) * 2.18F + lane * 0.12F;
            float zp = 0.72F + random(i, 36.1F);
            ribbon(b, x0, yp, zp, x0 + len, yp + 0.09F + lane * 0.04F, zp + 0.03F,
                    0.022F + random(i, 38.9F) * 0.024F, 1.0F, 0.10F, 0.045F,
                    a * (0.24F + random(i, 40.5F) * 0.30F), 0.0F);
        }
    }

    private static void redFeatherSlashes(BufferBuilder b, float slashT, float burst, float alpha) {
        float travel = smootherStep(slashT), a = alpha * burst;
        for (int i = 0; i < 7; i++) {
            float f = i / 6.0F;
            float yp = -0.86F + f * 0.98F;
            float zp = 0.78F + f * 0.18F;
            float lift = MathHelper.sin(f * (float) Math.PI) * 0.84F;
            ribbon(b, -8.8F + f * 1.1F + travel * 2.0F, yp - lift * 0.14F, zp,
                    8.0F + f * 2.2F + travel * 2.5F, yp + 0.42F + lift, zp + 0.10F,
                    0.22F + f * 0.18F, 1.0F, 0.025F, 0.020F,
                    a * (0.32F + (1.0F - f) * 0.22F), 2.0F);
        }
    }

    private static void sparksAndShards(BufferBuilder b, float age, float progress,
                                        float alpha, float burst, float flash) {
        float presence = alpha * MathHelper.clamp((age - 7.0F) / 5.0F, 0.0F, 1.0F)
                * (1.0F - smootherStep(MathHelper.clamp((progress - 0.72F) / 0.20F, 0.0F, 1.0F)));
        if (presence <= 0.001F) return;
        for (int i = 0; i < SHARD_COUNT; i++) {
            float side = random(i, 47.1F) * 2.0F - 1.0F;
            float drift = frac(age * (0.026F + random(i, 48.6F) * 0.032F) + random(i, 49.8F));
            float xp = -3.2F + random(i, 51.3F) * 7.2F + drift * (1.1F + burst * 1.5F);
            float yp = -0.40F + random(i, 52.9F) * 2.05F + drift * 0.40F;
            float zp = 0.50F + random(i, 54.4F) * 1.55F + side * 0.28F;
            float size = 0.034F + random(i, 55.6F) * 0.088F + flash * 0.022F;
            float a = presence * (1.0F - smootherStep(drift))
                    * (0.22F + random(i, 57.2F) * 0.40F + burst * 0.28F);
            diamond(b, xp, yp, zp, size, age * (0.12F + random(i, 58.9F) * 0.20F) + i,
                    1.0F, 0.04F, 0.04F, a);
            if ((i & 7) == 0) {
                ribbon(b, xp - size * 4.0F, yp - size * 0.12F, zp,
                        xp + size * 8.0F, yp + size * 0.32F, zp,
                        size * 0.40F, 1.0F, 0.10F, 0.05F, a * 0.42F, 0.0F);
            }
        }
    }

    private static void ribbon(BufferBuilder b, float x0, float y0, float z0,
                               float x1, float y1, float z1, float width,
                               float r, float g, float blue, float alpha, float uBase) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = MathHelper.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001F) return;
        float px = -dy / len * width * 0.5F;
        float py = dx / len * width * 0.5F;
        quad(b,
                x0 + px, y0 + py, z0, uBase, 0.0F,
                x1 + px, y1 + py, z1, uBase + 1.0F, 0.0F,
                x1 - px, y1 - py, z1, uBase + 1.0F, 1.0F,
                x0 - px, y0 - py, z0, uBase, 1.0F,
                r, g, blue, alpha);
    }

    private static void diamond(BufferBuilder b, float x, float y, float z, float size,
                                float rotation, float r, float g, float blue, float alpha) {
        float c = MathHelper.cos(rotation), s = MathHelper.sin(rotation);
        float ax = c * size * 1.35F, ay = s * size * 1.35F;
        float bx = -s * size, by = c * size;
        quad(b,
                x - ax, y - ay, z, 6.0F, 0.5F,
                x + bx, y + by, z, 6.5F, 0.0F,
                x + ax, y + ay, z, 7.0F, 0.5F,
                x - bx, y - by, z, 6.5F, 1.0F,
                r, g, blue, alpha);
    }

    private static void quad(BufferBuilder b,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float r, float g, float blue, float alpha) {
        vertex(b, x0, y0, z0, u0, v0, r, g, blue, alpha);
        vertex(b, x1, y1, z1, u1, v1, r, g, blue, alpha);
        vertex(b, x2, y2, z2, u2, v2, r, g, blue, alpha);
        vertex(b, x3, y3, z3, u3, v3, r, g, blue, alpha);
    }

    private static void vertex(BufferBuilder b, float x, float y, float z, float u, float v,
                               float r, float g, float blue, float alpha) {
        b.pos(x, y, z).tex(u, v).color(clamp(r), clamp(g), clamp(blue), clamp(alpha)).endVertex();
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
    private static double lerp(float t, double a, double b) { return a + (b - a) * t; }
}

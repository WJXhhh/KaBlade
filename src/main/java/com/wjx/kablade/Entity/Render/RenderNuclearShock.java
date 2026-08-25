package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityNuclearShock;
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

/** 1.20 核能震动五层特效在 1.12 的同步实现。 */
public class RenderNuclearShock extends Render<EntityNuclearShock> {
    private static final float TWO_PI = (float) Math.PI * 2.0F;

    public RenderNuclearShock(RenderManager manager) { super(manager); shadowSize = 0.0F; }
    @Nullable @Override protected ResourceLocation getEntityTexture(EntityNuclearShock entity) { return null; }

    @Override
    public void doRender(EntityNuclearShock entity, double x, double y, double z,
                         float yaw, float partialTicks) {
        float seconds = entity.getAnimationFrame(partialTicks) / 30.0F;
        if (seconds < 0.88F || seconds > 2.24F) return;
        float burst = seconds - 0.88F;
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        int previousProgram = 0;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GlStateManager.translate(x, y + 0.035D, z);
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            GlStateManager.depthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder b = tessellator.getBuffer();

            // 与 1.20 相同，五层效果共用菲涅尔等离子 shader；若在穹顶后
            // 提前解绑，光柱会落回固定管线，在部分渲染环境中几乎不可见。
            float domeProgress = clamp(burst / 0.52F);
            float radius = (1.0F - (float) Math.pow(1.0F - domeProgress, 3.0D)) * 4.8F;
            float alpha = burst < 0.45F ? 1.0F : clamp(1.0F - (burst - 0.45F) / 0.91F);
            previousProgram = NuclearShockShader.bind(seconds);
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            hemisphere(b, radius, alpha, 1.0F, 0.35F, 0.08F, 16, 32);
            if (radius > 0.5F) hemisphere(b, radius * 1.06F, alpha * 0.5F,
                    1.0F, 0.55F, 0.15F, 16, 32);
            tessellator.draw();

            // 闪光、贯天柱、三重地波和放射尖刺保持高亮顶点渐变。
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            if (burst < 0.38F) flashCore(b, burst);
            if (burst < 0.55F) pillar(b, burst);
            if (burst < 1.15F) groundRibbons(b, radius, burst, alpha);
            if (burst < 0.70F) radialSpikes(b, burst, alpha);
            tessellator.draw();
        } finally {
            NuclearShockShader.restore(previousProgram);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.color(1, 1, 1, 1);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    private static void flashCore(BufferBuilder b, float t) {
        float progress = t / 0.38F;
        float radius = (1.0F - (float) Math.pow(1.0F - progress, 2.0D)) * 2.2F;
        hemisphere(b, radius, (1.0F - progress) * 0.95F,
                1.0F, 0.95F, 0.75F, 8, 16);
    }

    private static void hemisphere(BufferBuilder b, float radius, float alpha,
                                   float red, float green, float blue,
                                   int rings, int segments) {
        for (int i = 0; i < rings; i++) {
            double phi1 = i / (double) rings * Math.PI * 0.5D;
            double phi2 = (i + 1) / (double) rings * Math.PI * 0.5D;
            for (int j = 0; j < segments; j++) {
                double theta1 = j / (double) segments * Math.PI * 2.0D;
                double theta2 = (j + 1) / (double) segments * Math.PI * 2.0D;
                sphereVertex(b, radius, phi1, theta1, red, green, blue, alpha);
                sphereVertex(b, radius, phi1, theta2, red, green, blue, alpha);
                sphereVertex(b, radius, phi2, theta2, red, green, blue, alpha);
                sphereVertex(b, radius, phi2, theta1, red, green, blue, alpha);
            }
        }
    }

    private static void sphereVertex(BufferBuilder b, float radius, double phi, double theta,
                                     float red, float green, float blue, float alpha) {
        vertex(b, radius * Math.cos(phi) * Math.cos(theta), radius * Math.sin(phi),
                radius * Math.cos(phi) * Math.sin(theta), red, green, blue, alpha);
    }

    private static void pillar(BufferBuilder b, float t) {
        float progress = t / 0.55F;
        float width = 0.85F * (1.0F - progress * progress);
        float alpha = 1.0F - progress;
        float height = 13.5F;
        for (int k = 0; k < 4; k++) {
            double rotation = k * Math.PI / 4.0D + t * 4.0D;
            double cos = Math.cos(rotation) * width, sin = Math.sin(rotation) * width;
            vertex(b, -cos, .05, -sin, 1, .95F, .60F, alpha * .95F);
            vertex(b, cos, .05, sin, 1, .95F, .60F, alpha * .95F);
            vertex(b, cos * .6, height, sin * .6, 1, .35F, .05F, 0);
            vertex(b, -cos * .6, height, -sin * .6, 1, .35F, .05F, 0);
        }
        float cylinderRadius = width * 1.35F;
        for (int j = 0; j < 16; j++) {
            double a = j / 16.0D * Math.PI * 2.0D + t * 6.0D;
            double n = (j + 1) / 16.0D * Math.PI * 2.0D + t * 6.0D;
            double x1 = Math.cos(a) * cylinderRadius, z1 = Math.sin(a) * cylinderRadius;
            double x2 = Math.cos(n) * cylinderRadius, z2 = Math.sin(n) * cylinderRadius;
            vertex(b, x1, .05, z1, 1, .65F, .15F, alpha * .75F);
            vertex(b, x2, .05, z2, 1, .65F, .15F, alpha * .75F);
            vertex(b, x2 * .7, height * .85, z2 * .7, 1, .20F, .02F, 0);
            vertex(b, x1 * .7, height * .85, z1 * .7, 1, .20F, .02F, 0);
        }
    }

    private static void groundRibbons(BufferBuilder b, float radius, float t, float alpha) {
        float rotation = t * 7.5F;
        ribbon(b, radius * 1.12F, .55F, rotation,
                1, .65F, .15F, alpha * .9F, 1, .15F, .02F);
        ribbon(b, radius * .65F, .40F, -rotation * 1.4F,
                1, .90F, .35F, alpha * .85F, 1, .40F, .05F);
        float outerRadius = clamp(t / .45F) * 6.8F;
        float outerAlpha = (1.0F - clamp(t / .85F)) * alpha;
        if (outerAlpha > .01F) ribbon(b, outerRadius, .28F, 0,
                1, .45F, .08F, outerAlpha * .8F, 1, .10F, .01F);
    }

    private static void ribbon(BufferBuilder b, float radius, float width, float rotation,
                               float r1, float g1, float b1, float alpha,
                               float r2, float g2, float b2) {
        for (int j = 0; j < 36; j++) {
            double a = j / 36.0D * TWO_PI + rotation;
            double n = (j + 1) / 36.0D * TWO_PI + rotation;
            vertex(b, (radius - width) * Math.cos(a), .03, (radius - width) * Math.sin(a), r1, g1, b1, alpha);
            vertex(b, (radius + width) * Math.cos(a), .03, (radius + width) * Math.sin(a), r2, g2, b2, 0);
            vertex(b, (radius + width) * Math.cos(n), .03, (radius + width) * Math.sin(n), r2, g2, b2, 0);
            vertex(b, (radius - width) * Math.cos(n), .03, (radius - width) * Math.sin(n), r1, g1, b1, alpha);
        }
    }

    private static void radialSpikes(BufferBuilder b, float t, float alpha) {
        float progress = t / .70F;
        float length = 3.6F * (1.0F - (float) Math.pow(progress, 1.5D));
        float spikeAlpha = (1.0F - progress) * alpha;
        for (int i = 0; i < 24; i++) {
            double angle = i / 24.0D * TWO_PI + t * 2.0F;
            float elevation = .25F + .35F * ((i % 3) / 2.0F);
            double dx = Math.cos(angle) * length, dz = Math.sin(angle) * length;
            double dy = elevation * length;
            float width = .22F * (1.0F - progress);
            double px = -Math.sin(angle) * width, pz = Math.cos(angle) * width;
            vertex(b, -px, .08, -pz, 1, .95F, .50F, spikeAlpha * .95F);
            vertex(b, px, .08, pz, 1, .95F, .50F, spikeAlpha * .95F);
            vertex(b, dx, dy, dz, 1, .25F, .02F, 0);
            vertex(b, 0, .08, 0, 1, .95F, .50F, spikeAlpha * .95F);
        }
    }

    private static float clamp(float value) { return MathHelper.clamp(value, 0.0F, 1.0F); }
    private static void vertex(BufferBuilder b, double x, double y, double z,
                               float red, float green, float blue, float alpha) {
        b.pos(x, y, z).color(red, green, blue, clamp(alpha)).endVertex();
    }
}

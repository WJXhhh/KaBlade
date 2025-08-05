package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityConfinementForceField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public class RenderConfinementForceField extends Render<EntityConfinementForceField> {
    public RenderConfinementForceField(RenderManager renderManager) {
        super(renderManager);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityConfinementForceField entity) {
        return null;
    }

    @Override
    public void doRender(EntityConfinementForceField entity, double x, double y, double z, float entityYaw, float partialTicks) {

    }
    public void doRender2(EntityConfinementForceField entity, double x, double y, double z, float entityYaw, float partialTicks){
        super.doRender(entity, x, y, z, entityYaw, partialTicks);

        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + entity.height / 2f, (float) z);

        // 渲染空间扭曲涟漪效果
        renderDistortionRipples(entity, partialTicks);

        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
    }
    private void renderDistortionRipples(EntityConfinementForceField entity, float partialTicks) {
        float time = (entity.ticksExisted + partialTicks) * 0.1f;

        // 设置混合模式

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 渲染多层涟漪
        for (int layer = 0; layer < 3; layer++) {
            float layerOffset = layer * 0.3f;
            renderRippleLayer(buffer, tessellator, time + layerOffset, layer);
        }

        // 恢复OpenGL状态
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void renderRippleLayer(BufferBuilder buffer, Tessellator tessellator, float time, int layer) {
        int rippleCount = 4; // 每层涟漪数量

        for (int i = 0; i < rippleCount; i++) {
            float ripplePhase = time + i * 2.0f;
            float radius = (ripplePhase % 8.0f); // 涟漪半径，8格最大范围

            if (radius < 0.5f) continue; // 太小的涟漪不渲染

            float alpha = Math.max(0.0f, 1.0f - (radius / 8.0f)); // 随距离衰减
            alpha *= 0.3f - layer * 0.1f; // 每层透明度递减

            // 扭曲强度随时间和距离变化
            float distortionStrength = alpha * 0.5f * (float) Math.sin(ripplePhase * 2.0f);

            renderDistortionRing(buffer, tessellator, radius, alpha, distortionStrength, layer);
        }
    }

    private void renderDistortionRing(BufferBuilder buffer, Tessellator tessellator, float radius, float alpha, float distortion, int layer) {
        int segments = 32; // 圆环分段数
        float segmentAngle = (float) (2 * Math.PI / segments);

        // 设置颜色 - 蓝紫色扭曲效果
        float r = 0.4f + layer * 0.2f;
        float g = 0.2f + layer * 0.1f;
        float b = 0.8f + layer * 0.1f;

        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = i * segmentAngle;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // 添加扭曲效果
            float distortedRadius1 = radius + distortion * (float) Math.sin(angle * 4 + layer);
            float distortedRadius2 = radius * 0.9f + distortion * (float) Math.cos(angle * 4 + layer);

            // 外圈顶点
            float x1 = cos * distortedRadius1;
            float z1 = sin * distortedRadius1;
            float y1 = distortion * 0.3f * (float) Math.sin(angle * 2);

            // 内圈顶点
            float x2 = cos * distortedRadius2;
            float z2 = sin * distortedRadius2;
            float y2 = distortion * 0.2f * (float) Math.cos(angle * 2);

            buffer.pos(x1, y1, z1).color(r, g, b, alpha * 0.8f).endVertex();
            buffer.pos(x2, y2, z2).color(r, g, b, alpha).endVertex();
        }

        tessellator.draw();

        // 添加粒子效果
        if (layer == 0) {
            renderDistortionParticles(buffer, tessellator, radius, alpha, r, g, b);
        }
    }

    private void renderDistortionParticles(BufferBuilder buffer, Tessellator tessellator, float radius, float alpha, float r, float g, float b) {
        int particleCount = (int) (radius * 4); // 粒子数量基于半径

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (Math.random() * 2 * Math.PI);
            float particleRadius = radius + (float) (Math.random() - 0.5) * 0.5f;

            float x = (float) Math.cos(angle) * particleRadius;
            float z = (float) Math.sin(angle) * particleRadius;
            float y = (float) (Math.random() - 0.5) * 0.3f;

            float size = 0.05f + (float) Math.random() * 0.05f;
            float particleAlpha = alpha * 0.6f * (float) Math.random();

            // 渲染小方块粒子
            buffer.pos(x - size, y - size, z - size).color(r, g, b, particleAlpha).endVertex();
            buffer.pos(x - size, y + size, z - size).color(r, g, b, particleAlpha).endVertex();
            buffer.pos(x + size, y + size, z + size).color(r, g, b, particleAlpha).endVertex();
            buffer.pos(x + size, y - size, z + size).color(r, g, b, particleAlpha).endVertex();
        }

        tessellator.draw();
    }
}

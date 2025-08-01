package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityRainUmbrella;
import com.wjx.kablade.Entity.EntityTuna;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
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

import static org.lwjgl.opengl.GL11.*;

public class RenderEntityRainUmbrella extends Render<EntityRainUmbrella> {

    private static final float RADIUS = 4F; // 圆环半径
    private static final float RING_THICKNESS = 0.05F; // 圆环厚度
    private static final float HEIGHT = 0.2F; // 纵向厚度
    private static final int LAYERS = 8; // 纵向层数
    private static final int SEGMENTS = 64; // 圆环分段数
    private static final int DURATION = 20;
    private static final int START_TICK = 70;
    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityRainUmbrella entity) {
        return null;
    }
    public RenderEntityRainUmbrella(RenderManager renderManager){
        super(renderManager);
        this.shadowSize = 0.0f;
    }

    private static final WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade","effects/rain/umbrella.obj"));
    //WavefrontObject model2 = new WavefrontObject(new ResourceLocationRaw("kablade","effects/tuna/impact.obj"));

    int finalT = 0;

    @Override
    public void doRender(EntityRainUmbrella entity, double x, double y, double z, float entityYaw, float partialTicks) {

        GL11.glDisable(GL_LIGHTING);
        GL11.glDisable(GL_BLEND);

        GlStateManager.pushMatrix();
        float angle = (entity.ticksExisted+partialTicks)*10;
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        Minecraft mc = Minecraft.getMinecraft();

        GlStateManager.translate(x,y+1,z);
        float tk = entity.ticksExisted+partialTicks;
        if(tk>=0&&tk<=20){
            float bl = (float) Math.sin(tk/40d*Math.PI);
            GlStateManager.scale(140f*bl,140f*bl,140f*bl);
        }
        else if(tk>70&&tk<=75){
            float bl = (float) Math.sin((75-tk)/40d*Math.PI);
            GlStateManager.scale(140f*bl,140f*bl,140f*bl);

        }
        else if(tk>75){
            GlStateManager.scale(0f,0f,0f);
        }

        else {
            GlStateManager.scale(140f,140f,140f);
        }


        GlStateManager.rotate(angle,0,1,0);
        mc.getTextureManager().bindTexture(new ResourceLocation("kablade","effects/rain/umb.png"));

        model.renderAll();

        GlStateManager.popMatrix();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL_LIGHTING);
        GL11.glDisable(GL_BLEND);//开启混合

        if (entity.ticksExisted > START_TICK) {
            // 计算特效进度
            float effectTicks = entity.ticksExisted+partialTicks - START_TICK;
            float progress = Math.min(1.0F, (float)effectTicks / DURATION);

            // 如果特效已结束，不再渲染
            if (progress >= 1.0F) {
                return;
            }

            float lastx = OpenGlHelper.lastBrightnessX;
            float lasty = OpenGlHelper.lastBrightnessY;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            // 保存当前渲染状态w

            GlStateManager.pushMatrix();

            GlStateManager.scale(5.0f, 5.0f, 5.0f);
            // 移动到实体位置
            GlStateManager.translate((float)x, (float)y+0.1, (float)z);

            // 禁用光照以获得发光效果
            GlStateManager.disableLighting();

            // 启用混合模式

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            GL11.glDisable(GL_CULL_FACE);
            // 禁用深度测试以实现透明效果

            GlStateManager.rotate(180f, 1.0f, 0.0f, 0.0f);

            // 获取Tessellator实例
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            float sp = (float) Math.sin(progress*Math.PI/2f);
            // 根据进度计算当前半径
            float currentRadius = RADIUS * sp;

            // 渲染立体圆环
            renderThickRing(tessellator, buffer, currentRadius, sp);

            // 恢复渲染状态

            GL11.glEnable(GL_CULL_FACE);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
        }









        super.doRender(entity, x, y, z, entityYaw, partialTicks);

    }

    private void renderThickRing(Tessellator tessellator, BufferBuilder buffer, float radius, float progress) {
        // 计算透明度（随进度递减）
        float alpha = (1.0F - progress);

        // 纵向厚度分层
        float layerHeight = HEIGHT / LAYERS;


        for (int layer = 0; layer < LAYERS; layer++) {
            // 计算当前层的高度位置（-HEIGHT/2 到 HEIGHT/2）
            float layerY = -HEIGHT/2 + layer * layerHeight;

            // 计算当前层的透明度（中间最亮，两边渐暗）
            float layerAlpha = alpha * (1.0F - Math.abs(layer - LAYERS/2.0F) / (LAYERS/2.0F) * 0.7F);


            // 开始绘制四边形
            buffer.begin(4, DefaultVertexFormats.POSITION_COLOR);

            // 绘制圆环的每个分段
            for (int i = 0; i < SEGMENTS; i++) {
                float angle1 = (float)i / SEGMENTS * (float)Math.PI * 2.0F;
                float angle2 = (float)((i + 1) % SEGMENTS) / SEGMENTS * (float)Math.PI * 2.0F;

// 计算内外半径
                float innerRadius = radius - RING_THICKNESS / 2;
                float outerRadius = radius + RING_THICKNESS / 2;

// 计算四个角的坐标
                float x1 = (float)Math.cos(angle1) * innerRadius;
                float z1 = (float)Math.sin(angle1) * innerRadius;
                float x2 = (float)Math.cos(angle1) * outerRadius;
                float z2 = (float)Math.sin(angle1) * outerRadius;
                float x3 = (float)Math.cos(angle2) * outerRadius;
                float z3 = (float)Math.sin(angle2) * outerRadius;
                float x4 = (float)Math.cos(angle2) * innerRadius;
                float z4 = (float)Math.sin(angle2) * innerRadius;

// 计算颜色（从白色渐变到浅蓝色）
                float r = 1.0F - progress * 0.3F;
                float g = 1.0F - progress * 0.1F;
                float b = 1.0F;

// ===== 底面三角形（两个三角形组成一个四边形）=====
                buffer.pos(x1, layerY, z1).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x2, layerY, z2).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY, z3).color(r, g, b, layerAlpha).endVertex();

                buffer.pos(x1, layerY, z1).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY, z3).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x4, layerY, z4).color(r, g, b, layerAlpha).endVertex();

// ===== 顶面三角形 =====
                buffer.pos(x1, layerY + layerHeight, z1).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY + layerHeight, z3).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x2, layerY + layerHeight, z2).color(r, g, b, layerAlpha).endVertex();

                buffer.pos(x1, layerY + layerHeight, z1).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x4, layerY + layerHeight, z4).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY + layerHeight, z3).color(r, g, b, layerAlpha).endVertex();

// ===== 外侧面三角形 =====
                buffer.pos(x2, layerY, z2).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY, z3).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY + layerHeight, z3).color(r, g, b, layerAlpha).endVertex();

                buffer.pos(x2, layerY, z2).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x3, layerY + layerHeight, z3).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x2, layerY + layerHeight, z2).color(r, g, b, layerAlpha).endVertex();

// ===== 内侧面三角形 =====
                buffer.pos(x4, layerY, z4).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x1, layerY, z1).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x1, layerY + layerHeight, z1).color(r, g, b, layerAlpha).endVertex();

                buffer.pos(x4, layerY, z4).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x1, layerY + layerHeight, z1).color(r, g, b, layerAlpha).endVertex();
                buffer.pos(x4, layerY + layerHeight, z4).color(r, g, b, layerAlpha).endVertex();


            }

            // 绘制当前层
            tessellator.draw();





        }
    }
}

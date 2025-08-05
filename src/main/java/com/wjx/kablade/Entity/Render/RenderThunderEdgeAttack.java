package com.wjx.kablade.Entity.Render;

import com.google.common.collect.Lists;
import com.wjx.kablade.Entity.EntityThunderEdgeAttack;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.annotation.Nullable;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static com.wjx.kablade.Main.logger;
import static org.lwjgl.opengl.GL11.*;

public class RenderThunderEdgeAttack extends Render<Entity> {
    static ResourceLocation TEXTURE1 = new ResourceLocation(Main.MODID + ":textures/entity/thunder_edge_attack/thunder_edge_attack_n.png");


    float[] lightPosition = {0.0f, 0.0f, 1.0f, 0.0f}; // 光源位置


    static ArrayList<ResourceLocation> EffectTEXS = Lists.newArrayList();
    public static ArrayList<Vector3d> inVertex = Lists.newArrayList();

    public static double ferh = 1d/59d;

    /**
     * 绘制顶点到缓冲构建器中，根据索引奇偶性应用不同的缩放和纹理坐标
     *
     * @param i           顶点索引，用于确定当前处理的顶点位置
     * @param bufferBuilder 缓冲构建器对象，用于构建渲染数据（不可为空）
     * @param thickness   厚度系数，用于奇数索引顶点的缩放计算
     */
    public void drawPoint(int i, BufferBuilder bufferBuilder, double thickness) {

        /* 检查缓冲构建器有效性 */
        if (bufferBuilder == null)
            logger.error("bn");

        /* 计算顶点索引并获取对应三维向量 */
        int xxx = i / 2;
        Vector3d vecx = inVertex.get(xxx);

        /* 检查顶点数据有效性 */
        if (vecx == null)
            logger.error("ven");

        /* 根据顶点索引奇偶性分发处理逻辑 */
        if (i % 2 == 0) {
            /* 偶数索引顶点处理：固定缩放系数，纹理坐标V分量为0 */
            int xx = i / 2;
            Vector3d vec = inVertex.get(xx);
            bufferBuilder.pos(vec.x * 2.25, 0, vec.z * 2.25).tex(ferh * xx, 0).endVertex();
        } else {
            /* 奇数索引顶点处理：使用动态厚度参数，纹理坐标V分量为1 */
            int xx = i / 2;
            Vector3d vec = inVertex.get(xx);
            bufferBuilder.pos(vec.x * thickness, 0, vec.z * thickness).tex(ferh * xx, 1).endVertex();
        }
    }


    static{
        /* 初始化inVertex顶点列表：
         * 以3度为步长逆序生成180°至0°的弧度值
         * 计算单位圆上对应角度的cos/sin值作为x/z坐标
         * 创建Vector3d对象并设置x/z分量，添加到inVertex集合
         * 用于生成半圆形的顶点坐标序列 */
        double inR = 180d;
        for(;inR>=0;inR-=3d){
            double th = Math.toRadians(inR);
            double tx = Math.cos(th) * 1;
            double ty = Math.sin(th) * 1;
            Vector3d v = new Vector3d();
            v.x = tx;
            v.z = ty;
            inVertex.add(v);
        }

        /* 预加载闪电边缘攻击特效的纹理资源：
         * 从0到19依次加载20张连续编号的PNG纹理
         * 路径格式为textures/entity/thunder_edge_attack/thunder_edge_light_X.png
         * 存储到EffectTEXS列表用于后续动画渲染 */
        for(int i = 0;i<=19;i++){
            EffectTEXS.add(new ResourceLocation(Main.MODID + ":textures/entity/thunder_edge_attack/thunder_edge_light_"+i+".png"));
        }
    }


    public RenderThunderEdgeAttack(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0f;
    }



    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        Tessellator tessellator = Tessellator.getInstance();
        EntityThunderEdgeAttack eee =(EntityThunderEdgeAttack)entity;
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        GL11.glDisable(GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL_BLEND);//开启混合
        Minecraft mc = Minecraft.getMinecraft();

        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

        //RenderEffect
        if(eee.counter>=5 && eee.counter<=24)
        {
            float[] color = new float[]{1f,1f,1f,1f};
            GlStateManager.pushMatrix();

            GlStateManager.translate(x, y + 0.25, z);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.rotate(-entity.rotationYaw, 0f, 1f, 0f);
            GlStateManager.scale(1,2,1);

            GlStateManager.translate(2.16506350946109625, 0, 1.25);


            Minecraft.getMinecraft().getTextureManager().bindTexture(EffectTEXS.get((eee.counter-5)));
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();

            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY+entity.rotationYaw, 0, 1, 0);
            //GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);


            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tess.draw();
            GlStateManager.popMatrix();
        }

        if(eee.counter>=10 && eee.counter<=29)
        {
            GlStateManager.pushMatrix();

            GlStateManager.translate(x, y + 0.25, z);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.rotate(-entity.rotationYaw, 0f, 1f, 0f);
            GlStateManager.scale(1,2,1);

            GlStateManager.translate(1.25, 0, 2.16506350946109625);


            Minecraft.getMinecraft().getTextureManager().bindTexture(EffectTEXS.get((eee.counter-10)));
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();

            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY+entity.rotationYaw, 0, 1, 0);
            //GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);


            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tess.draw();
            GlStateManager.popMatrix();
        }

        if(eee.counter>=15 && eee.counter<=34)
        {
            GlStateManager.pushMatrix();

            GlStateManager.translate(x, y + 0.25, z);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.rotate(-entity.rotationYaw, 0f, 1f, 0f);
            GlStateManager.scale(1,2,1);

            GlStateManager.translate(0, 0, 2.5);


            Minecraft.getMinecraft().getTextureManager().bindTexture(EffectTEXS.get((eee.counter-15)));
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();

            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY+entity.rotationYaw, 0, 1, 0);
            //GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);


            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tess.draw();
            GlStateManager.popMatrix();
        }

        if(eee.counter>=20 && eee.counter<=39)
        {
            GlStateManager.pushMatrix();

            GlStateManager.translate(x, y + 0.25, z);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.rotate(-entity.rotationYaw, 0f, 1f, 0f);
            GlStateManager.scale(1,2,1);

            GlStateManager.translate(-1.25, 0, 2.16506350946109625);


            Minecraft.getMinecraft().getTextureManager().bindTexture(EffectTEXS.get((eee.counter-20)));
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();

            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY+entity.rotationYaw, 0, 1, 0);
            //GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);


            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tess.draw();
            GlStateManager.popMatrix();
        }

        if(eee.counter>=25 && eee.counter<=44)
        {
            GlStateManager.pushMatrix();

            GlStateManager.translate(x, y + 0.25, z);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.rotate(-entity.rotationYaw, 0f, 1f, 0f);
            GlStateManager.scale(1,2,1);

            GlStateManager.translate(-2.16506350946109625, 0, 1.25);


            Minecraft.getMinecraft().getTextureManager().bindTexture(EffectTEXS.get((eee.counter-25)));
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();

            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY+entity.rotationYaw, 0, 1, 0);
            //GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);


            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tess.draw();
            GlStateManager.popMatrix();
        }



        //RenderEdge
        GlStateManager.pushMatrix();
        GlStateManager.color(1f,1f,1f,((EntityThunderEdgeAttack) entity).alpha);
        GlStateManager.translate(x,y + 0.75,z);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.rotate(-entity.rotationYaw + ((EntityThunderEdgeAttack) entity).ang,0f,1f,0f);
        bufferBuilder.begin(4, DefaultVertexFormats.POSITION_TEX);
        this.bindTexture(TEXTURE1);


        double tIn = ((EntityThunderEdgeAttack) entity).thickness;
            if(((EntityThunderEdgeAttack) entity).tC > 30){
                tIn = ((EntityThunderEdgeAttack) entity).thick2;
            }
            for (int i = 117; i >= ((EntityThunderEdgeAttack) entity).progress; i--) {

                drawPoint(i, bufferBuilder,tIn);
                drawPoint(i + 1, bufferBuilder,tIn);
                drawPoint(i + 2, bufferBuilder,tIn);

            }

        tessellator.draw();

        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL_BLEND);//开启混合
    }
}

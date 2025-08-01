package com.wjx.kablade.Entity.Render;

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

import static com.wjx.kablade.Entity.Render.RenderWindEnchantment.effect1;
import static org.lwjgl.opengl.GL11.*;

public class RenderEntityTuna extends Render<EntityTuna> {
    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityTuna entity) {
        return null;
    }
    public RenderEntityTuna(RenderManager renderManager){
        super(renderManager);
        this.shadowSize = 0.0f;
    }

    private static final WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade","effects/tuna/tuna.obj"));
    private static final WavefrontObject model2 = new WavefrontObject(new ResourceLocationRaw("kablade","effects/tuna/impact.obj"));

    int finalT = 0;

    @Override
    public void doRender(EntityTuna entity, double x, double y, double z, float entityYaw, float partialTicks) {

        GL11.glDisable(GL_LIGHTING);
        GL11.glDisable(GL_BLEND);
        GlStateManager.pushMatrix();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        Minecraft mc = Minecraft.getMinecraft();
        if (entity.ticksExisted > 2){
            GlStateManager.translate(x, y - 2d, z);
        }
        else {
            // Use partialTicks to Render Falling
            double fallProgress = entity.ticksExisted + partialTicks;
            double offsetY = 3 - (fallProgress * 2d);
            GlStateManager.translate(x, y + offsetY, z);
        }
        GlStateManager.scale(4.5f,3.5f,4.5f);
        mc.getTextureManager().bindTexture(new ResourceLocation("kablade","effects/tuna/tuna.png"));

        model.renderAll();

        GlStateManager.popMatrix();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL_LIGHTING);
        GL11.glDisable(GL_BLEND);//开启混合


        int nowT = entity.ftick;

        if(nowT > 15){
            if(nowT<20)
                finalT = nowT;
            if(nowT>20)
                finalT = 20;
            GL11.glDisable(GL_LIGHTING);
            GL11.glDisable(GL_BLEND);
            GL11.glDisable(GL_CULL_FACE);
            GlStateManager.pushMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
            GlStateManager.translate(x,y+0.01,z);

            GlStateManager.color(1f,1f,1f);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("kablade","effects/tuna/crack.png"));
            GlStateManager.scale((float)(finalT-15)/15,0.01f,(float)(finalT-15)/15);

            bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
            bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
            bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
            bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glEnable(GL_LIGHTING);
            GL11.glDisable(GL_BLEND);
            GL11.glEnable(GL_CULL_FACE);
            //GlStateManager.pushMatrix();
            // GL11.glDisable(GL_LIGHTING);
            // GL11.glEnable(GL_BLEND);
            // GL11.glDisable(GL_CULL_FACE);
            // //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            //
            //
            // OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            // GL11.glEnable(GL_CULL_FACE);
            // GL11.glEnable(GL_LIGHTING);
            // GL11.glDisable(GL_BLEND);//开启混合
            // GlStateManager.popMatrix();
        }
        if(nowT>15&&nowT<24){
            float sc = 0;
            if(nowT < 18){
                sc = (nowT-12)*0.33332f;
            }
            if(nowT >= 18){
                sc = (24-nowT)*0.16666f;
            }
            float lastx = OpenGlHelper.lastBrightnessX;
            float lasty = OpenGlHelper.lastBrightnessY;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            GL11.glDisable(GL_LIGHTING);
            GL11.glEnable(GL_BLEND);
            GlStateManager.pushMatrix();

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.translate(x,y,z);
            GlStateManager.scale(sc,sc,sc);
            mc.getTextureManager().bindTexture(new ResourceLocation("kablade","effects/tuna/impact.png"));

            model2.renderAll();

            GlStateManager.popMatrix();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glEnable(GL_LIGHTING);
            GL11.glDisable(GL_BLEND);
        }


        super.doRender(entity, x, y, z, entityYaw, partialTicks);

    }
}

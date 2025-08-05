package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityConceptual;
import com.wjx.kablade.Entity.EntityTuna;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;

public class RenderConceptual extends Render<EntityConceptual> {
    protected RenderConceptual(RenderManager manager) {
        super(manager);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(@Nonnull EntityConceptual entityConceptual) {
        return null;
    }

    @Override
    public void doRender(@Nonnull EntityConceptual entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.pushMatrix();
        float time = entity.ticksExisted + partialTicks;
        Tessellator  tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        if(entity.ticksExisted<30){
            Minecraft mc = Minecraft.getMinecraft();
            mc.getTextureManager().bindTexture(new ResourceLocation("kablade:effects/cnzy/ef2.png"));
            GlStateManager.translate(x,y+0.5,z);
            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY+entity.rotationYaw, 0, 1, 0);
            GlStateManager.scale(time/5*Math.sin(time/30*Math.PI*0.5f), time/5*Math.sin(time/30*Math.PI*0.5f),1);
            if(time>15){
                GlStateManager.color(1f,1f,1f, (float) (1-((time-15)/15*Math.sin((time-15)/15*Math.PI*0.5f))));
            }
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tessellator.draw();
        }

        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL_LIGHTING);
        GL11.glDisable(GL_BLEND);
    }
}

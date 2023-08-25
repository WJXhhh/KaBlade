package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityFreezeDomain;
import com.wjx.kablade.Main;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public class RenderFreezeDomain extends Render<EntityFreezeDomain> {
    public RenderFreezeDomain(RenderManager renderManager) {
        super(renderManager);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityFreezeDomain entity) {
        return new ResourceLocation(Main.MODID + ":textures/entity/freeze_domain/f_" + entity.getRenderTick() + ".png");
    }

    @Override
    public void doRender(EntityFreezeDomain entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.1,z);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindEntityTexture(entity);
        GlStateManager.rotate(-180,1,0,0);
        bufferBuilder.pos(-8,0,-8).tex(0,0).endVertex();
        bufferBuilder.pos(8,0,-8).tex(1,0).endVertex();
        bufferBuilder.pos(8,0,8).tex(1,1).endVertex();
        bufferBuilder.pos(-8,0,8).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }
}

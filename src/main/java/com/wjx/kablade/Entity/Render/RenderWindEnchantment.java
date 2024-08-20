package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityWindEnchantment;
import com.wjx.kablade.Main;
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

public class RenderWindEnchantment extends Render<EntityWindEnchantment> {
    public RenderWindEnchantment(RenderManager renderManager) {
        super(renderManager);
    }

    public static ResourceLocation effect1 = new ResourceLocation(Main.MODID + ":textures/entity/wind_enchantment/effect1.png");
    public static ResourceLocation effect2 = new ResourceLocation(Main.MODID + ":textures/entity/wind_enchantment/effect2.png");
    public static ResourceLocation effect3 = new ResourceLocation(Main.MODID + ":textures/entity/wind_enchantment/effect3.png");
    public static ResourceLocation effect4 = new ResourceLocation(Main.MODID + ":textures/entity/wind_enchantment/effect4.png");
    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityWindEnchantment entity) {
        return null;
    }

    @Override
    public void doRender(EntityWindEnchantment entity, double x, double y, double z, float entityYaw, float partialTicks) {
        int renderTick = entity.getDataManager().get(EntityWindEnchantment.renderTick);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.pushMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+1,z);
        GlStateManager.rotate(renderTick*entity.getRate.get("effect1"),0f,1f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect1);
        GlStateManager.rotate(-180,1,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+1,z);
        GlStateManager.rotate(180f,1f,0f,0f);
        GlStateManager.rotate(-renderTick*entity.getRate.get("effect1"),0f,1f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect1);
        GlStateManager.rotate(-180f,1f,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.5,z);
        GlStateManager.rotate(renderTick*entity.getRate.get("effect2"),0f,1f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect2);
        GlStateManager.rotate(-180,1,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.5,z);
        GlStateManager.rotate(180f,1f,0f,0f);
        GlStateManager.rotate(-renderTick*entity.getRate.get("effect2"),0f,1f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect2);
        GlStateManager.rotate(-180f,1f,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.1,z);
        GlStateManager.rotate(renderTick*entity.getRate.get("effect3"),0f,1f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect3);
        GlStateManager.rotate(-180,1,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.1,z);
        GlStateManager.rotate(180f,1f,0f,0f);
        GlStateManager.rotate(-renderTick*entity.getRate.get("effect3"),0f,1f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect3);
        GlStateManager.rotate(-180f,1f,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.1,z);
        GlStateManager.scale(0.4f,0f,0.4f);
        GlStateManager.rotate(renderTick*entity.getRate.get("effect4"),0f,2f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect4);
        GlStateManager.rotate(-180,1,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        bufferBuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        GlStateManager.translate(x,y+0.1,z);
        GlStateManager.scale(0.4f,0f,0.4f);
        GlStateManager.rotate(180f,1f,0f,0f);
        GlStateManager.rotate(-renderTick*entity.getRate.get("effect4"),0f,2f,0f);
        GlStateManager.color(1f,1f,1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(effect4);
        GlStateManager.rotate(-180f,1f,0,0);
        bufferBuilder.pos(-10,0,-10).tex(0,0).endVertex();
        bufferBuilder.pos(10,0,-10).tex(1,0).endVertex();
        bufferBuilder.pos(10,0,10).tex(1,1).endVertex();
        bufferBuilder.pos(-10,0,10).tex(0,1).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }
}

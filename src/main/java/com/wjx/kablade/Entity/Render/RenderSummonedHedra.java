package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntitySummonHedra;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class RenderSummonedHedra extends Render<Entity> {
    private static final WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade:effects/rain/hedra.obj"));
    public RenderSummonedHedra(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(Entity entity, double d0, double d1, double d2, float f, float f1) {
        if (entity instanceof EntitySummonHedra) {
            this.doDriveRender((EntitySummonHedra)entity, d0, d1, d2, f, f1);
        }

    }

    protected ResourceLocationRaw getEntityTexture(Entity var1) {
        return null;
    }

    private void doDriveRender(EntitySummonHedra entityPhantomSword, double dX, double dY, double dZ, float f, float f1) {

        GL11.glDisable(GL_LIGHTING);
        GL11.glEnable(GL_BLEND);//开启混合


        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE);//混合 使用源颜色的alpha值来作为因子;使用1.0作为因子;新颜色：RsSr+RdDr, GsSg+GdDg, BsSb+BdDb, AsSa+AdDa

        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)dX, (float)dY+0.1f, (float)dZ);
        GL11.glRotatef(this.lerpDegrees(entityPhantomSword.prevRotationYaw, entityPhantomSword.rotationYaw, f1), 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.lerpDegrees(entityPhantomSword.prevRotationPitch, entityPhantomSword.rotationPitch, f1), 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(entityPhantomSword.getRoll(), 0.0F, 0.0F, 1.0F);
        GL11.glScalef(2.0F, 2.0F, 2.0F);
        float lifetime = (float)entityPhantomSword.getLifeTime();
        float ticks = (float)entityPhantomSword.ticksExisted;
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(new ResourceLocation("kablade:effects/rain/3.png"));
        model.renderAll();
        GL11.glPopMatrix();
        GL11.glDisable(GL_BLEND);
        GL11.glEnable(GL_LIGHTING);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);

    }

    float lerp(float start, float end, float percent) {
        return start + percent * (end - start);
    }

    float lerpDegrees(float start, float end, float percent) {
        float diff;
        for(diff = end - start; diff < -180.0F; diff += 360.0F) {
        }

        while(diff >= 180.0F) {
            diff -= 360.0F;
        }

        return start + percent * diff;
    }
}

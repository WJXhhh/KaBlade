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
    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityRainUmbrella entity) {
        return null;
    }
    public RenderEntityRainUmbrella(RenderManager renderManager){
        super(renderManager);
        this.shadowSize = 0.0f;
    }

    WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade","effects/rain/umbrella.obj"));
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

        GlStateManager.translate(x,y-1,z);
        float tk = entity.ticksExisted+partialTicks;
        if(tk>=0&&tk<=20){
            float bl = (float) Math.sin(tk/40d*Math.PI);
            GlStateManager.scale(140f*bl,140f*bl,140f*bl);
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





        super.doRender(entity, x, y, z, entityYaw, partialTicks);

    }
}

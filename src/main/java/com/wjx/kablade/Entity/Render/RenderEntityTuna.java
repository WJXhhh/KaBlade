package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityTuna;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;

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

    WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade","effects/tuna/tuna.obj"));

    @Override
    public void doRender(EntityTuna entity, double x, double y, double z, float entityYaw, float partialTicks) {


        GlStateManager.pushMatrix();
        GL11.glDisable(GL_LIGHTING);
        GL11.glEnable(GL_BLEND);
        //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
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
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL_LIGHTING);
        GL11.glDisable(GL_BLEND);//开启混合
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);

    }
}

package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import com.wjx.kablade.Entity.model.ModelVorpalBlackHole;
import com.wjx.kablade.Main;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@SideOnly(Side.CLIENT)
public class RenderVorpalBlackHole extends Render<EntityVorpalBlackHole> {
    protected ModelBase mainModel;

    public RenderVorpalBlackHole(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.mainModel = new ModelVorpalBlackHole();
        this.shadowSize = 0.5F;
    }

    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID + ":textures/entity/blackhole/texture.png");

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityVorpalBlackHole entity) {
        return TEXTURE;
    }

    protected void preRenderCallback()
    {
        GlStateManager.scale(2d,2d,2d);
    }

    public float prepareScale()
    {
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        this.preRenderCallback();
        float f = 0.0625F;
        GlStateManager.translate(0.0F, -1.501F, 0.0F);
        return 0.0625F;
    }

    @Override
    public void doRender(EntityVorpalBlackHole entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        this.bindEntityTexture(entity);

        if (this.renderOutlines)
        {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }
        prepareScale();
        this.mainModel.render(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);

        if (this.renderOutlines)
        {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }
}

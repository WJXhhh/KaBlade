package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityRaikiriBlade;
import com.wjx.kablade.Entity.model.ModelVorpalBlackHole;
import com.wjx.kablade.Entity.model.mdlRaikiriBlade;
import com.wjx.kablade.Main;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@SideOnly(Side.CLIENT)
public class RenderRaikiriBlade extends Render<Entity> {
    protected ModelBase mainModel;


    public RenderRaikiriBlade(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.mainModel = new mdlRaikiriBlade();
        this.shadowSize = 0.0F;
    }

    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID + ":textures/effect/tex_raikiri_blade.png");

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }

    protected void preRenderCallback()
    {
        GlStateManager.scale(1d,1d,1d);
    }

    public void prepareScale()
    {
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        this.preRenderCallback();
        float f = 0.0625F;
        GlStateManager.translate(0.0F, -1.501F, 0.0F);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {

        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0.5, 0);
            GlStateManager.rotate(10f * entity.world.getTotalWorldTime(), 0f, 1f, 0f);
            this.bindEntityTexture(entity);

            if (this.renderOutlines) {
                GlStateManager.enableColorMaterial();
                GlStateManager.enableOutlineMode(this.getTeamColor(entity));
            }
            prepareScale();
            this.mainModel.render(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);

            if (this.renderOutlines) {
                GlStateManager.disableOutlineMode();
                GlStateManager.disableColorMaterial();
            }

            GlStateManager.popMatrix();}
            super.doRender(entity, x, y, z, entityYaw, partialTicks);

    }
    /*ModelBase modelBase;

    public RenderRaikiriBlade(RenderManager renderManager) {
        super(renderManager);
        modelBase = new ModelVorpalBlackHole();
    }

    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID + ":textures/entity/blackhole/texture.png");


    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityRaikiriBlade entity) {
        //return new ResourceLocation(Main.MODID + ":textures/effect/texRaikiriBlade.png");
        return TEXTURE;
    }

    @Override
    public void doRender(EntityRaikiriBlade entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        GlStateManager.pushMatrix();
        if (!entity.shouldFollow){
            GlStateManager.translate(entity.posX,entity.posY,entity.posZ);
        }else GlStateManager.translate(entity.followX,entity.followY,entity.followZ);
        GlStateManager.scale(2,2,2);
        if (this.renderOutlines)
        {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }

        this.modelBase.render(entity,0,0,0,0,0,0.6f);

        if (this.renderOutlines)
        {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }

        GlStateManager.popMatrix();
    }*/
}

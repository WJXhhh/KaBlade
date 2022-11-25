package com.wjx.kablade.Entity.Render.Layer;

import com.wjx.kablade.init.PotionInit;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;

public class LayerFreeze implements LayerRenderer<EntityLivingBase> {
    private final RenderLivingBase<?> renderer;
    public static ResourceLocation OVERLAY_ICE = new ResourceLocation("textures/blocks/ice.png");

    public LayerFreeze(RenderLivingBase<?> rendererIn)
    {
        this.renderer = rendererIn;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        int i = 30;
        doRenderLayerPetrify(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
    }

    public void doRenderLayerPetrify(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        IAttributeInstance attribute = entitylivingbaseIn.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED) ;
        if (attribute.getModifier(PotionInit.UUID_FREEZE) == null)
        {
            return;
        }

        //bg
        this.renderer.bindTexture(OVERLAY_ICE);
        this.renderer.getMainModel().render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}

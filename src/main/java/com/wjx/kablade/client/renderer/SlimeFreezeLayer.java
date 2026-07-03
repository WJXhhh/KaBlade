package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;

/**
 * Freeze overlay for slime's visible outer body, which vanilla renders as a separate layer.
 */
public final class SlimeFreezeLayer extends RenderLayer<Slime, SlimeModel<Slime>> {

    private static final ResourceLocation ICE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ice.png");

    private final SlimeModel<Slime> outerModel;

    public SlimeFreezeLayer(RenderLayerParent<Slime, SlimeModel<Slime>> parent, EntityModelSet models) {
        super(parent);
        this.outerModel = new SlimeModel<>(models.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Slime slime,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (slime.isInvisible() || !FreezeLayer.isFrozen(slime)) {
            return;
        }

        this.getParentModel().copyPropertiesTo(this.outerModel);
        this.outerModel.prepareMobModel(slime, limbSwing, limbSwingAmount, partialTick);
        this.outerModel.setupAnim(slime, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ICE_TEXTURE));
        this.outerModel.renderToBuffer(poseStack, vertexConsumer, packedLight,
                LivingEntityRenderer.getOverlayCoords(slime, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F);
    }
}

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.mobeffect.FreezeMobEffect;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Ice shell layer for entities affected by KaBlade's 1.12.2-style freeze effect.
 */
public final class FreezeLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation ICE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ice.png");

    public FreezeLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || !isFrozen(entity)) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ICE_TEXTURE));
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static boolean isFrozen(LivingEntity entity) {
        if (entity.hasEffect(ModMobEffects.FREEZE.get())) {
            return true;
        }
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return (speed != null && speed.getModifier(FreezeMobEffect.UUID_FREEZE) != null)
                || entity.getTicksFrozen() > entity.getTicksRequiredToFreeze();
    }
}

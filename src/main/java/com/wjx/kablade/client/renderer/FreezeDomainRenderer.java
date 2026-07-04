package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.FreezeDomainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Floor quad renderer for the Snow Dance freeze domain, matching the 1.12.2 f_0..f_5 animation. */
public class FreezeDomainRenderer extends EntityRenderer<FreezeDomainEntity> {

    private static final float ALPHA = 0.48F;

    public FreezeDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FreezeDomainEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.1D, 0.0D);

        ResourceLocation texture = getTextureLocation(entity);
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.freezeDomain(texture));
        Matrix4f pose = poseStack.last().pose();

        vertex(vc, pose, -8.0F, 0.0F, -8.0F, 0.0F, 0.0F);
        vertex(vc, pose, 8.0F, 0.0F, -8.0F, 1.0F, 0.0F);
        vertex(vc, pose, 8.0F, 0.0F, 8.0F, 1.0F, 1.0F);
        vertex(vc, pose, -8.0F, 0.0F, 8.0F, 0.0F, 1.0F);

        vertex(vc, pose, -8.0F, 0.0F, 8.0F, 0.0F, 1.0F);
        vertex(vc, pose, 8.0F, 0.0F, 8.0F, 1.0F, 1.0F);
        vertex(vc, pose, 8.0F, 0.0F, -8.0F, 1.0F, 0.0F);
        vertex(vc, pose, -8.0F, 0.0F, -8.0F, 0.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose,
                               float x, float y, float z, float u, float v) {
        vc.vertex(pose, x, y, z)
                .color(1.0F, 1.0F, 1.0F, ALPHA)
                .uv(u, v)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FreezeDomainEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID,
                "textures/entity/freeze_domain/f_" + entity.getRenderTick() + ".png");
    }
}

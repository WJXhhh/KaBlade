package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.OriginFreeSwordEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 1.12.2 自由召唤剑的发光三角剑体复刻。
 */
public class OriginFreeSwordRenderer extends EntityRenderer<OriginFreeSwordEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final float[][] VERTICES = {
            {0.0F, 0.0F, 417.7431F},
            {0.0F, -44.6113F, -30.0F},
            {38.9907F, 0.0F, -20.0F},
            {0.0F, 44.6113F, -30.0F},
            {-38.9907F, 0.0F, -20.0F},
            {30.9907F, 0.0F, -50.0F},
            {-30.9907F, 0.0F, -50.0F},
            {0.0F, 0.0F, -214.0305F},
            {159.1439F, 0.0F, -30.0F},
            {-159.1439F, 0.0F, -30.0F}
    };

    private static final int[][] FACES = {
            {0, 2, 1}, {0, 3, 2}, {0, 4, 3}, {0, 1, 4},
            {1, 5, 7}, {5, 3, 7}, {3, 6, 7}, {6, 1, 7},
            {2, 8, 1}, {5, 8, 3}, {4, 9, 3}, {6, 9, 1},
            {1, 8, 5}, {1, 9, 4}, {3, 8, 2}, {3, 9, 6}
    };

    public OriginFreeSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(OriginFreeSwordEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float alpha = age <= entity.getDelay()
                ? 0.35F + 0.45F * (age / Math.max(1.0F, entity.getDelay()))
                : 0.9F;

        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() + age * 18.0F));
        poseStack.scale(0.00225F, 0.00225F, 0.0045F);

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        for (int[] face : FACES) {
            vertex(vc, mat, VERTICES[face[0]], r, g, b, alpha);
            vertex(vc, mat, VERTICES[face[1]], r, g, b, alpha);
            vertex(vc, mat, VERTICES[face[2]], r, g, b, alpha);
            vertex(vc, mat, VERTICES[face[2]], r, g, b, alpha);
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float[] p, float r, float g, float b, float a) {
        vc.vertex(mat, p[0], p[1], p[2]).color(r, g, b, a).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(OriginFreeSwordEntity entity) {
        return TEX;
    }
}

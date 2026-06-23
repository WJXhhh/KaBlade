package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.PhantomSwordExEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 幻影剑Ex 渲染器 —— 绘制一个带颜色的菱形剑体。
 * 同时用于 {@link com.wjx.kablade.entity.LightningSwordEntity}。
 */
public class PhantomSwordExRenderer extends EntityRenderer<PhantomSwordExEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    /** Exact 1.12.2 sword silhouette; units are converted by the legacy 0.0045 scale. */
    private static final float[][] VERTICES = {
            {0.0F, 0.0F, 417.7431F},
            {0.0F, -44.6113F, 0.0F},
            {38.9907F, 0.0F, 50.0F},
            {0.0F, 44.6113F, 0.0F},
            {-38.9907F, 0.0F, 50.0F},
            {38.9907F, 0.0F, -50.0F},
            {-38.9907F, 0.0F, -50.0F},
            {0.0F, 0.0F, -214.0305F},
            {159.1439F, 0.0F, -49.6611F},
            {-159.1439F, 0.0F, -49.6611F}
    };

    private static final int[][] FACES = {
            {0, 2, 1}, {0, 3, 2}, {0, 4, 3}, {0, 1, 4},
            {1, 5, 7}, {5, 3, 7}, {3, 6, 7}, {6, 1, 7},
            {2, 8, 1}, {5, 8, 3}, {4, 9, 3}, {6, 9, 1},
            {1, 8, 5}, {1, 9, 4}, {3, 8, 2}, {3, 9, 6}
    };

    public PhantomSwordExRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(PhantomSwordExEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float alpha = 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
        poseStack.scale(0.0045F, 0.0045F, 0.0045F);
        poseStack.scale(0.5F, 0.5F, 1.0F);

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

    private static void vertex(VertexConsumer vc, Matrix4f mat, float[] p,
                               float r, float g, float b, float a) {
        vc.vertex(mat, p[0], p[1], p[2]).color(r, g, b, a).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomSwordExEntity entity) {
        return TEX;
    }
}

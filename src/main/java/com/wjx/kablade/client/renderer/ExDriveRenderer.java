package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.ExSlashDriveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 可变速驱动实体渲染器 —— 绘制一个带颜色的发光矩形薄片（类似 1.12.2 的飞斩渲染）。
 * 同时用于 {@link com.wjx.kablade.entity.FlareEdgeEntity} 和
 * {@link com.wjx.kablade.entity.AquaEdgeEntity}（共用同一渲染器）。
 */
public class ExDriveRenderer extends EntityRenderer<ExSlashDriveEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    public ExDriveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ExSlashDriveEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = 0.85F;

        float scX = entity.getScaleX();
        float scY = entity.getScaleY();
        float scZ = entity.getScaleZ();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() + entity.tickCount * 18.0F));
        poseStack.scale(scX, scY, scZ);

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        // 一个扁平的菱形/矩形刀光形状
        float hw = 1.0F, hh = 0.15F, hl = 2.0F;
        // 前箭头
        vertex(vc, mat, 0, 0, hl, r, g, b, a);
        vertex(vc, mat, -hw, -hh, 0, r, g, b, a);
        vertex(vc, mat, hw, -hh, 0, r, g, b, a);
        vertex(vc, mat, hw, hh, 0, r, g, b, a);
        vertex(vc, mat, -hw, hh, 0, r, g, b, a);
        vertex(vc, mat, -hw, -hh, 0, r, g, b, a);
        // 后部
        vertex(vc, mat, 0, 0, -hl * 0.6F, r, g, b, a);
        vertex(vc, mat, -hw * 0.5F, -hh, 0, r, g, b, a);
        vertex(vc, mat, hw * 0.5F, -hh, 0, r, g, b, a);
        vertex(vc, mat, hw * 0.5F, hh, 0, r, g, b, a);
        vertex(vc, mat, -hw * 0.5F, hh, 0, r, g, b, a);
        vertex(vc, mat, -hw * 0.5F, -hh, 0, r, g, b, a);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                               float r, float g, float b, float a) {
        vc.vertex(mat, x, y, z).color(r, g, b, a).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ExSlashDriveEntity entity) {
        return TEX;
    }
}

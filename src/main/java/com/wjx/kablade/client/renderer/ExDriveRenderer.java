package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.ExSlashDriveEntity;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 可变速驱动实体渲染器 —— 使用 OBJ 模型绘制 3D 刀光（匹配 Resharped DriveRenderer 行为）。
 * <p>
 * 同时用于 {@link com.wjx.kablade.entity.FlareEdgeEntity} 和
 * {@link com.wjx.kablade.entity.AquaEdgeEntity}。
 */
public class ExDriveRenderer extends EntityRenderer<ExSlashDriveEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "model/util/ss.png");
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath("kablade", "model/util/drive.obj");

    public ExDriveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ExSlashDriveEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // ── 亮度衰减 ─────────────────────────────────────────
        float lifetime = entity.getLifetime();
        double t = Mth.clamp(lifetime - entity.tickCount + (double) partialTick, 0.0, lifetime);
        double ratio = t / lifetime;
        double alpha = Math.max(0.0, 0.75 - Math.pow(ratio - 1.0, 4.0));

        // ── 变换 ──────────────────────────────────────────────
        poseStack.pushPose();
        // 旋转序列严格对齐 Resharped DriveRenderer（模型 drive.obj 即按此约定建模）：
        //   YP(yaw-90) → ZP(pitch) → XP(roll) → scale → YP(+90)
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRoll()));

        // scale: Resharped 固定 0.015（模型是整刀大小）
        poseStack.scale(0.03F, 0.03F, 0.03F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        // ── 颜色 ──────────────────────────────────────────────
        int color = entity.getColor() & 0xFFFFFF;
        int a = (int) (Mth.clamp(alpha, 0.0, 1.0) * 255.0) & 0xFF;
        BladeRenderState.setCol(color | (a << 24));

        // ── 渲染 ──────────────────────────────────────────────
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "base", TEXTURE, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ExSlashDriveEntity entity) {
        return TEXTURE;
    }
}

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.FengxuanDimensionEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * Renderer for Fengxuan's modified SlashDimension, following the 1.12.2 RenderSlashDimensionAdd shape.
 */
public final class FengxuanDimensionRenderer extends EntityRenderer<FengxuanDimensionEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath("kablade", "model/util/sahuixuanqiu/model.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "model/util/sahuixuanqiu/texture.png");
    private static final ItemStack EMPTY = ItemStack.EMPTY;
    private static final boolean RENDER_HIGH_MODEL = true;
    private static final double FULL_DETAIL_DISTANCE_SQR = 14.0D * 14.0D;
    private static final double MAX_RENDER_DISTANCE_SQR = 36.0D * 36.0D;
    private static final int WIND_SEQUENCE_COUNT = 5;

    public FengxuanDimensionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(FengxuanDimensionEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double dx = entity.getX() - cameraX;
        double dy = entity.getY() - cameraY;
        double dz = entity.getZ() - cameraZ;
        if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQR) {
            return false;
        }
        return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public void render(FengxuanDimensionEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!RENDER_HIGH_MODEL) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        double distanceSqr = distanceToCameraSqr(entity);
        if (distanceSqr > MAX_RENDER_DISTANCE_SQR) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        float age = entity.tickCount + partialTick;
        int color = entity.getColor();
        double alpha = alpha(entity, partialTick);
        if (alpha <= 0.01D) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        int baseColor = shiftedDimColor(color);
        boolean fullDetail = distanceSqr <= FULL_DETAIL_DISTANCE_SQR;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSeed()));
        poseStack.scale(0.01F, 0.01F, 0.01F);

        try {
            renderBaseLayers(model, poseStack, buffer, packedLight, baseColor, alpha, fullDetail ? 2 : 1);
            if (fullDetail) {
                renderPulseLayer(entity, partialTick, model, poseStack, buffer, packedLight, baseColor, alpha);
            }
            renderWindLayers(entity, age, model, poseStack, buffer, color, alpha, fullDetail ? 2 : 1);
        } finally {
            BladeRenderState.resetCol();
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderBaseLayers(WavefrontObject model, PoseStack poseStack, MultiBufferSource buffer,
                                         int packedLight, int baseColor, double alpha, int layers) {
        int layerAlpha = ((int) (0x78 * alpha) & 0xFF) << 24;
        poseStack.pushPose();
        for (int i = 0; i < layers; i++) {
            if (i > 0) {
                poseStack.scale(0.92F, 0.92F, 0.92F);
            }
            BladeRenderState.setCol(baseColor | layerAlpha);
            renderReverseLuminous(model, "base", poseStack, buffer, packedLight);
        }
        poseStack.popPose();
    }

    private static void renderPulseLayer(FengxuanDimensionEntity entity, float partialTick,
                                         WavefrontObject model, PoseStack poseStack,
                                         MultiBufferSource buffer, int packedLight, int baseColor, double alpha) {
        float ticks = 15.0F;
        float wave = (entity.tickCount + partialTick) % ticks;
        int waveAlpha = (int) (0x88 * ((ticks - wave) / ticks) * alpha) & 0xFF;
        if (waveAlpha < 6) {
            return;
        }
        float waveScale = 1.0F + 0.03F * wave;
        poseStack.pushPose();
        poseStack.scale(waveScale, waveScale, waveScale);
        BladeRenderState.setCol(baseColor | (waveAlpha << 24));
        renderReverseLuminous(model, "base", poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void renderWindLayers(FengxuanDimensionEntity entity, float age,
                                         WavefrontObject model, PoseStack poseStack,
                                         MultiBufferSource buffer, int color, double alpha, int windCount) {
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < windCount; i++) {
            int windSlot = i * WIND_SEQUENCE_COUNT / windCount;
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees((360.0F / WIND_SEQUENCE_COUNT) * windSlot));
            poseStack.mulPose(Axis.YP.rotationDegrees(30.0F));

            double rotWind = 360.0D / 20.0D;
            double offsetBase = 7.0D;
            double offset = windSlot * offsetBase;
            double motionLen = offsetBase * (WIND_SEQUENCE_COUNT - 1);
            double offsetTicks = age + entity.getSeed() + offset;
            double progress = (offsetTicks % motionLen) / motionLen;
            double rad = Math.PI * 2.0D * progress;
            float windScale = (float) (0.4D + progress);
            int windAlpha = (int) (0xD0 * Math.max(0.0D, -Math.sin(rad)) * alpha) & 0xFF;

            if (windAlpha >= 6) {
                poseStack.scale(windScale, windScale, windScale);
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (rotWind * offsetTicks)));
                BladeRenderState.setCol(rgb | (windAlpha << 24));
                renderColorWrite(model, "wind", poseStack, buffer);
            }
            poseStack.popPose();
        }
    }

    private static void renderReverseLuminous(WavefrontObject model, String part, PoseStack poseStack,
                                              MultiBufferSource buffer, int packedLight) {
        BladeRenderState.renderOverridedReverseLuminous(
                EMPTY, model, part, TEXTURE, poseStack, buffer, packedLight);
    }

    private static void renderColorWrite(WavefrontObject model, String part,
                                         PoseStack poseStack, MultiBufferSource buffer) {
        BladeRenderState.renderOverridedColorWrite(
                EMPTY, model, part, TEXTURE, poseStack, buffer, BladeRenderState.MAX_LIGHT);
    }

    private static int shiftedDimColor(int color) {
        int rgb = color & 0xFFFFFF;
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        return Color.HSBtoRGB(0.5F + hsb[0], hsb[1], 0.2F) & 0xFFFFFF;
    }

    private static double alpha(FengxuanDimensionEntity entity, float partialTick) {
        double lifetime = entity.getLifetime();
        double remaining = Mth.clamp(lifetime - entity.tickCount - partialTick, 0.0D, lifetime);
        double ratio = remaining / lifetime;
        return 1.0D - Math.pow(ratio - 1.0D, 4.0D);
    }

    private double distanceToCameraSqr(FengxuanDimensionEntity entity) {
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();
        double dx = entity.getX() - camera.x;
        double dy = entity.getY() - camera.y;
        double dz = entity.getZ() - camera.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public ResourceLocation getTextureLocation(FengxuanDimensionEntity entity) {
        return TEXTURE;
    }
}

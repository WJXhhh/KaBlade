package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.TunaEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Renderer for the falling tuna and its impact decals. */
public class TunaRenderer extends EntityRenderer<TunaEntity> {

    private static final ResourceLocation TUNA_MODEL =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/tuna/tuna.obj");
    private static final ResourceLocation TUNA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/tuna/tuna.png");
    private static final ResourceLocation IMPACT_MODEL =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/tuna/impact.obj");
    private static final ResourceLocation IMPACT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/tuna/impact.png");
    private static final ResourceLocation CRACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/tuna/crack.png");
    private static final float IMPACT_START_AGE = 2.0F;
    private static final float IMPACT_PEAK_AGE = 5.0F;
    private static final float IMPACT_END_AGE = 11.0F;

    public TunaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(TunaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        renderTuna(entity, age, poseStack, buffer, packedLight);
        renderCrack(age, poseStack, buffer);
        renderImpact(age, poseStack, buffer, packedLight);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderTuna(TunaEntity entity, float age, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        if (age > 2.0F) {
            poseStack.translate(0.0D, -2.0D, 0.0D);
        } else {
            poseStack.translate(0.0D, 3.0D - age * 2.0D, 0.0D);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.scale(4.5F, 3.5F, 4.5F);

        BladeRenderState.setCol(0xFFFFFFFF);
        WavefrontObject model = BladeModelManager.getInstance().getModel(TUNA_MODEL);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "tuna", TUNA_TEXTURE, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void renderCrack(float age, PoseStack poseStack, MultiBufferSource buffer) {
        if (age <= IMPACT_START_AGE) {
            return;
        }
        float finalT = Mth.clamp(age, IMPACT_START_AGE, IMPACT_PEAK_AGE + 2.0F);
        float scale = (finalT - IMPACT_START_AGE) / 15.0F;
        if (scale <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.01D, 0.0D);
        poseStack.scale(scale, 1.0F, scale);

        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.windEnchantment(CRACK_TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        float half = 10.0F;
        vertex(vc, mat, -half, -half, 0.0F, 0.0F);
        vertex(vc, mat, half, -half, 1.0F, 0.0F);
        vertex(vc, mat, half, half, 1.0F, 1.0F);
        vertex(vc, mat, -half, half, 0.0F, 1.0F);
        poseStack.popPose();
    }

    private static void renderImpact(float age, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight) {
        if (age < IMPACT_START_AGE || age >= IMPACT_END_AGE) {
            return;
        }

        float scale = age < IMPACT_PEAK_AGE
                ? Mth.lerp((age - IMPACT_START_AGE) / (IMPACT_PEAK_AGE - IMPACT_START_AGE),
                        0.85F, 1.6F)
                : Mth.lerp((age - IMPACT_PEAK_AGE) / (IMPACT_END_AGE - IMPACT_PEAK_AGE),
                        1.6F, 0.0F);
        if (scale <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        BladeRenderState.setCol(0xFFFFFFFF);
        WavefrontObject model = BladeModelManager.getInstance().getModel(IMPACT_MODEL);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "plat", IMPACT_TEXTURE, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float z, float u, float v) {
        vc.vertex(mat, x, 0.0F, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(TunaEntity entity) {
        return TUNA_TEXTURE;
    }
}

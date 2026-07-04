package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.SummonedHedraEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Renderer for Pledge of Rain's summoned light hedra. */
public class SummonedHedraRenderer extends EntityRenderer<SummonedHedraEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/hedra.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/3.png");

    public SummonedHedraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SummonedHedraEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.1D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
        poseStack.scale(2.0F, 2.0F, 2.0F);

        BladeRenderState.setCol(0xFFFFFFFF);
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY, model, "Hedra001", TEXTURE, poseStack, buffer, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SummonedHedraEntity entity) {
        return TEXTURE;
    }
}

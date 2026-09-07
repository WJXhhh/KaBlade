package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.TreasonMissileEntity;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Dedicated textured missile mesh with a lit hull and emissive engine. */
public final class TreasonMissileRenderer extends EntityRenderer<TreasonMissileEntity> {
    private static final ResourceLocation MODEL = ResourceUtil.getLocation("model/entity/treason_missile/mdl.obj");
    private static final ResourceLocation TEXTURE = ResourceUtil.getLocation("model/entity/treason_missile/tex.png");

    public TreasonMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(TreasonMissileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        BladeRenderState.setCol(0xFFFFFFFF);
        BladeRenderState.renderOverrided(ItemStack.EMPTY, model, "body", TEXTURE,
                poseStack, buffer, packedLight);
        BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "glow", TEXTURE,
                poseStack, buffer, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TreasonMissileEntity entity) {
        return TEXTURE;
    }
}

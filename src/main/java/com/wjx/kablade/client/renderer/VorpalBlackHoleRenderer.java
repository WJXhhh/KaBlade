package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.model.VorpalBlackHoleModel;
import com.wjx.kablade.entity.VorpalBlackHoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 反力场黑洞渲染器 —— 一比一复刻 1.12.2 的 {@code RenderVorpalBlackHole}。
 * <p>
 * 使用 3D 方块模型（非公告板），变换序列完全照搬原版：翻转 {@code (-1,-1,1)} → 放大 2 倍
 * → {@code translate(0, -1.501, 0)}，贴图 {@code textures/entity/blackhole/texture.png}。
 */
public class VorpalBlackHoleRenderer extends EntityRenderer<VorpalBlackHoleEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/blackhole/texture.png");

    private final VorpalBlackHoleModel model;

    public VorpalBlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VorpalBlackHoleModel(context.bakeLayer(VorpalBlackHoleModel.LAYER));
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(VorpalBlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 1.12.2 RenderVorpalBlackHole 的变换序列：先翻转到世界朝向，再整体放大 2 倍，最后下移 1.501。
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        VertexConsumer vc = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VorpalBlackHoleEntity entity) {
        return TEXTURE;
    }
}

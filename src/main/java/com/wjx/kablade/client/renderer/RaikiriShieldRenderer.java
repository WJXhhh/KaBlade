package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.model.RaikiriShieldModel;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 雷切护盾渲染器 —— 复刻 1.12.2 WorldEvent + RenderRaikiriBlade 的全部渲染逻辑。
 * <p>
 * 使用 mdlRaikiriBlade 模型 + additive 全亮度青白渲染，绕 Y 轴自转。
 */
@OnlyIn(Dist.CLIENT)
public class RaikiriShieldRenderer extends EntityRenderer<RaikiriShieldEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/effect/tex_raikiri_blade.png");

    private final RaikiriShieldModel model;

    public RaikiriShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RaikiriShieldModel(context.bakeLayer(RaikiriShieldModel.LAYER));
    }

    @Override
    public void render(RaikiriShieldEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float blood = entity.getShieldBlood();
        if (blood <= 0.0F) return;

        float h = Math.min(1.0F, blood / 10.0F);
        float alpha = 0.25F + 0.4F * h;          // 0.25 ~ 0.65

        // 颜色：青白 (0, 1, 1)，复刻 1.12.2 GlStateManager.color(0f, 1f, 1f, 0.5f)
        float r = 0.0F;
        float g = 1.0F;
        float b = 1.0F;

        // 抬高到腰部稍上（复刻 1.12.2 原版在玩家身体周围的位置）
        float yOff = 0.35F;

        poseStack.pushPose();
        poseStack.translate(0.0, yOff, 0.0);

        // 复刻 1.12.2 prepareScale: scale(-1, -1, 1) then translate(0, -1.501, 0)
        // scale(-1, -1, 1) 翻转 X 和 Y，后续 translate 在翻转后的坐标系中
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        // 绕 Y 轴缓慢自转，partialTick 插值确保帧间平滑
        float angle = (entity.tickCount + partialTick) * 0.012F;
        poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(angle * 360.0F));

        // 使用 fullbright additive 渲染（复刻 disableLighting + setLightmapTextureCoords(240,240)）
        // RenderType.lightning() 使用 POSITION_COLOR 格式，忽略纹理/光照
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        model.renderToBuffer(poseStack, consumer, 0xF000F0, 0, r, g, b, alpha);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RaikiriShieldEntity entity) {
        return TEXTURE;
    }
}

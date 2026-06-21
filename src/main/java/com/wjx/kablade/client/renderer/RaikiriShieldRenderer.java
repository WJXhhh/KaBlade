package com.wjx.kablade.client.renderer;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 雷切护盾渲染器。
 * <p>
 * 1.12.2 原版的渲染逻辑已被注释掉（实体实际不可见），这里保持空渲染，
 * 仅提供必要的 getTextureLocation 实现以避免客户端注册异常。
 */
@OnlyIn(Dist.CLIENT)
public class RaikiriShieldRenderer extends EntityRenderer<RaikiriShieldEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/effect/tex_raikiri_blade.png");

    public RaikiriShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RaikiriShieldEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 1.12.2 原版无实际渲染，保持为空。
    }

    @Override
    public ResourceLocation getTextureLocation(RaikiriShieldEntity entity) {
        return TEXTURE;
    }
}

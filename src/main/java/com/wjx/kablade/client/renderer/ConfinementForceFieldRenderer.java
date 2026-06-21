package com.wjx.kablade.client.renderer;

import com.wjx.kablade.entity.ConfinementForceFieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 禁锢力场渲染器。
 * <p>
 * 1.12.2 原版的 {@code RenderConfinementForceField#doRender} 为空实现，
 * 这里同样保持空渲染，仅提供必要的 getTextureLocation 实现以避免客户端注册异常。
 */
@OnlyIn(Dist.CLIENT)
public class ConfinementForceFieldRenderer extends EntityRenderer<ConfinementForceFieldEntity> {

    public ConfinementForceFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ConfinementForceFieldEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 1.12.2 原版无实际渲染，保持为空。
    }

    @Override
    public ResourceLocation getTextureLocation(ConfinementForceFieldEntity entity) {
        return null;
    }
}

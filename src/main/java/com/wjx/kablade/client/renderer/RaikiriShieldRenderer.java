package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 雷切护盾渲染器 —— 绘制一道贴身的青白色环形电弧，随护盾耐久变暗。
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
        float blood = entity.getShieldBlood();
        if (blood <= 0.0F) return;

        // 耐久越少越暗越透明
        float alpha = Math.min(1.0F, blood / 10.0F);
        float brightness = 0.3F + 0.7F * alpha;
        int r = (int) (180 * brightness);
        int g = (int) (220 * brightness);
        int b = (int) (255 * brightness);

        poseStack.pushPose();
        // 贴身环绕 (以玩家脚下为中心)
        double t = (entity.tickCount + partialTick) * 0.12;
        float radius = 0.85F;
        float height = 1.6F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        // 两道交叉的环形电弧
        for (int ring = 0; ring < 2; ring++) {
            float phase = ring * (float) Math.PI / 2.0F;
            int segments = 16;
            for (int i = 0; i < segments; i++) {
                float a0 = (float) (Math.PI * 2.0 * i / segments + t + phase);
                float a1 = (float) (Math.PI * 2.0 * (i + 1) / segments + t + phase);
                float y0 = (float) (Math.sin(a0 * 2.0) * height * 0.3 + height * 0.5);
                float y1 = (float) (Math.sin(a1 * 2.0) * height * 0.3 + height * 0.5);
                float x0 = (float) (Math.cos(a0) * radius);
                float z0 = (float) (Math.sin(a0) * radius);
                float x1 = (float) (Math.cos(a1) * radius);
                float z1 = (float) (Math.sin(a1) * radius);

                consumer.vertex(mat, x0, y0 - 0.5F, z0).color(r, g, b, (int) (alpha * 120)).endVertex();
                consumer.vertex(mat, x1, y1 - 0.5F, z1).color(r, g, b, (int) (alpha * 120)).endVertex();
            }
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RaikiriShieldEntity entity) {
        return TEXTURE;
    }
}

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 雷切护盾渲染器 —— 复刻 1.12.2 WorldEvent 的 RenderWorldLastEvent 渲染逻辑，
 * 改为标准实体渲染，使用 additive blending + 全亮度 + 青白色。
 * <p>
 * 在玩家胸部高度绘制十字星芒，绕 Y 轴自转，随护盾耐久变暗变透明。
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

        // 耐久比例决定亮度和透明度
        float healthRatio = Math.min(1.0F, blood / 10.0F);
        float alpha = 0.25F + 0.35F * healthRatio;      // 0.25 ~ 0.60

        // 第一人称：Minecraft.getInstance().cameraEntity 就是 player 自己
        // 护盾实体紧随玩家，渲染抬高到胸部位置使其在视线内
        float yOff = 0.85F;  // 胸部高度

        // 旋转角度（随时间自转，复刻 1.12.2 angleManager.getAngle()）
        float angle = (entity.tickCount + partialTick) * 0.08F;

        // 使用光线追踪层次（雷电）渲染类型，支持透明度
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        // ── 十字星芒：四根扁平臂 ──
        // 与 1.12.2 模型对应：mdlRaikiriBlade 在水平面有 4 组扁平板
        float armLen = 0.9F * (0.5F + 0.5F * healthRatio);
        float armWid = 0.08F;
        float r = 0.5F;
        float g = 0.9F;
        float b = 1.0F;
        int a = (int) (alpha * 220);

        poseStack.pushPose();
        // 抬高到胸部，绕 Y 轴旋转
        poseStack.translate(0.0, yOff, 0.0);
        poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(angle * 360.0F));
        mat = poseStack.last().pose();

        // 四条臂（X 正、X 负、Z 正、Z 负）
        float[][] arms = {
                { armLen, 0, 0, -armLen, 0, 0 },   // X+
                { -armLen, 0, 0, armLen, 0, 0 },    // X-
                { 0, 0, armLen, 0, 0, -armLen },    // Z+
                { 0, 0, -armLen, 0, 0, armLen }     // Z-
        };

        for (float[] arm : arms) {
            // 每条臂由两根平行线构成（宽 armWid）
            for (int side = -1; side <= 1; side += 2) {
                float wx = arm[3] == 0 ? armWid * side : 0;
                float wz = arm[5] == 0 ? armWid * side : 0;
                // 起点
                consumer.vertex(mat, arm[0] + wx, arm[1], arm[2] + wz).color(r, g, b, a).endVertex();
                // 终点
                consumer.vertex(mat, arm[3] + wx, arm[4], arm[5] + wz).color(r, g, b, a).endVertex();
            }
            // 两条对角线交叉线在臂端连接
            if (armLen > 0.3F) {
                float tip = 0.06F;
                consumer.vertex(mat, arm[3] + tip, arm[4], arm[5] + tip).color(r, g, b, a).endVertex();
                consumer.vertex(mat, arm[3] - tip, arm[4], arm[5] - tip).color(r, g, b, a).endVertex();
            }
        }

        // 额外小圆环（护盾轮廓）
        int segments = 12;
        float ringR = armLen * 0.65F;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / segments);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / segments);
            float x0 = (float) (Math.cos(a0) * ringR);
            float z0 = (float) (Math.sin(a0) * ringR);
            float x1 = (float) (Math.cos(a1) * ringR);
            float z1 = (float) (Math.sin(a1) * ringR);
            consumer.vertex(mat, x0, 0, z0).color(r * 0.7F, g * 0.7F, b * 0.7F, a / 2).endVertex();
            consumer.vertex(mat, x1, 0, z1).color(r * 0.7F, g * 0.7F, b * 0.7F, a / 2).endVertex();
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RaikiriShieldEntity entity) {
        return TEXTURE;
    }
}

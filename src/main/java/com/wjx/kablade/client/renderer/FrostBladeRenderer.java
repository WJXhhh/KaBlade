package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.FrostBladeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 崩坏 3 风格寒霜灵刃渲染器：冰晶剑体、双层高速长尾与命中后的放射状晶簇。
 * 全部几何均程序化生成，不依赖额外贴图。
 */
public class FrostBladeRenderer extends EntityRenderer<FrostBladeEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final float CYAN_R = 0.08F;
    private static final float CYAN_G = 0.82F;
    private static final float CYAN_B = 1.0F;

    public FrostBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FrostBladeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        if (entity.isImpacting()) {
            renderImpact(entity, partialTick, poseStack, vc);
        } else {
            renderFlyingBlade(entity, partialTick, poseStack, vc);
        }
    }

    private static void renderFlyingBlade(FrostBladeEntity entity, float partialTick,
                                          PoseStack poseStack, VertexConsumer vc) {
        float age = entity.tickCount + partialTick;
        float appear = Mth.clamp(age / 2.0F, 0.0F, 1.0F);
        float pulse = 0.96F + 0.04F * Mth.sin(age * 1.4F);
        float finisherScale = entity.isFinisher() ? 1.28F : 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.12D, 0.0D);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.scale(appear * pulse * finisherScale,
                appear * pulse * finisherScale,
                appear * pulse * finisherScale);

        Matrix4f mat = poseStack.last().pose();
        boolean moving = entity.getInterval() <= 0 || age > entity.getInterval();
        if (moving) {
            float trail = entity.isFinisher() ? 5.1F : 3.8F;
            drawTrail(vc, mat, trail, 0.34F, CYAN_R, CYAN_G, CYAN_B, 0.30F);
            drawTrail(vc, mat, trail * 0.72F, 0.105F, 0.92F, 1.0F, 1.0F, 0.88F);
        } else {
            // 两 tick 的显形阶段：剑下方先亮起一小段冰蓝芒，随后再飞出。
            drawTrail(vc, mat, 1.15F, 0.28F, CYAN_R, CYAN_G, CYAN_B, 0.24F);
        }

        drawBlade(vc, mat, 1.0F, 0.33F, CYAN_R, CYAN_G, CYAN_B, 0.38F);
        drawBlade(vc, mat, 0.88F, 0.205F, 0.32F, 0.94F, 1.0F, 0.92F);
        drawBlade(vc, mat, 0.74F, 0.078F, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static void renderImpact(FrostBladeEntity entity, float partialTick,
                                     PoseStack poseStack, VertexConsumer vc) {
        float impactAge = Math.max(0.0F, entity.tickCount + partialTick - entity.getImpactTick());
        float duration = entity.isFinisher() ? 11.0F : 8.0F;
        float t = Mth.clamp(impactAge / duration, 0.0F, 1.0F);
        float alpha = Mth.clamp(impactAge / 1.25F, 0.0F, 1.0F)
                * Mth.clamp((duration - impactAge) / 4.5F, 0.0F, 1.0F);
        if (alpha <= 0.01F) {
            return;
        }

        float size = (entity.isFinisher() ? 1.55F : 1.0F) * (0.62F + 0.58F * easeOut(t));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.getId() * 37) % 360 + impactAge * 7.0F));
        poseStack.scale(size, size, size);

        // 中心白核：三片互相垂直的尖锐晶面。
        Matrix4f center = poseStack.last().pose();
        drawBlade(vc, center, 0.78F, 0.30F, 0.72F, 0.98F, 1.0F, alpha * 0.88F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        drawBlade(vc, poseStack.last().pose(), 0.78F, 0.30F,
                0.72F, 0.98F, 1.0F, alpha * 0.76F);
        poseStack.popPose();

        // 八向晶刺由中心爆开；终结剑更长，复刻参考画面的巨大十字晶化命中。
        int spikes = entity.isFinisher() ? 10 : 8;
        for (int i = 0; i < spikes; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * (360.0F / spikes)));
            poseStack.mulPose(Axis.XP.rotationDegrees(-38.0F + (i % 3) * 36.0F));
            float length = (1.25F + (i % 4) * 0.18F) * (0.72F + 0.42F * easeOut(t));
            drawSpike(vc, poseStack.last().pose(), length, 0.17F + (i & 1) * 0.045F,
                    CYAN_R, CYAN_G, CYAN_B, alpha * 0.80F);
            poseStack.popPose();
        }

        // 快速扩散的双环让命中读起来更像“爆闪”，而不是静止的一团模型。
        float ringRadius = 0.38F + easeOut(t) * (entity.isFinisher() ? 1.55F : 1.15F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        drawRing(vc, poseStack.last().pose(), ringRadius, 0.095F,
                0.70F, 0.98F, 1.0F, alpha * (1.0F - t));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        drawRing(vc, poseStack.last().pose(), ringRadius * 0.82F, 0.065F,
                1.0F, 1.0F, 1.0F, alpha * 0.72F * (1.0F - t));
        poseStack.popPose();

        poseStack.popPose();
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    /** 沿 +Z 的冰晶剑，两张交叉菱形面让任意观察角度都保持清晰轮廓。 */
    private static void drawBlade(VertexConsumer vc, Matrix4f mat, float scale, float width,
                                  float r, float g, float b, float alpha) {
        float tip = 1.72F * scale;
        float shoulder = 0.18F * scale;
        float base = -0.62F * scale;
        float guardZ = -0.34F * scale;
        float guardWidth = width * 1.65F;

        quad(vc, mat,
                0.0F, 0.0F, tip, r, g, b, alpha,
                width, 0.0F, shoulder, r, g, b, alpha * 0.82F,
                0.0F, 0.0F, base, r, g, b, alpha * 0.45F,
                -width, 0.0F, shoulder, r, g, b, alpha * 0.82F);
        quad(vc, mat,
                0.0F, 0.0F, tip, r, g, b, alpha,
                0.0F, width, shoulder, r, g, b, alpha * 0.82F,
                0.0F, 0.0F, base, r, g, b, alpha * 0.45F,
                0.0F, -width, shoulder, r, g, b, alpha * 0.82F);

        // 小型菱形护手，令轮廓更接近参考图中的机械冰刃，而非普通粒子线。
        quad(vc, mat,
                0.0F, 0.0F, guardZ + width * 0.55F, r, g, b, alpha * 0.82F,
                guardWidth, 0.0F, guardZ, r, g, b, alpha * 0.66F,
                0.0F, 0.0F, guardZ - width * 0.55F, r, g, b, alpha * 0.45F,
                -guardWidth, 0.0F, guardZ, r, g, b, alpha * 0.66F);
    }

    private static void drawTrail(VertexConsumer vc, Matrix4f mat, float length, float width,
                                  float r, float g, float b, float alpha) {
        float front = -0.18F;
        float tail = -length;
        quad(vc, mat,
                -width, 0.0F, front, r, g, b, alpha,
                width, 0.0F, front, r, g, b, alpha,
                0.0F, 0.0F, tail, r, g, b, 0.0F,
                0.0F, 0.0F, tail, r, g, b, 0.0F);
        quad(vc, mat,
                0.0F, -width, front, r, g, b, alpha,
                0.0F, width, front, r, g, b, alpha,
                0.0F, 0.0F, tail, r, g, b, 0.0F,
                0.0F, 0.0F, tail, r, g, b, 0.0F);
    }

    private static void drawSpike(VertexConsumer vc, Matrix4f mat, float length, float width,
                                  float r, float g, float b, float alpha) {
        quad(vc, mat,
                0.0F, 0.0F, length, 1.0F, 1.0F, 1.0F, alpha,
                width, 0.0F, 0.0F, r, g, b, alpha * 0.72F,
                0.0F, 0.0F, -0.18F, r, g, b, 0.0F,
                -width, 0.0F, 0.0F, r, g, b, alpha * 0.72F);
        quad(vc, mat,
                0.0F, 0.0F, length, 1.0F, 1.0F, 1.0F, alpha,
                0.0F, width, 0.0F, r, g, b, alpha * 0.72F,
                0.0F, 0.0F, -0.18F, r, g, b, 0.0F,
                0.0F, -width, 0.0F, r, g, b, alpha * 0.72F);
    }

    private static void drawRing(VertexConsumer vc, Matrix4f mat, float radius, float width,
                                 float r, float g, float b, float alpha) {
        int segments = 40;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / segments);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / segments);
            float outer = radius + width * 0.5F;
            float inner = radius - width * 0.5F;
            quad(vc, mat,
                    Mth.cos(a0) * outer, Mth.sin(a0) * outer, 0.0F, r, g, b, alpha,
                    Mth.cos(a1) * outer, Mth.sin(a1) * outer, 0.0F, r, g, b, alpha,
                    Mth.cos(a1) * inner, Mth.sin(a1) * inner, 0.0F, r, g, b, alpha * 0.45F,
                    Mth.cos(a0) * inner, Mth.sin(a0) * inner, 0.0F, r, g, b, alpha * 0.45F);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                             float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                             float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                             float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        vc.vertex(mat, x0, y0, z0).color(r0, g0, b0, a0).endVertex();
        vc.vertex(mat, x1, y1, z1).color(r1, g1, b1, a1).endVertex();
        vc.vertex(mat, x2, y2, z2).color(r2, g2, b2, a2).endVertex();
        vc.vertex(mat, x3, y3, z3).color(r3, g3, b3, a3).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FrostBladeEntity entity) {
        return TEX;
    }
}

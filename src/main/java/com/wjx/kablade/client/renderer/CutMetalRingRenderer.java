package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.CutMetalRingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 白色环形刀光：近满圆的薄弧带，淡入、扩张、留一点缺口，避免变成僵硬的平面圆盘。
 */
public class CutMetalRingRenderer extends EntityRenderer<CutMetalRingEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int SEGMENTS = 72;
    private static final float SPAN_DEG = 332.0F;
    private static final float BASE_RADIUS = 3.15F;
    private static final float BASE_THICKNESS = 0.34F;
    private static final float SPIN_DEG_PER_TICK = 14.0F;

    public CutMetalRingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(CutMetalRingEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        int life = entity.getLifetime();
        float t = Mth.clamp(age / life, 0.0F, 1.0F);

        float alpha = Mth.clamp(age / 2.0F, 0.0F, 1.0F)
                * Mth.clamp((life - age) / 7.0F, 0.0F, 1.0F);
        if (alpha <= 0.01F) {
            return;
        }

        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float size = entity.getSize();
        float grow = 0.72F + 0.42F * easeOut(t);
        float radius = BASE_RADIUS * size * grow;
        float thickness = BASE_THICKNESS * size * (1.0F - 0.28F * t);

        poseStack.pushPose();
        poseStack.translate(0.0, 0.03 * Mth.sin(age * 0.45F), 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + age * SPIN_DEG_PER_TICK));
        poseStack.mulPose(Axis.XP.rotationDegrees(8.0F + 4.0F * Mth.sin(age * 0.22F)));
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        // 柔光底 + 锋利高光，叠成拔刀剑风格的白银刀痕。
        buildRing(vc, mat, radius, thickness * 2.15F, r, g, b, alpha * 0.20F, 0.35F);
        buildRing(vc, mat, radius, thickness, r, g, b, alpha * 0.72F, 0.86F);
        buildRing(vc, mat, radius + thickness * 0.58F, thickness * 0.28F,
                1.0F, 1.0F, 1.0F, alpha * 0.92F, 1.0F);

        poseStack.popPose();
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static void buildRing(VertexConsumer vc, Matrix4f mat, float radius, float thickness,
                                  float r, float g, float b, float alpha, float taperBase) {
        float start = -SPAN_DEG * 0.5F;
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = i / (float) SEGMENTS;
            float f1 = (i + 1) / (float) SEGMENTS;
            float a0 = (float) Math.toRadians(start + SPAN_DEG * f0);
            float a1 = (float) Math.toRadians(start + SPAN_DEG * f1);
            float w0 = thickness * taper(f0, taperBase);
            float w1 = thickness * taper(f1, taperBase);

            float o0 = radius + w0 * 0.5F;
            float i0 = radius - w0 * 0.5F;
            float o1 = radius + w1 * 0.5F;
            float i1 = radius - w1 * 0.5F;

            float ox0 = Mth.cos(a0) * o0;
            float oz0 = Mth.sin(a0) * o0;
            float ix0 = Mth.cos(a0) * i0;
            float iz0 = Mth.sin(a0) * i0;
            float ox1 = Mth.cos(a1) * o1;
            float oz1 = Mth.sin(a1) * o1;
            float ix1 = Mth.cos(a1) * i1;
            float iz1 = Mth.sin(a1) * i1;

            float aEdge0 = alpha * taper(f0, 0.0F);
            float aEdge1 = alpha * taper(f1, 0.0F);
            quad(vc, mat,
                    ox0, 0.0F, oz0, r, g, b, aEdge0,
                    ox1, 0.0F, oz1, r, g, b, aEdge1,
                    ix1, 0.0F, iz1, r, g, b, aEdge1 * 0.42F,
                    ix0, 0.0F, iz0, r, g, b, aEdge0 * 0.42F);
            quad(vc, mat,
                    ix0, 0.0F, iz0, r, g, b, aEdge0 * 0.42F,
                    ix1, 0.0F, iz1, r, g, b, aEdge1 * 0.42F,
                    ox1, 0.0F, oz1, r, g, b, aEdge1,
                    ox0, 0.0F, oz0, r, g, b, aEdge0);
        }
    }

    private static float taper(float f, float base) {
        return Mth.clamp(base + (1.0F - base) * Mth.sin((float) Math.PI * f), 0.0F, 1.0F);
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
    public ResourceLocation getTextureLocation(CutMetalRingEntity entity) {
        return TEX;
    }
}

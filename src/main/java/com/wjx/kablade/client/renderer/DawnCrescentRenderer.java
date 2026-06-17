package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.DawnCrescentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 破晓弧月渲染器：用 {@code RenderType.lightning()} 程序化绘制一弯<b>实心炽亮</b>的金色月牙——
 * 一条恒定外半径、厚度向两端渐尖的弧带（两角成尖），随存活时间张大、淡出。
 * 朝向使其月面正对飞行方向（玩家身后看到完整弧光）。与极光帷幕的柔软多色形成强烈反差。
 */
public class DawnCrescentRenderer extends EntityRenderer<DawnCrescentEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int SEGMENTS = 32;
    private static final float SPAN_DEG = 46.0F;    // 弧的半张角（约 92° 弧 → 利落的月牙斩，不是大半圈的彩虹拱）
    private static final float OUTER_RADIUS = 2.6F;
    private static final float THICKNESS = 1.1F;
    private static final float SPIN_DEG_PER_TICK = 18.0F;   // 自旋速度

    public DawnCrescentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(DawnCrescentEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        int life = entity.getLifetime();
        float lifeFrac = Mth.clamp(age / life, 0.0F, 1.0F);

        float alpha = Mth.clamp(age / 2.0F, 0.0F, 1.0F)
                * Mth.clamp((life - age) / 5.0F, 0.0F, 1.0F) * 0.85F;
        if (alpha <= 0.01F) {
            return;
        }

        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float gs = entity.getSize() * (0.55F + 0.6F * lifeFrac);   // 边飞边张大
        float radius = OUTER_RADIUS * gs;
        float thick = THICKNESS * gs;
        float yShift = 0.0F;   // 弧心置于原点，自旋时在原地转而非绕圈漂

        poseStack.pushPose();
        // 躺平 + 绕垂直轴自旋：弧光平铺成水平光刃旋转飞掠（XP 90° 把竖直月面放平，ZP 在平面内自旋）。
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * SPIN_DEG_PER_TICK));
        Matrix4f mat = poseStack.last().pose();
        // 加色辉光（自发光质感）——单层即可，就是最初那种亮的观感。
        buildCrescent(buffer.getBuffer(RenderType.lightning()), mat, radius, thick, yShift, r, g, b, alpha);

        poseStack.popPose();
    }

    private static void buildCrescent(VertexConsumer vc, Matrix4f mat, float radius, float thick,
                                      float yShift, float r, float g, float b, float alpha) {
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = i / (float) SEGMENTS;
            float f1 = (i + 1) / (float) SEGMENTS;
            float a0 = (float) Math.toRadians(90.0 - SPAN_DEG + 2.0 * SPAN_DEG * f0);
            float a1 = (float) Math.toRadians(90.0 - SPAN_DEG + 2.0 * SPAN_DEG * f1);
            float th0 = thick * Mth.sin((float) Math.PI * f0);   // 两端渐尖
            float th1 = thick * Mth.sin((float) Math.PI * f1);

            float ox0 = Mth.cos(a0) * radius;
            float oy0 = Mth.sin(a0) * radius + yShift;
            float ox1 = Mth.cos(a1) * radius;
            float oy1 = Mth.sin(a1) * radius + yShift;
            float ix0 = Mth.cos(a0) * (radius - th0);
            float iy0 = Mth.sin(a0) * (radius - th0) + yShift;
            float ix1 = Mth.cos(a1) * (radius - th1);
            float iy1 = Mth.sin(a1) * (radius - th1) + yShift;

            // 外缘提亮做「刃口」高光
            float edge = 0.25F;
            float er = Math.min(r + edge, 1.0F);
            float eg = Math.min(g + edge, 1.0F);
            float eb = Math.min(b + edge, 1.0F);
            // 双面四边形：outer0 - outer1 - inner1 - inner0
            quad(vc, mat,
                    ox0, oy0, er, eg, eb, alpha,
                    ox1, oy1, er, eg, eb, alpha,
                    ix1, iy1, r, g, b, alpha,
                    ix0, iy0, r, g, b, alpha);
            quad(vc, mat,
                    ix0, iy0, r, g, b, alpha,
                    ix1, iy1, r, g, b, alpha,
                    ox1, oy1, er, eg, eb, alpha,
                    ox0, oy0, er, eg, eb, alpha);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float r0, float g0, float b0, float a0,
                             float x1, float y1, float r1, float g1, float b1, float a1,
                             float x2, float y2, float r2, float g2, float b2, float a2,
                             float x3, float y3, float r3, float g3, float b3, float a3) {
        vc.vertex(mat, x0, y0, 0.0F).color(r0, g0, b0, a0).endVertex();
        vc.vertex(mat, x1, y1, 0.0F).color(r1, g1, b1, a1).endVertex();
        vc.vertex(mat, x2, y2, 0.0F).color(r2, g2, b2, a2).endVertex();
        vc.vertex(mat, x3, y3, 0.0F).color(r3, g3, b3, a3).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DawnCrescentEntity entity) {
        return TEX;
    }
}

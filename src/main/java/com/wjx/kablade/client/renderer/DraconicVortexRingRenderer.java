package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.DraconicVortexRingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 「魔龙旋斩」环形旋刃渲染器：
 * 外侧浅红、内侧深红的渐变刃带，附加多层发散光晕与外缘炽亮光刃效果。
 */
public class DraconicVortexRingRenderer extends EntityRenderer<DraconicVortexRingEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int SEGMENTS = 72;
    private static final float SPAN_DEG = 332.0F;
    private static final float BASE_RADIUS = 3.20F;
    private static final float BASE_THICKNESS = 0.38F;
    private static final float SPIN_DEG_PER_TICK = 16.0F;

    public DraconicVortexRingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(DraconicVortexRingEntity entity, float entityYaw, float partialTick,
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
        float tintR = ((color >> 16) & 0xFF) / 255.0F;
        float tintG = ((color >> 8) & 0xFF) / 255.0F;
        float tintB = (color & 0xFF) / 255.0F;

        // 外侧浅红基调 (Light Red / Pink-Crimson Highlight)
        float outR = Math.min(1.0F, 1.0F * tintR);
        float outG = Math.min(1.0F, 0.38F + 0.20F * tintG);
        float outB = Math.min(1.0F, 0.42F + 0.20F * tintB);

        // 内侧深红基调 (Deep Crimson / Dark Dragon Blood Red)
        float inR = 0.48F * tintR;
        float inG = 0.02F * tintG;
        float inB = 0.04F * tintB;

        float size = entity.getSize();
        float grow = 0.72F + 0.45F * easeOut(t);
        float radius = BASE_RADIUS * size * grow;
        float thickness = BASE_THICKNESS * size * (1.0F - 0.25F * t);

        poseStack.pushPose();
        poseStack.translate(0.0, 0.04 * Mth.sin(age * 0.45F), 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + age * SPIN_DEG_PER_TICK));
        poseStack.mulPose(Axis.XP.rotationDegrees(8.0F + 4.0F * Mth.sin(age * 0.22F)));
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        // 1. 外部发散光晕层（宽幅柔光：外侧浅红光晕，内侧深红底光，提供丰满光效）
        buildGradientRing(vc, mat, radius * 1.03F, thickness * 2.35F,
                outR, outG * 0.85F, outB * 0.85F, alpha * 0.32F,
                inR, inG, inB, alpha * 0.15F,
                0.35F);

        // 2. 主体旋刃渐变层（外侧浅红，内侧深红）
        buildGradientRing(vc, mat, radius, thickness * 1.15F,
                outR, outG, outB, alpha * 0.88F,
                inR, inG, inB, alpha * 0.80F,
                0.86F);

        // 3. 内圈深红凝聚核（加深内侧深暗龙血质感）
        buildGradientRing(vc, mat, radius - thickness * 0.22F, thickness * 0.58F,
                inR * 1.25F, inG * 1.2F, inB * 1.2F, alpha * 0.60F,
                inR * 0.55F, 0.0F, inB * 0.4F, alpha * 0.85F,
                0.70F);

        // 4. 外缘极亮光刃层（浅红白炽高光，形成锐利的刀刃反光与锋芒光效）
        buildGradientRing(vc, mat, radius + thickness * 0.44F, thickness * 0.26F,
                1.0F, 0.82F, 0.86F, alpha * 0.95F,
                outR, outG * 0.9F, outB * 0.9F, alpha * 0.72F,
                1.0F);

        poseStack.popPose();
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    /**
     * 构建双面渐变环形带：
     * 外侧顶点应用 (outR, outG, outB, outA)，内侧顶点应用 (inR, inG, inB, inA)。
     */
    private static void buildGradientRing(VertexConsumer vc, Matrix4f mat,
                                         float radius, float thickness,
                                         float outR, float outG, float outB, float outA,
                                         float inR, float inG, float inB, float inA,
                                         float taperBase) {
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

            float tEdge0 = taper(f0, 0.0F);
            float tEdge1 = taper(f1, 0.0F);

            float aOut0 = outA * tEdge0;
            float aOut1 = outA * tEdge1;
            float aIn0 = inA * tEdge0;
            float aIn1 = inA * tEdge1;

            // 正反双面四边形
            quad(vc, mat,
                    ox0, 0.0F, oz0, outR, outG, outB, aOut0,
                    ox1, 0.0F, oz1, outR, outG, outB, aOut1,
                    ix1, 0.0F, iz1, inR, inG, inB, aIn1,
                    ix0, 0.0F, iz0, inR, inG, inB, aIn0);
            quad(vc, mat,
                    ix0, 0.0F, iz0, inR, inG, inB, aIn0,
                    ix1, 0.0F, iz1, inR, inG, inB, aIn1,
                    ox1, 0.0F, oz1, outR, outG, outB, aOut1,
                    ox0, 0.0F, oz0, outR, outG, outB, aOut0);
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
    public ResourceLocation getTextureLocation(DraconicVortexRingEntity entity) {
        return TEX;
    }
}

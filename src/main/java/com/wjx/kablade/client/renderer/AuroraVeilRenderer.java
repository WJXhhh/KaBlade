package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.AuroraVeilEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 极光帷幕渲染器：用 {@code RenderType.lightning()}（无贴图、全亮、加色）程序化绘制一道
 * 沿水平方向起伏、纵向半透明渐隐、横向极光色流转的帷幕，整体随存活时间淡入淡出。
 * 几何是一排竖直四边形，顶边做正弦波、深度做正弦涟漪 → 北极光那种飘动的幕帘感。
 */
public class AuroraVeilRenderer extends EntityRenderer<AuroraVeilEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int COLS = 16;
    private static final float BASE_WIDTH = 6.0F;
    private static final float BASE_HEIGHT = 4.0F;

    public AuroraVeilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AuroraVeilEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        int life = entity.getLifetime();
        float size = entity.getSize();
        int seed = entity.getSeed();

        float alpha = Mth.clamp(age / 3.0F, 0.0F, 1.0F)
                * Mth.clamp((life - age) / 6.0F, 0.0F, 1.0F) * 0.55F;
        if (alpha <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        Matrix4f mat = poseStack.last().pose();

        float w = BASE_WIDTH * size;
        float h = BASE_HEIGHT * size;

        // 加色辉光（自发光质感）——单层即可，就是最初那种亮的观感。
        buildVeil(buffer.getBuffer(RenderType.lightning()), mat, w, h, size, age, seed, alpha);

        poseStack.popPose();
    }

    private static void buildVeil(VertexConsumer vc, Matrix4f mat, float w, float h, float size,
                                  float age, int seed, float alpha) {
        for (int c = 0; c < COLS; c++) {
            float x0 = -w / 2.0F + w * c / COLS;
            float x1 = -w / 2.0F + w * (c + 1) / COLS;
            float z0 = ripple(x0, age, seed);
            float z1 = ripple(x1, age, seed);
            float top0 = h + (float) (0.4 * Math.sin(x0 * 0.7 + age * 0.2 + seed)) * size;
            float top1 = h + (float) (0.4 * Math.sin(x1 * 0.7 + age * 0.2 + seed)) * size;

            float[] cl = hue(c, age, seed);
            float[] cr = hue(c + 1, age, seed);
            float aBottom = alpha * 0.12F;

            // 正面 + 背面（双面，规避剔除）
            quad(vc, mat,
                    x0, 0, z0, cl, aBottom,
                    x1, 0, z1, cr, aBottom,
                    x1, top1, z1, cr, alpha,
                    x0, top0, z0, cl, alpha);
            quad(vc, mat,
                    x0, top0, z0, cl, alpha,
                    x1, top1, z1, cr, alpha,
                    x1, 0, z1, cr, aBottom,
                    x0, 0, z0, cl, aBottom);
        }
    }

    private static float ripple(float x, float age, int seed) {
        return (float) (0.6 * Math.sin(x * 0.6 + age * 0.25 + seed));
    }

    /** 沿帷幕宽度 + 随时间流转的极光色（青 / 绿 / 蓝紫），红色分量压低。 */
    private static float[] hue(int col, float age, int seed) {
        float p = col * 0.4F + age * 0.12F + seed;
        float g = Mth.clamp(0.55F + 0.45F * Mth.sin(p), 0.0F, 1.0F);
        float b = Mth.clamp(0.65F + 0.35F * Mth.sin(p + 2.0F), 0.0F, 1.0F);
        return new float[]{0.12F, g, b};
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float[] c0, float a0,
                             float x1, float y1, float z1, float[] c1, float a1,
                             float x2, float y2, float z2, float[] c2, float a2,
                             float x3, float y3, float z3, float[] c3, float a3) {
        vc.vertex(mat, x0, y0, z0).color(c0[0], c0[1], c0[2], a0).endVertex();
        vc.vertex(mat, x1, y1, z1).color(c1[0], c1[1], c1[2], a1).endVertex();
        vc.vertex(mat, x2, y2, z2).color(c2[0], c2[1], c2[2], a2).endVertex();
        vc.vertex(mat, x3, y3, z3).color(c3[0], c3[1], c3[2], a3).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(AuroraVeilEntity entity) {
        return TEX;
    }
}

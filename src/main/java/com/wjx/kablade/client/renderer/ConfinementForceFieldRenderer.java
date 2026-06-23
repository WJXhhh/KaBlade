package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.ConfinementForceFieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 「领域压杀 / 高频坍缩」共用的源能禁锢领域渲染。
 * 用程序化几何画出青白色的上下环、纵向束缚弧片与内收刀痕，保留原有色调与环形压制感。
 */
public class ConfinementForceFieldRenderer extends EntityRenderer<ConfinementForceFieldEntity> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int SEGMENTS = 48;
    private static final int SPOKES = 10;

    public ConfinementForceFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ConfinementForceFieldEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float life = Math.max(1.0F, entity.getLifetimeTicks());
        float t = Mth.clamp(age / life, 0.0F, 1.0F);
        float enterT = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float exitT = Mth.clamp((life - age) / 10.0F, 0.0F, 1.0F);
        float appear = bezier(enterT, 0.20F, 0.96F);
        float vanish = bezier(exitT, 0.10F, 0.88F);
        float alpha = appear * vanish;
        if (alpha <= 0.01F) {
            return;
        }

        float growth = Mth.lerp(appear, 0.76F, 1.0F);
        float collapse = Mth.lerp(1.0F - vanish, 1.0F, 0.90F);
        float scale = growth * collapse;
        float spinProgress = bezier(t, 0.28F, 0.82F);
        float pulse = (0.84F + 0.16F * Mth.sin(age * 0.22F)) * (0.92F + 0.08F * appear);
        float radius = (float) entity.getFieldRadius() * scale * (0.96F + 0.06F * Mth.sin(age * 0.14F));
        float height = 3.6F * scale + 0.45F * Mth.sin(age * 0.17F) * (0.65F + 0.35F * appear);
        float innerRadius = radius * 0.64F;
        float topY = height;
        float bottomY = 0.12F * (0.65F + 0.35F * appear);
        float worldRot = Mth.lerp(spinProgress, 0.0F, 42.0F) + age * (0.45F + 1.35F * appear * vanish);

        poseStack.pushPose();
        poseStack.translate(0.0F, (1.0F - appear) * -0.18F + (1.0F - vanish) * 0.10F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(worldRot));
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        renderRing(vc, mat, radius, bottomY, 0.40F, age * 5.5F, 0.00F, 1.00F, 1.00F, alpha * 0.32F * pulse);
        renderRing(vc, mat, radius * 0.94F, bottomY + 0.18F, 0.18F, -age * 7.0F, 0.38F, 1.00F, 1.00F, alpha * 0.74F);
        renderRing(vc, mat, radius * 0.98F, topY, 0.24F, age * 8.5F, 0.18F, 0.95F, 1.00F, alpha * 0.60F);
        renderRing(vc, mat, innerRadius, 1.24F + 0.18F * Mth.sin(age * 0.32F), 0.12F,
                -age * 11.5F, 0.78F, 1.00F, 1.00F, alpha * 0.62F);

        renderCurtains(vc, mat, radius * 0.96F, bottomY, topY, age, alpha);
        renderSpokes(vc, mat, innerRadius, radius * 0.92F, 1.15F, age, alpha);
        renderCentralSeal(vc, mat, 1.18F + 0.12F * Mth.sin(age * 0.28F), age, alpha);

        poseStack.popPose();
    }

    private static float bezier(float t, float c1, float c2) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float it = 1.0F - t;
        return 3.0F * it * it * t * c1
                + 3.0F * it * t * t * c2
                + t * t * t;
    }

    private static void renderRing(VertexConsumer vc, Matrix4f mat, float radius, float y, float thickness,
                                   float spinDeg, float r, float g, float b, float alpha) {
        float spin = (float) Math.toRadians(spinDeg);
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = i / (float) SEGMENTS;
            float f1 = (i + 1) / (float) SEGMENTS;
            float a0 = spin + f0 * Mth.TWO_PI;
            float a1 = spin + f1 * Mth.TWO_PI;
            float wave0 = 0.82F + 0.18F * Mth.sin(f0 * Mth.TWO_PI * 6.0F + spin * 0.7F);
            float wave1 = 0.82F + 0.18F * Mth.sin(f1 * Mth.TWO_PI * 6.0F + spin * 0.7F);
            float o0 = radius + thickness * wave0;
            float i0 = radius - thickness * 0.65F * wave0;
            float o1 = radius + thickness * wave1;
            float i1 = radius - thickness * 0.65F * wave1;
            quad(vc, mat,
                    Mth.cos(a0) * o0, y, Mth.sin(a0) * o0, r, g, b, alpha,
                    Mth.cos(a1) * o1, y, Mth.sin(a1) * o1, r, g, b, alpha,
                    Mth.cos(a1) * i1, y, Mth.sin(a1) * i1, r, g, b, alpha * 0.26F,
                    Mth.cos(a0) * i0, y, Mth.sin(a0) * i0, r, g, b, alpha * 0.26F);
            quad(vc, mat,
                    Mth.cos(a0) * i0, y, Mth.sin(a0) * i0, r, g, b, alpha * 0.26F,
                    Mth.cos(a1) * i1, y, Mth.sin(a1) * i1, r, g, b, alpha * 0.26F,
                    Mth.cos(a1) * o1, y, Mth.sin(a1) * o1, r, g, b, alpha,
                    Mth.cos(a0) * o0, y, Mth.sin(a0) * o0, r, g, b, alpha);
        }
    }

    private static void renderCurtains(VertexConsumer vc, Matrix4f mat, float radius, float bottomY,
                                       float topY, float age, float alpha) {
        for (int i = 0; i < 8; i++) {
            float base = i / 8.0F * Mth.TWO_PI + age * 0.035F;
            for (int j = 0; j < 2; j++) {
                float width = 0.18F + 0.04F * j;
                float a0 = base - width;
                float a1 = base + width;
                float sink = 0.42F + 0.20F * Mth.sin(age * 0.16F + i * 0.9F + j);
                float midY = Mth.lerp(0.58F, bottomY, topY);
                quad(vc, mat,
                        Mth.cos(a0) * radius, bottomY, Mth.sin(a0) * radius, 0.55F, 1.0F, 1.0F, alpha * 0.28F,
                        Mth.cos(base) * (radius - sink), midY, Mth.sin(base) * (radius - sink), 0.80F, 1.0F, 1.0F, alpha * 0.10F,
                        Mth.cos(a1) * radius, topY, Mth.sin(a1) * radius, 0.70F, 1.0F, 1.0F, alpha * 0.56F,
                        Mth.cos(base) * (radius + 0.08F), midY + 0.3F, Mth.sin(base) * (radius + 0.08F), 0.20F, 0.95F, 1.0F, 0.0F);
            }
        }
    }

    private static void renderSpokes(VertexConsumer vc, Matrix4f mat, float innerRadius, float outerRadius,
                                     float y, float age, float alpha) {
        for (int i = 0; i < SPOKES; i++) {
            float angle = i / (float) SPOKES * Mth.TWO_PI - age * 0.08F;
            float next = angle + 0.10F;
            float twist = 0.24F * Mth.sin(age * 0.28F + i * 0.7F);
            quad(vc, mat,
                    Mth.cos(angle) * innerRadius, y - 0.08F, Mth.sin(angle) * innerRadius, 1.0F, 1.0F, 1.0F, alpha * 0.44F,
                    Mth.cos(angle) * outerRadius, y + twist, Mth.sin(angle) * outerRadius, 0.40F, 0.95F, 1.0F, alpha * 0.08F,
                    Mth.cos(next) * outerRadius, y + twist, Mth.sin(next) * outerRadius, 0.40F, 0.95F, 1.0F, alpha * 0.08F,
                    Mth.cos(next) * innerRadius, y + 0.08F, Mth.sin(next) * innerRadius, 1.0F, 1.0F, 1.0F, alpha * 0.36F);
        }
    }

    private static void renderCentralSeal(VertexConsumer vc, Matrix4f mat, float size, float age, float alpha) {
        for (int i = 0; i < 3; i++) {
            float rot = age * (8.0F - i * 2.0F) + i * 60.0F;
            renderCrossBlade(vc, mat, size * (1.0F - i * 0.18F), 1.4F + i * 0.28F,
                    rot, 0.90F - i * 0.15F, 1.0F, 1.0F, alpha * (0.36F - i * 0.08F));
        }
    }

    private static void renderCrossBlade(VertexConsumer vc, Matrix4f mat, float size, float y,
                                         float rotDeg, float r, float g, float b, float alpha) {
        for (int arm = 0; arm < 4; arm++) {
            float a0 = (float) Math.toRadians(rotDeg + arm * 90.0F - 8.0F);
            float a1 = (float) Math.toRadians(rotDeg + arm * 90.0F + 8.0F);
            float inner = size * 0.16F;
            float outer = size;
            quad(vc, mat,
                    Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, r, g, b, alpha * 0.22F,
                    Mth.cos(a1) * inner, y, Mth.sin(a1) * inner, r, g, b, alpha * 0.22F,
                    Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, r, g, b, alpha,
                    Mth.cos(a0) * outer, y, Mth.sin(a0) * outer, r, g, b, alpha);
            quad(vc, mat,
                    Mth.cos(a0) * outer, y, Mth.sin(a0) * outer, r, g, b, alpha,
                    Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, r, g, b, alpha,
                    Mth.cos(a1) * inner, y, Mth.sin(a1) * inner, r, g, b, alpha * 0.22F,
                    Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, r, g, b, alpha * 0.22F);
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
    public ResourceLocation getTextureLocation(ConfinementForceFieldEntity entity) {
        return TEX;
    }
}

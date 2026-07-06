package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusSkillRenderer;
import com.wjx.kablade.entity.CrimsonSakuraAttackEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CrimsonSakuraRenderer extends EntityRenderer<CrimsonSakuraAttackEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/crimson_sakura/crimson_sakura_edge.png");
    private static final int SEGMENTS = 60;
    private static final float OUTER_RADIUS = 2.25F;
    private static final float MIN_BAND_WIDTH = 0.05F;
    private static final float POST_OUTER_GROWTH = 0.04F;
    private static final float POST_INNER_GROWTH = 0.08F;
    private static final float POST_SOFT_OUTER_GROWTH = 0.12F;
    private static final float POST_SOFT_INNER_GROWTH = 0.20F;
    private static final float POST_CORE_ALPHA = 0.96F;
    private static final float POST_SOFT_ALPHA = 0.58F;
    private static final float SHADER_BRIGHT_ALPHA = 1.70F;
    private static final float BLOOM_INNER_GROWTH = 0.03F;
    private static final float BLOOM_OUTER_GROWTH = 0.18F;
    private static final float BLOOM_ALPHA = 0.58F;
    private static final Vec[] POINTS = buildPoints();

    public CrimsonSakuraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(CrimsonSakuraAttackEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float alpha = entity.getAlpha(partialTick);
        if (alpha <= 0.01F) {
            return;
        }

        int visible = Mth.clamp(entity.getVisibleSegments(partialTick), 1, SEGMENTS);
        float inner = Math.max(entity.getInnerRadius(partialTick), OUTER_RADIUS + MIN_BAND_WIDTH);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.75D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + entity.getAngle(partialTick)));

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f pose = lastPose.pose();
        Matrix3f normal = lastPose.normal();
        int start = SEGMENTS - visible;

        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            if (OculusSkillRenderer.runPostIfNeeded(immediate ->
                    renderPostPass(immediate, pose, normal, start, inner, alpha))) {
                poseStack.popPose();
                super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
                return;
            }

            VertexConsumer bright = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
            float brightAlpha = Mth.clamp(alpha * SHADER_BRIGHT_ALPHA, 0.0F, 1.0F);
            for (int i = SEGMENTS - 1; i >= start; i--) {
                drawSegment(bright, pose, normal, i, OUTER_RADIUS, inner, brightAlpha, 1.0F, 0.70F, 0.76F);
            }

            VertexConsumer bloom = buffer.getBuffer(KabladeRenderTypes.rainPetalBloom(TEXTURE));
            float bloomAlpha = Mth.clamp(alpha * BLOOM_ALPHA, 0.0F, 0.90F);
            float bloomOuter = OUTER_RADIUS + BLOOM_OUTER_GROWTH;
            float bloomInner = Math.max(inner + BLOOM_INNER_GROWTH, bloomOuter + MIN_BAND_WIDTH);
            for (int i = SEGMENTS - 1; i >= start; i--) {
                drawSegment(bloom, pose, normal, i, bloomOuter, bloomInner,
                        bloomAlpha, 1.0F, 0.28F, 0.36F);
            }
        } else {
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            for (int i = SEGMENTS - 1; i >= start; i--) {
                drawSegment(vc, pose, normal, i, OUTER_RADIUS, inner, alpha, 1.0F, 1.0F, 1.0F);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderPostPass(MultiBufferSource buffer, Matrix4f pose, Matrix3f normal,
                                       int start, float inner, float alpha) {
        VertexConsumer soft = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        float softOuter = OUTER_RADIUS + POST_SOFT_OUTER_GROWTH;
        float softInner = Math.max(inner + POST_SOFT_INNER_GROWTH, softOuter + MIN_BAND_WIDTH);
        float softAlpha = Mth.clamp(alpha * POST_SOFT_ALPHA, 0.0F, 1.0F);
        for (int i = SEGMENTS - 1; i >= start; i--) {
            drawSegment(soft, pose, normal, i, softOuter, softInner, softAlpha, 1.0F, 0.22F, 0.30F);
        }

        VertexConsumer core = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        float coreOuter = OUTER_RADIUS + POST_OUTER_GROWTH;
        float coreInner = Math.max(inner + POST_INNER_GROWTH, coreOuter + MIN_BAND_WIDTH);
        float coreAlpha = Mth.clamp(alpha * POST_CORE_ALPHA, 0.0F, 1.0F);
        for (int i = SEGMENTS - 1; i >= start; i--) {
            drawSegment(core, pose, normal, i, coreOuter, coreInner, coreAlpha, 1.0F, 0.78F, 0.84F);
        }
    }

    private static void drawSegment(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                    int i, float outerRadius, float innerRadius,
                                    float alpha, float red, float green, float blue) {
        Vec p0 = POINTS[i];
        Vec p1 = POINTS[i + 1];
        float u0 = i / (float) (SEGMENTS - 1);
        float u1 = (i + 1) / (float) (SEGMENTS - 1);
        float y0 = rise(i);
        float y1 = rise(i + 1);

        vertex(vc, pose, normal, p0.x * outerRadius, y0, p0.z * outerRadius, u0, 0.0F, alpha, red, green, blue);
        vertex(vc, pose, normal, p1.x * outerRadius, y1, p1.z * outerRadius, u1, 0.0F, alpha, red, green, blue);
        vertex(vc, pose, normal, p1.x * innerRadius, y1, p1.z * innerRadius, u1, 1.0F, alpha, red, green, blue);
        vertex(vc, pose, normal, p0.x * innerRadius, y0, p0.z * innerRadius, u0, 1.0F, alpha, red, green, blue);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float alpha, float red, float green, float blue) {
        vc.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static float rise(int idx) {
        return idx > 32 ? 2.0F / 26.0F * (idx - 32) : 0.0F;
    }

    private static Vec[] buildPoints() {
        Vec[] points = new Vec[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.toRadians(720.0D - i * 12.0D);
            points[i] = new Vec((float) Math.cos(angle), (float) Math.sin(angle));
        }
        return points;
    }

    @Override
    public ResourceLocation getTextureLocation(CrimsonSakuraAttackEntity entity) {
        return TEXTURE;
    }

    private record Vec(float x, float z) {
    }
}

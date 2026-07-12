package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusSkillRenderer;
import com.wjx.kablade.entity.ThunderEdgeAttackEntity;
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

import java.util.ArrayList;
import java.util.List;

/** Rebuilds the 1.12.2 Thunder Edge crescent and lightning flashes for 1.20 rendering. */
public final class ThunderEdgeAttackRenderer extends EntityRenderer<ThunderEdgeAttackEntity> {

    private static final ResourceLocation EDGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID,
                    "textures/entity/thunder_edge_attack/thunder_edge_attack_n.png");
    private static final List<ResourceLocation> LIGHT_TEXTURES = new ArrayList<>();
    private static final int SEGMENTS = 60;
    private static final float START_INNER_RADIUS = 2.35F;
    private static final float PEAK_INNER_RADIUS = 3.35F;
    private static final float END_INNER_RADIUS = 3.05F;
    private static final float START_WIDTH = 0.48F;
    private static final float PEAK_WIDTH = 0.78F;
    private static final float END_WIDTH = 0.62F;
    private static final float LIGHT_SPREAD = 1.28F;
    private static final float POST_CORE_ALPHA = 0.92F;
    private static final float POST_SOFT_ALPHA = 0.56F;
    private static final float POST_INNER_GROWTH = 0.04F;
    private static final float POST_OUTER_GROWTH = 0.08F;
    private static final float POST_SOFT_INNER_GROWTH = 0.10F;
    private static final float POST_SOFT_OUTER_GROWTH = 0.18F;
    private static final float POST_FLASH_ALPHA = 0.90F;
    private static final float POST_FLASH_SCALE = 1.85F;
    private static final float SHADER_BRIGHT_ALPHA = 1.55F;
    private static final float BLOOM_ALPHA = 0.30F;
    private static final float BLOOM_INNER_GROWTH = 0.02F;
    private static final float BLOOM_OUTER_GROWTH = 0.14F;

    static {
        for (int i = 0; i <= 19; i++) {
            LIGHT_TEXTURES.add(ResourceLocation.fromNamespaceAndPath(Main.MODID,
                    "textures/entity/thunder_edge_attack/thunder_edge_light_" + i + ".png"));
        }
    }

    public ThunderEdgeAttackRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ThunderEdgeAttackEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float life = Math.max(1.0F, entity.getLifetime());
        float progress = Mth.clamp(age / life, 0.0F, 1.0F);
        float open = smootherStep(Mth.clamp(age / 7.0F, 0.0F, 1.0F));
        // Leave enough time for the crescent and flashes to dissolve instead of
        // disappearing on the same tick that the server removes the anchor.
        float fade = 1.0F - smootherStep(Mth.clamp((progress - 0.52F) / 0.48F, 0.0F, 1.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        if (KabladeRenderTypes.useShaderFallbackTextures()
                && OculusSkillRenderer.runPostIfNeeded(immediate ->
                renderPostPass(entity, age, partialTick, poseStack, immediate, alpha, open, progress))) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.78F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + Mth.lerp(progress, 15.0F, -15.0F)));
        poseStack.scale(1.08F, 1.82F, 1.08F);
        PoseStack.Pose pose = poseStack.last();
        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            renderCrescent(pose.pose(), pose.normal(),
                    buffer.getBuffer(RenderType.entityTranslucentEmissive(EDGE_TEXTURE)),
                    Mth.clamp(alpha * SHADER_BRIGHT_ALPHA, 0.0F, 1.0F),
                    open, progress, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            renderCrescent(pose.pose(), pose.normal(),
                    buffer.getBuffer(KabladeRenderTypes.rainPetalBloom(EDGE_TEXTURE)),
                    Mth.clamp(alpha * BLOOM_ALPHA, 0.0F, 0.85F),
                    open, progress, BLOOM_INNER_GROWTH, BLOOM_OUTER_GROWTH, 1.0F, 0.74F, 1.0F);
        } else {
            renderCrescent(pose.pose(), pose.normal(),
                    buffer.getBuffer(RenderType.entityTranslucentEmissive(EDGE_TEXTURE)),
                    alpha, open, progress, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
        poseStack.popPose();

        renderFlashes(entity, age, partialTick, poseStack, buffer, alpha, false);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderPostPass(ThunderEdgeAttackEntity entity, float age, float partialTick,
                                PoseStack poseStack, MultiBufferSource buffer,
                                float alpha, float open, float progress) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.78F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + Mth.lerp(progress, 15.0F, -15.0F)));
        poseStack.scale(1.08F, 1.82F, 1.08F);
        PoseStack.Pose pose = poseStack.last();

        renderCrescent(pose.pose(), pose.normal(),
                buffer.getBuffer(RenderType.entityTranslucentEmissive(EDGE_TEXTURE)),
                Mth.clamp(alpha * POST_SOFT_ALPHA, 0.0F, 1.0F),
                open, progress, POST_SOFT_INNER_GROWTH, POST_SOFT_OUTER_GROWTH, 0.55F, 0.86F, 1.0F);
        renderCrescent(pose.pose(), pose.normal(),
                buffer.getBuffer(RenderType.entityTranslucentEmissive(EDGE_TEXTURE)),
                Mth.clamp(alpha * POST_CORE_ALPHA, 0.0F, 1.0F),
                open, progress, POST_INNER_GROWTH, POST_OUTER_GROWTH, 0.92F, 0.98F, 1.0F);
        poseStack.popPose();

        renderFlashes(entity, age, partialTick, poseStack, buffer, alpha, true);
    }

    private static void renderCrescent(Matrix4f mat, Matrix3f normal, VertexConsumer vc,
                                       float alpha, float open, float progress,
                                       float innerGrowth, float outerGrowth,
                                       float red, float green, float blue) {
        int visible = Math.max(2, Math.min(SEGMENTS, Mth.ceil(SEGMENTS * open)));
        int first = SEGMENTS - visible;
        float inner = Mth.lerp(open, START_INNER_RADIUS, PEAK_INNER_RADIUS);
        float width = Mth.lerp(open, START_WIDTH, PEAK_WIDTH);
        float shrink = smootherStep(Mth.clamp((progress - 0.55F) / 0.45F, 0.0F, 1.0F));
        inner = Mth.lerp(shrink, inner, END_INNER_RADIUS);
        width = Mth.lerp(shrink, width, END_WIDTH);
        float outer = inner + width + outerGrowth;
        inner = Math.max(0.1F, inner + innerGrowth);
        float widthAlpha = Mth.clamp(width / PEAK_WIDTH, 0.25F, 1.0F);

        for (int i = first; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            float a0 = Mth.PI - Mth.PI * t0;
            float a1 = Mth.PI - Mth.PI * t1;
            float reveal0 = revealAlpha(t0, open);
            float reveal1 = revealAlpha(t1, open);
            float rim0 = alpha * trailAlpha(t0) * reveal0 * widthAlpha;
            float rim1 = alpha * trailAlpha(t1) * reveal1 * widthAlpha;

            vertex(vc, mat, normal, Mth.cos(a0) * outer, 0.0F, Mth.sin(a0) * outer,
                    t0, 1.0F, rim0, red, green, blue);
            vertex(vc, mat, normal, Mth.cos(a1) * outer, 0.0F, Mth.sin(a1) * outer,
                    t1, 1.0F, rim1, red, green, blue);
            vertex(vc, mat, normal, Mth.cos(a1) * inner, 0.0F, Mth.sin(a1) * inner,
                    t1, 0.0F, rim1 * 0.72F, red, green, blue);
            vertex(vc, mat, normal, Mth.cos(a0) * inner, 0.0F, Mth.sin(a0) * inner,
                    t0, 0.0F, rim0 * 0.72F, red, green, blue);
        }
    }

    private void renderFlashes(ThunderEdgeAttackEntity entity, float age, float partialTick,
                               PoseStack poseStack, MultiBufferSource buffer, float alpha,
                               boolean postPass) {
        float[][] offsets = {
                {2.17F, 1.25F, 5.0F},
                {1.25F, 2.17F, 8.0F},
                {0.0F, 2.5F, 11.0F},
                {-1.25F, 2.17F, 14.0F},
                {-2.17F, 1.25F, 17.0F}
        };

        for (float[] offset : offsets) {
            int frame = Mth.floor(age - offset[2]);
            if (frame < 0 || frame >= LIGHT_TEXTURES.size()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.25F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            poseStack.translate(offset[0] * LIGHT_SPREAD, 0.0F, offset[1] * LIGHT_SPREAD);
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            PoseStack.Pose pose = poseStack.last();
            float flashAlpha = alpha * (1.0F - frame / 24.0F);
            ResourceLocation texture = LIGHT_TEXTURES.get(frame);
            renderBillboard(pose.pose(), pose.normal(),
                    buffer.getBuffer(RenderType.entityTranslucentEmissive(texture)),
                    Mth.clamp(flashAlpha * (postPass ? POST_FLASH_ALPHA : 1.35F), 0.0F, 1.0F),
                    postPass ? POST_FLASH_SCALE : 1.55F, 1.0F, 1.0F, 1.0F);
            if (!postPass && KabladeRenderTypes.useShaderFallbackTextures()) {
                renderBillboard(pose.pose(), pose.normal(),
                        buffer.getBuffer(KabladeRenderTypes.rainPetalBloom(texture)),
                        Mth.clamp(flashAlpha * 0.62F, 0.0F, 0.8F), 1.90F, 0.38F, 0.92F, 1.0F);
            }
            poseStack.popPose();
        }
    }

    private static void renderBillboard(Matrix4f mat, Matrix3f normal, VertexConsumer vc,
                                        float alpha, float scale,
                                        float red, float green, float blue) {
        float w = 0.66F * scale;
        float y0 = -0.32F * scale;
        float y1 = 0.96F * scale;
        vertex(vc, mat, normal, -w, y0, 0.0F, 0.0F, 1.0F, alpha, red, green, blue);
        vertex(vc, mat, normal, w, y0, 0.0F, 1.0F, 1.0F, alpha, red, green, blue);
        vertex(vc, mat, normal, w, y1, 0.0F, 1.0F, 0.0F, alpha, red, green, blue);
        vertex(vc, mat, normal, -w, y1, 0.0F, 0.0F, 0.0F, alpha, red, green, blue);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float alpha, float red, float green, float blue) {
        vc.vertex(mat, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float trailAlpha(float t) {
        float head = smootherStep(Mth.clamp(t / 0.12F, 0.0F, 1.0F));
        float tail = 1.0F - smootherStep(Mth.clamp((t - 0.88F) / 0.12F, 0.0F, 1.0F));
        return head * tail;
    }

    private static float revealAlpha(float t, float open) {
        float start = 1.0F - open;
        return smootherStep(Mth.clamp((t - start) / 0.12F, 0.0F, 1.0F));
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderEdgeAttackEntity entity) {
        return EDGE_TEXTURE;
    }
}

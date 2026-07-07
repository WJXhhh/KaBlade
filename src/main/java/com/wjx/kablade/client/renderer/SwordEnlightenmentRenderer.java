package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.shader.OculusSkillRenderer;
import com.wjx.kablade.entity.SwordEnlightenmentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Procedural purple-white slash lattice for Sword Enlightenment. */
public final class SwordEnlightenmentRenderer extends EntityRenderer<SwordEnlightenmentEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final int RING_SEGMENTS = 92;
    private static final int LOOP_SEGMENTS = 58;
    private static final int SWEEP_SEGMENTS = 54;
    private static final int SHARD_COUNT = 58;

    public SwordEnlightenmentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SwordEnlightenmentEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (KabladeRenderTypes.useShaderFallbackTextures()
                && !OculusSkillRenderer.isRenderingPass()
                && OculusSkillRenderer.runPostIfNeeded(immediate ->
                render(entity, entityYaw, partialTick, poseStack, immediate, packedLight))) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        float age = entity.tickCount + partialTick;
        if (age >= entity.getLifetime() + 2.0F) {
            return;
        }

        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.swordEnlightenment());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        Matrix4f mat = poseStack.last().pose();

        renderGroundRings(age, mat, vc);
        renderHaloShell(age, mat, vc);
        renderSlashTimeline(age, mat, vc);
        renderPurpleArcSlashes(age, mat, vc);
        renderDiagonalBlades(age, mat, vc);
        renderFragments(age, mat, vc);
        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            renderOculusFallbackFilaments(age, mat, vc);
        }

        poseStack.popPose();
        renderBillboards(entity, age, poseStack, vc);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderGroundRings(float age, Matrix4f mat, VertexConsumer vc) {
        float open = smootherStep(stage(age, 5.0F, 10.0F));
        float fade = 1.0F - smootherStep(stage(age, 38.0F, 16.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.38F);
        drawBrokenRing(vc, mat, 0.055F, Mth.lerp(open, 0.64F, 4.95F),
                Mth.lerp(open, 0.08F, 0.42F), 0.20F + open * 0.52F,
                0.76F, 0.28F, 1.0F, alpha * 0.44F);
        drawRing(vc, mat, 0.075F, 1.35F + open * 5.05F + pulse * 0.10F,
                0.10F + open * 0.32F, 0.94F, 0.82F, 1.0F, alpha * 0.28F);

        for (int i = 0; i < 5; i++) {
            float start = 6.0F + i * 3.2F;
            float t = stage(age, start, 18.0F);
            if (t >= 1.0F) {
                continue;
            }
            float grow = fastOut(t);
            float local = (1.0F - smootherStep(stage(age, start + 8.0F, 10.0F))) * fade;
            float radius = 0.55F + grow * (4.9F + i * 0.55F);
            drawBrokenRing(vc, mat, 0.10F + i * 0.012F, radius,
                    0.12F + grow * (0.34F + i * 0.03F), grow,
                    i % 2 == 0 ? 0.92F : 0.56F,
                    i % 2 == 0 ? 0.72F : 0.24F,
                    1.0F,
                    local * (0.26F - i * 0.025F));
        }
    }

    private static void renderHaloShell(float age, Matrix4f mat, VertexConsumer vc) {
        float appear = smootherStep(stage(age, 7.0F, 3.0F));
        float fade = 1.0F - smootherStep(stage(age, 36.0F, 10.0F));
        float alpha = appear * fade;
        if (alpha <= 0.01F) {
            return;
        }

        for (int i = 0; i < 11; i++) {
            float seed = i * 0.713F;
            float localOpen = smootherStep(stage(age, 7.4F + i * 0.55F, 8.0F));
            float localFade = 1.0F - smootherStep(stage(age, 25.0F + i * 0.75F, 12.0F));
            float local = alpha * localOpen * localFade;
            if (local <= 0.01F) {
                continue;
            }

            float rx = 2.05F + i * 0.22F + localOpen * 1.25F;
            float rz = 1.55F + i * 0.18F + localOpen * 0.88F;
            float y = 0.82F + (i % 4) * 0.20F;
            float lift = 0.48F + localOpen * (1.05F + (i % 3) * 0.20F);
            drawLiftedLoop(vc, mat,
                    age * (0.11F + i * 0.008F) + seed,
                    (0.58F + localOpen * 0.66F) * Mth.TWO_PI,
                    rx, rz, y, lift, 0.045F + i * 0.006F,
                    i % 3 == 0 ? 0.98F : 0.58F,
                    i % 3 == 0 ? 0.86F : 0.28F,
                    1.0F,
                    local * (0.20F + (i % 3) * 0.035F));
        }
    }

    private static void renderSlashTimeline(float age, Matrix4f mat, VertexConsumer vc) {
        drawCrescentSweep(vc, mat, age, 6.4F, 5.6F, 4.65F,
                116.0F, -58.0F, 0.18F, 2.12F, 0.15F, 0.70F,
                0.94F, 0.78F, 1.0F, 1.00F);
        drawCrescentSweep(vc, mat, age, 9.0F, 6.8F, 5.15F,
                158.0F, -26.0F, 0.42F, 2.62F, 0.45F, 0.48F,
                0.68F, 0.34F, 1.0F, 0.84F);
        drawCrescentSweep(vc, mat, age, 13.2F, 8.0F, 5.90F,
                210.0F, 18.0F, 0.22F, 2.92F, 0.82F, 0.40F,
                0.92F, 0.82F, 1.0F, 0.74F);
        drawCrescentSweep(vc, mat, age, 17.2F, 9.0F, 6.45F,
                255.0F, 52.0F, 0.18F, 3.34F, 1.20F, 0.38F,
                0.58F, 0.30F, 1.0F, 0.72F);
        drawCrescentSweep(vc, mat, age, 21.6F, 10.0F, 6.85F,
                318.0F, 112.0F, 0.34F, 3.76F, 1.62F, 0.30F,
                0.96F, 0.86F, 1.0F, 0.55F);
    }

    private static void renderPurpleArcSlashes(float age, Matrix4f mat, VertexConsumer vc) {
        drawVerticalArcSlash(vc, mat, age, 8.2F, 4.8F,
                -0.25F, 1.62F, 2.45F, 5.45F, 2.05F, 0.72F,
                206.0F, 344.0F, 0.62F,
                0.66F, 0.24F, 1.0F, 0.88F);
        drawVerticalArcSlash(vc, mat, age, 10.6F, 5.8F,
                0.32F, 1.78F, 2.75F, 4.85F, 2.55F, -0.56F,
                -24.0F, 174.0F, 0.46F,
                0.82F, 0.42F, 1.0F, 0.68F);
        drawVerticalArcSlash(vc, mat, age, 14.4F, 6.5F,
                -0.46F, 1.96F, 3.00F, 5.90F, 2.90F, 0.48F,
                222.0F, 396.0F, 0.40F,
                0.58F, 0.20F, 1.0F, 0.70F);
        drawVerticalArcSlash(vc, mat, age, 17.8F, 6.8F,
                0.50F, 2.06F, 3.20F, 5.15F, 2.70F, -0.66F,
                -40.0F, 152.0F, 0.36F,
                0.74F, 0.34F, 1.0F, 0.62F);
        drawVerticalArcSlash(vc, mat, age, 21.5F, 7.2F,
                -0.18F, 2.22F, 3.45F, 6.35F, 3.10F, 0.82F,
                198.0F, 358.0F, 0.34F,
                0.92F, 0.62F, 1.0F, 0.54F);
        drawVerticalArcSlash(vc, mat, age, 27.0F, 7.0F,
                0.20F, 2.26F, 3.18F, 5.45F, 2.80F, -0.44F,
                18.0F, 188.0F, 0.28F,
                0.62F, 0.28F, 1.0F, 0.42F);
    }

    private static void renderDiagonalBlades(float age, Matrix4f mat, VertexConsumer vc) {
        drawTimedBlade(vc, mat, age, 14.0F, 9.0F,
                -5.6F, 0.36F, 0.85F, 4.6F, 4.20F, 5.15F,
                0.13F, 0.90F, 0.82F, 1.0F, 0.78F);
        drawTimedBlade(vc, mat, age, 16.2F, 9.5F,
                4.8F, 0.32F, 0.20F, -4.4F, 4.85F, 5.45F,
                0.12F, 0.62F, 0.36F, 1.0F, 0.78F);
        drawTimedBlade(vc, mat, age, 18.4F, 10.0F,
                -5.0F, 0.70F, 5.15F, 5.2F, 4.55F, 1.35F,
                0.10F, 0.98F, 0.88F, 1.0F, 0.64F);
        drawTimedBlade(vc, mat, age, 20.4F, 12.0F,
                2.45F, 0.20F, 2.15F, 5.25F, 5.25F, 6.35F,
                0.11F, 0.72F, 0.42F, 1.0F, 0.62F);
        drawTimedBlade(vc, mat, age, 22.5F, 12.0F,
                -2.70F, 0.24F, 1.45F, -5.75F, 5.05F, 5.95F,
                0.10F, 0.88F, 0.62F, 1.0F, 0.54F);

        for (int i = 0; i < 9; i++) {
            float start = 18.0F + i * 1.15F;
            float side = -4.5F + i * 1.15F;
            drawTimedBlade(vc, mat, age, start, 8.0F,
                    side, 0.15F, 3.7F + (i % 2) * 0.9F,
                    side + 1.45F, 4.30F + (i % 3) * 0.35F, 6.8F,
                    0.055F + (i % 3) * 0.012F,
                    i % 2 == 0 ? 0.92F : 0.58F,
                    i % 2 == 0 ? 0.86F : 0.32F,
                    1.0F,
                    0.34F);
        }
    }

    private static void renderFragments(float age, Matrix4f mat, VertexConsumer vc) {
        float globalFade = 1.0F - smootherStep(stage(age, 48.0F, 9.0F));
        if (globalFade <= 0.01F) {
            return;
        }

        for (int i = 0; i < SHARD_COUNT; i++) {
            float start = 7.0F + deterministic(i, 2.2F) * 27.0F;
            float raw = stage(age, start, 20.0F + deterministic(i, 3.1F) * 6.0F);
            if (raw <= 0.0F || raw >= 1.0F) {
                continue;
            }

            float launch = fastOut(Mth.clamp(raw / 0.42F, 0.0F, 1.0F));
            float fade = (1.0F - smootherStep(raw)) * globalFade;
            float angle = deterministic(i, 4.0F) * Mth.TWO_PI + age * 0.018F;
            float radius = 0.72F + deterministic(i, 5.0F) * 4.70F + launch * (0.35F + deterministic(i, 6.0F) * 1.45F);
            float x = Mth.cos(angle) * radius;
            float z = 0.45F + Mth.sin(angle) * radius * 0.68F + deterministic(i, 7.0F) * 2.4F;
            float y = 0.35F + deterministic(i, 8.0F) * 2.75F + launch * (0.35F + deterministic(i, 9.0F) * 1.35F);
            float size = 0.045F + deterministic(i, 10.0F) * 0.11F;
            float alpha = fade * (0.26F + deterministic(i, 11.0F) * 0.44F);
            drawShard(vc, mat, x, y, z, size, age * 0.16F + i * 0.91F,
                    i % 3 == 0 ? 0.96F : 0.62F,
                    i % 3 == 0 ? 0.82F : 0.34F,
                    1.0F, alpha);
        }
    }

    private static void renderOculusFallbackFilaments(float age, Matrix4f mat, VertexConsumer vc) {
        drawOculusCurvedFilament(vc, mat, age, 8.0F, 8.0F,
                -0.52F, 1.12F, 2.35F, 4.70F, 2.20F,
                218.0F, 330.0F, 0.075F, 0.76F, 0.26F, 1.0F, 0.55F);
        drawOculusCurvedFilament(vc, mat, age, 11.0F, 9.5F,
                0.42F, 1.25F, 2.70F, 4.20F, 2.85F,
                -28.0F, 162.0F, 0.060F, 0.98F, 0.62F, 1.0F, 0.46F);
        drawOculusCurvedFilament(vc, mat, age, 15.5F, 10.0F,
                -0.36F, 1.38F, 3.10F, 5.20F, 2.70F,
                204.0F, 384.0F, 0.052F, 0.62F, 0.22F, 1.0F, 0.50F);
        drawOculusCurvedFilament(vc, mat, age, 19.5F, 11.0F,
                0.30F, 1.55F, 3.25F, 4.70F, 2.45F,
                -10.0F, 188.0F, 0.050F, 0.86F, 0.44F, 1.0F, 0.42F);
        drawOculusCurvedFilament(vc, mat, age, 24.0F, 12.0F,
                -0.15F, 1.65F, 3.15F, 5.65F, 2.85F,
                192.0F, 354.0F, 0.046F, 0.72F, 0.28F, 1.0F, 0.34F);

        for (int i = 0; i < 10; i++) {
            float start = 12.0F + i * 1.55F;
            float side = -1.85F + deterministic(i, 20.0F) * 3.70F;
            float z = 2.25F + deterministic(i, 21.0F) * 2.95F;
            float radius = 1.35F + deterministic(i, 22.0F) * 1.65F;
            drawOculusCurvedFilament(vc, mat, age, start, 8.0F,
                    side, 0.80F + deterministic(i, 23.0F) * 1.10F, z,
                    radius, 0.95F + deterministic(i, 24.0F) * 0.80F,
                    160.0F + deterministic(i, 25.0F) * 80.0F,
                    300.0F + deterministic(i, 26.0F) * 120.0F,
                    0.024F + deterministic(i, 27.0F) * 0.018F,
                    i % 2 == 0 ? 0.92F : 0.58F,
                    i % 2 == 0 ? 0.76F : 0.24F,
                    1.0F,
                    0.24F);
        }
    }

    private void renderBillboards(SwordEnlightenmentEntity entity, float age,
                                  PoseStack poseStack, VertexConsumer vc) {
        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            renderOculusFallbackStarbursts(entity, age, poseStack, vc);
            return;
        }

        drawPulseBillboard(entity, age, poseStack, vc, 7.4F, 4.2F,
                0.0F, 1.25F, 2.35F, 6.4F, 0.86F, 0.48F);
        drawPulseBillboard(entity, age, poseStack, vc, 11.5F, 6.0F,
                0.15F, 1.35F, 2.55F, 4.8F, 0.74F, -12.0F);
        drawPulseBillboard(entity, age, poseStack, vc, 17.0F, 7.0F,
                0.0F, 1.58F, 2.75F, 4.5F, 0.68F, 22.0F);
        drawPulseBillboard(entity, age, poseStack, vc, 23.0F, 7.5F,
                -0.10F, 1.72F, 3.05F, 4.1F, 0.54F, -28.0F);
        drawPulseBillboard(entity, age, poseStack, vc, 31.0F, 9.0F,
                0.0F, 1.80F, 3.30F, 5.1F, 0.62F, 10.0F);
    }

    private void renderOculusFallbackStarbursts(SwordEnlightenmentEntity entity, float age,
                                                PoseStack poseStack, VertexConsumer vc) {
        drawFallbackStarburst(entity, age, poseStack, vc, 7.4F, 4.2F,
                0.0F, 1.25F, 2.35F, 5.20F, 0.82F, 0.48F);
        drawFallbackStarburst(entity, age, poseStack, vc, 11.5F, 6.0F,
                0.15F, 1.35F, 2.55F, 4.10F, 0.70F, -12.0F);
        drawFallbackStarburst(entity, age, poseStack, vc, 17.0F, 7.0F,
                0.0F, 1.58F, 2.75F, 3.85F, 0.64F, 22.0F);
        drawFallbackStarburst(entity, age, poseStack, vc, 23.0F, 7.5F,
                -0.10F, 1.72F, 3.05F, 3.55F, 0.52F, -28.0F);
        drawFallbackStarburst(entity, age, poseStack, vc, 31.0F, 9.0F,
                0.0F, 1.80F, 3.30F, 4.25F, 0.58F, 10.0F);
    }

    private void drawPulseBillboard(SwordEnlightenmentEntity entity, float age, PoseStack poseStack,
                                    VertexConsumer vc, float center, float duration,
                                    float x, float y, float z, float scale,
                                    float alphaScale, float rotation) {
        float local = 1.0F - Math.abs(age - center) / duration;
        float pulse = smootherStep(Mth.clamp(local, 0.0F, 1.0F));
        if (pulse <= 0.01F) {
            return;
        }
        float flicker = 0.82F + 0.18F * Mth.sin(age * 1.7F + center);
        drawBillboardAt(entity, poseStack, vc, x, y, z,
                scale * (0.70F + pulse * 0.38F), rotation + age * 2.4F,
                0.96F, 0.78F, 1.0F, pulse * alphaScale * flicker);
    }

    private void drawFallbackStarburst(SwordEnlightenmentEntity entity, float age, PoseStack poseStack,
                                       VertexConsumer vc, float center, float duration,
                                       float x, float y, float z, float scale,
                                       float alphaScale, float rotation) {
        float local = 1.0F - Math.abs(age - center) / duration;
        float pulse = smootherStep(Mth.clamp(local, 0.0F, 1.0F));
        if (pulse <= 0.01F) {
            return;
        }

        float flicker = 0.82F + 0.18F * Mth.sin(age * 1.7F + center);
        drawFallbackStarAt(entity, poseStack, vc, x, y, z,
                scale * (0.60F + pulse * 0.30F), rotation + age * 2.4F,
                pulse * alphaScale * flicker);
    }

    private void drawBillboardAt(SwordEnlightenmentEntity entity, PoseStack poseStack, VertexConsumer vc,
                                 float x, float y, float z, float scale, float rotation,
                                 float red, float green, float blue, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        Matrix4f mat = poseStack.last().pose();
        float half = scale * 0.5F;
        quad(vc, mat,
                -half, -half, 0.0F, 6.0F, 1.0F,
                half, -half, 0.0F, 7.0F, 1.0F,
                half, half, 0.0F, 7.0F, 0.0F,
                -half, half, 0.0F, 6.0F, 0.0F,
                red, green, blue, alpha);
        poseStack.popPose();
    }

    private void drawFallbackStarAt(SwordEnlightenmentEntity entity, PoseStack poseStack, VertexConsumer vc,
                                    float x, float y, float z, float scale, float rotation, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        Matrix4f mat = poseStack.last().pose();

        drawScreenRibbon(vc, mat, 0.0F, scale * 0.52F, scale * 0.025F,
                1.0F, 0.88F, 1.0F, alpha * 0.92F);
        drawScreenRibbon(vc, mat, 90.0F, scale * 0.52F, scale * 0.025F,
                1.0F, 0.88F, 1.0F, alpha * 0.76F);
        drawScreenRibbon(vc, mat, 45.0F, scale * 0.38F, scale * 0.020F,
                0.72F, 0.28F, 1.0F, alpha * 0.60F);
        drawScreenRibbon(vc, mat, -45.0F, scale * 0.38F, scale * 0.020F,
                0.72F, 0.28F, 1.0F, alpha * 0.60F);
        drawScreenDiamond(vc, mat, scale * 0.090F, scale * 0.145F,
                1.0F, 0.82F, 1.0F, alpha * 0.88F);
        drawScreenDiamond(vc, mat, scale * 0.18F, scale * 0.26F,
                0.56F, 0.18F, 1.0F, alpha * 0.32F);

        poseStack.popPose();
    }

    private static void drawScreenRibbon(VertexConsumer vc, Matrix4f mat,
                                         float angleDeg, float halfLength, float halfWidth,
                                         float red, float green, float blue, float alpha) {
        float angle = angleDeg * Mth.DEG_TO_RAD;
        float dx = Mth.cos(angle) * halfLength;
        float dy = Mth.sin(angle) * halfLength;
        float sx = -Mth.sin(angle) * halfWidth;
        float sy = Mth.cos(angle) * halfWidth;
        quad(vc, mat,
                -dx - sx, -dy - sy, 0.0F, 2.0F, 0.0F,
                dx - sx, dy - sy, 0.0F, 3.0F, 0.0F,
                dx + sx, dy + sy, 0.0F, 3.0F, 1.0F,
                -dx + sx, -dy + sy, 0.0F, 2.0F, 1.0F,
                red, green, blue, alpha);
    }

    private static void drawScreenDiamond(VertexConsumer vc, Matrix4f mat,
                                          float halfWidth, float halfHeight,
                                          float red, float green, float blue, float alpha) {
        quad(vc, mat,
                -halfWidth, 0.0F, 0.0F, 8.0F, 0.5F,
                0.0F, halfHeight, 0.0F, 8.5F, 0.0F,
                halfWidth, 0.0F, 0.0F, 9.0F, 0.5F,
                0.0F, -halfHeight, 0.0F, 8.5F, 1.0F,
                red, green, blue, alpha);
    }

    private static void drawCrescentSweep(VertexConsumer vc, Matrix4f mat, float age,
                                          float start, float duration, float radius,
                                          float startDeg, float endDeg,
                                          float yStart, float yEnd, float zOffset, float width,
                                          float red, float green, float blue, float alphaScale) {
        float reveal = smootherStep(stage(age, start, duration));
        float fade = 1.0F - smootherStep(stage(age, start + duration + 9.0F, 9.0F));
        float alpha = reveal * fade * alphaScale;
        if (alpha <= 0.01F) {
            return;
        }

        int visible = Mth.clamp(Mth.ceil(SWEEP_SEGMENTS * reveal), 2, SWEEP_SEGMENTS);
        for (int i = 0; i < visible; i++) {
            float t0 = i / (float) SWEEP_SEGMENTS;
            float t1 = Math.min((i + 1) / (float) SWEEP_SEGMENTS, reveal);
            if (t1 <= t0) {
                continue;
            }
            SweepPoint p0 = sweepPoint(t0, radius, startDeg, endDeg, yStart, yEnd, zOffset);
            SweepPoint p1 = sweepPoint(t1, radius, startDeg, endDeg, yStart, yEnd, zOffset);
            float w0 = width * sweepWidth(t0);
            float w1 = width * sweepWidth(t1);
            float a0 = alpha * sweepAlpha(t0, reveal);
            float a1 = alpha * sweepAlpha(t1, reveal);

            quadAlpha(vc, mat,
                    p0.x + p0.sideX * w0, p0.y, p0.z + p0.sideZ * w0, 0.0F + t0, 0.0F, a0,
                    p1.x + p1.sideX * w1, p1.y, p1.z + p1.sideZ * w1, 0.0F + t1, 0.0F, a1,
                    p1.x - p1.sideX * w1, p1.y, p1.z - p1.sideZ * w1, 0.0F + t1, 1.0F, a1 * 0.88F,
                    p0.x - p0.sideX * w0, p0.y, p0.z - p0.sideZ * w0, 0.0F + t0, 1.0F, a0 * 0.88F,
                    red, green, blue);
            quadAlpha(vc, mat,
                    p0.x + p0.sideX * w0 * 0.32F, p0.y + 0.04F, p0.z + p0.sideZ * w0 * 0.32F, 2.0F + t0, 0.0F, a0 * 1.25F,
                    p1.x + p1.sideX * w1 * 0.32F, p1.y + 0.04F, p1.z + p1.sideZ * w1 * 0.32F, 2.0F + t1, 0.0F, a1 * 1.25F,
                    p1.x - p1.sideX * w1 * 0.32F, p1.y - 0.04F, p1.z - p1.sideZ * w1 * 0.32F, 2.0F + t1, 1.0F, a1 * 1.05F,
                    p0.x - p0.sideX * w0 * 0.32F, p0.y - 0.04F, p0.z - p0.sideZ * w0 * 0.32F, 2.0F + t0, 1.0F, a0 * 1.05F,
                    1.0F, 0.88F, 1.0F);
        }
    }

    private static void drawVerticalArcSlash(VertexConsumer vc, Matrix4f mat, float age,
                                             float start, float duration,
                                             float centerX, float centerY, float centerZ,
                                             float radiusX, float radiusY, float depthBend,
                                             float startDeg, float endDeg, float width,
                                             float red, float green, float blue, float alphaScale) {
        float reveal = smootherStep(stage(age, start, duration));
        float fade = 1.0F - smootherStep(stage(age, start + duration + 5.0F, 8.0F));
        float alpha = reveal * fade * alphaScale;
        if (alpha <= 0.01F) {
            return;
        }

        int visible = Mth.clamp(Mth.ceil(SWEEP_SEGMENTS * reveal), 2, SWEEP_SEGMENTS);
        for (int i = 0; i < visible; i++) {
            float t0 = i / (float) SWEEP_SEGMENTS;
            float t1 = Math.min((i + 1) / (float) SWEEP_SEGMENTS, reveal);
            if (t1 <= t0) {
                continue;
            }

            VerticalArcPoint p0 = verticalArcPoint(t0, centerX, centerY, centerZ,
                    radiusX, radiusY, depthBend, startDeg, endDeg);
            VerticalArcPoint p1 = verticalArcPoint(t1, centerX, centerY, centerZ,
                    radiusX, radiusY, depthBend, startDeg, endDeg);
            float w0 = width * verticalArcWidth(t0);
            float w1 = width * verticalArcWidth(t1);
            float a0 = alpha * verticalArcAlpha(t0, reveal);
            float a1 = alpha * verticalArcAlpha(t1, reveal);

            quadAlpha(vc, mat,
                    p0.x + p0.sideX * w0, p0.y + p0.sideY * w0, p0.z, t0, 0.0F, a0,
                    p1.x + p1.sideX * w1, p1.y + p1.sideY * w1, p1.z, t1, 0.0F, a1,
                    p1.x - p1.sideX * w1, p1.y - p1.sideY * w1, p1.z, t1, 1.0F, a1 * 0.90F,
                    p0.x - p0.sideX * w0, p0.y - p0.sideY * w0, p0.z, t0, 1.0F, a0 * 0.90F,
                    red, green, blue);
            quadAlpha(vc, mat,
                    p0.x + p0.sideX * w0 * 0.26F, p0.y + p0.sideY * w0 * 0.26F, p0.z - 0.018F, 2.0F + t0, 0.0F, a0 * 1.36F,
                    p1.x + p1.sideX * w1 * 0.26F, p1.y + p1.sideY * w1 * 0.26F, p1.z - 0.018F, 2.0F + t1, 0.0F, a1 * 1.36F,
                    p1.x - p1.sideX * w1 * 0.26F, p1.y - p1.sideY * w1 * 0.26F, p1.z + 0.018F, 2.0F + t1, 1.0F, a1 * 1.14F,
                    p0.x - p0.sideX * w0 * 0.26F, p0.y - p0.sideY * w0 * 0.26F, p0.z + 0.018F, 2.0F + t0, 1.0F, a0 * 1.14F,
                    1.0F, 0.84F, 1.0F);
        }
    }

    private static void drawOculusCurvedFilament(VertexConsumer vc, Matrix4f mat, float age,
                                                 float start, float duration,
                                                 float centerX, float centerY, float centerZ,
                                                 float radiusX, float radiusY,
                                                 float startDeg, float endDeg, float width,
                                                 float red, float green, float blue, float alphaScale) {
        float reveal = smootherStep(stage(age, start, duration));
        float fade = 1.0F - smootherStep(stage(age, start + duration + 4.0F, 8.0F));
        float alpha = reveal * fade * alphaScale;
        if (alpha <= 0.01F) {
            return;
        }

        int segments = 24;
        int visible = Mth.clamp(Mth.ceil(segments * reveal), 2, segments);
        for (int i = 0; i < visible; i++) {
            float t0 = i / (float) segments;
            float t1 = Math.min((i + 1) / (float) segments, reveal);
            if (t1 <= t0) {
                continue;
            }

            Vector3f p0 = filamentPoint(t0, centerX, centerY, centerZ,
                    radiusX, radiusY, startDeg, endDeg);
            Vector3f p1 = filamentPoint(t1, centerX, centerY, centerZ,
                    radiusX, radiusY, startDeg, endDeg);
            float local = edgeFade(t0) * (0.62F + Mth.sin(t0 * Mth.PI) * 0.38F);
            drawLineRibbon(vc, mat, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                    width * (0.72F + Mth.sin(t0 * Mth.PI) * 0.50F),
                    red, green, blue, alpha * local);
            if ((i & 3) == 1) {
                drawLineRibbon(vc, mat,
                        p0.x, p0.y + width * 1.45F, p0.z - width * 0.70F,
                        p1.x, p1.y + width * 1.45F, p1.z - width * 0.70F,
                        width * 0.38F,
                        1.0F, 0.86F, 1.0F, alpha * local * 0.52F);
            }
        }
    }

    private static Vector3f filamentPoint(float t,
                                          float centerX, float centerY, float centerZ,
                                          float radiusX, float radiusY,
                                          float startDeg, float endDeg) {
        float angle = (startDeg + (endDeg - startDeg) * t) * Mth.DEG_TO_RAD;
        float x = centerX + Mth.cos(angle) * radiusX;
        float y = centerY + Mth.sin(angle) * radiusY + Mth.sin(t * Mth.PI) * 0.42F;
        float z = centerZ + Mth.sin(t * Mth.PI) * 0.55F + Mth.cos(angle * 0.5F) * 0.22F;
        return new Vector3f(x, y, z);
    }

    private static VerticalArcPoint verticalArcPoint(float t,
                                                     float centerX, float centerY, float centerZ,
                                                     float radiusX, float radiusY, float depthBend,
                                                     float startDeg, float endDeg) {
        float angle = (startDeg + (endDeg - startDeg) * t) * Mth.DEG_TO_RAD;
        float x = centerX + Mth.cos(angle) * radiusX;
        float y = centerY + Mth.sin(angle) * radiusY;
        float z = centerZ + Mth.sin(t * Mth.PI) * depthBend;

        float angleSpan = (endDeg - startDeg) * Mth.DEG_TO_RAD;
        float dx = -Mth.sin(angle) * radiusX * angleSpan;
        float dy = Mth.cos(angle) * radiusY * angleSpan;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len <= 1.0E-5F) {
            return new VerticalArcPoint(x, y, z, 0.0F, 1.0F);
        }
        return new VerticalArcPoint(x, y, z, -dy / len, dx / len);
    }

    private record VerticalArcPoint(float x, float y, float z, float sideX, float sideY) {
    }

    private static float verticalArcWidth(float t) {
        float belly = Mth.sin(t * Mth.PI);
        return 0.20F + (float) Math.pow(Math.max(0.0F, belly), 0.48D) * 0.94F;
    }

    private static float verticalArcAlpha(float t, float reveal) {
        float head = smootherStep(Mth.clamp(t / Math.max(0.001F, reveal), 0.0F, 1.0F));
        float tail = smootherStep(Mth.clamp(t / 0.10F, 0.0F, 1.0F));
        float end = 1.0F - smootherStep(Mth.clamp((t - 0.94F) / 0.06F, 0.0F, 1.0F));
        return (0.32F + head * 0.68F) * tail * end;
    }

    private static SweepPoint sweepPoint(float t, float radius, float startDeg, float endDeg,
                                         float yStart, float yEnd, float zOffset) {
        float angle = (startDeg + (endDeg - startDeg) * t) * Mth.DEG_TO_RAD;
        float x = Mth.sin(angle) * radius;
        float z = Mth.cos(angle) * radius + zOffset;
        float y = Mth.lerp(t, yStart, yEnd) + Mth.sin(t * Mth.PI) * 0.56F;
        return new SweepPoint(x, y, z, Mth.sin(angle), Mth.cos(angle));
    }

    private record SweepPoint(float x, float y, float z, float sideX, float sideZ) {
    }

    private static float sweepWidth(float t) {
        return 0.16F + (float) Math.pow(Mth.sin(t * Mth.PI), 0.55D) * 0.84F;
    }

    private static float sweepAlpha(float t, float reveal) {
        float head = smootherStep(Mth.clamp(t / Math.max(0.001F, reveal), 0.0F, 1.0F));
        float tail = smootherStep(Mth.clamp(t / 0.14F, 0.0F, 1.0F));
        float end = 1.0F - smootherStep(Mth.clamp((t - 0.92F) / 0.08F, 0.0F, 1.0F));
        return (0.26F + head * 0.74F) * tail * end;
    }

    private static void drawTimedBlade(VertexConsumer vc, Matrix4f mat, float age,
                                       float start, float duration,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float width,
                                       float red, float green, float blue, float alphaScale) {
        float in = smootherStep(stage(age, start, 1.2F));
        float out = 1.0F - smootherStep(stage(age, start + duration - 2.2F, 2.2F));
        float alpha = in * out * alphaScale;
        if (alpha <= 0.01F) {
            return;
        }

        float jitter = Mth.sin(age * 0.66F + start) * 0.05F;
        drawLineRibbon(vc, mat,
                x0, y0 + jitter, z0,
                x1, y1 - jitter, z1,
                width * (0.75F + in * 0.55F),
                red, green, blue, alpha);
    }

    private static void drawLineRibbon(VertexConsumer vc, Matrix4f mat,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float width,
                                       float red, float green, float blue, float alpha) {
        Vector3f dir = new Vector3f(x1 - x0, y1 - y0, z1 - z0);
        if (dir.lengthSquared() <= 1.0E-5F) {
            return;
        }
        dir.normalize();
        Vector3f side = new Vector3f(-dir.z, 0.0F, dir.x);
        if (side.lengthSquared() <= 1.0E-5F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize(width);

        quad(vc, mat,
                x0 + side.x, y0 + side.y, z0 + side.z, 2.0F, 0.0F,
                x1 + side.x, y1 + side.y, z1 + side.z, 3.0F, 0.0F,
                x1 - side.x, y1 - side.y, z1 - side.z, 3.0F, 1.0F,
                x0 - side.x, y0 - side.y, z0 - side.z, 2.0F, 1.0F,
                red, green, blue, alpha);
    }

    private static void drawLiftedLoop(VertexConsumer vc, Matrix4f mat,
                                       float phase, float arc, float rx, float rz,
                                       float baseY, float lift, float width,
                                       float red, float green, float blue, float alpha) {
        for (int i = 0; i < LOOP_SEGMENTS; i++) {
            float t0 = i / (float) LOOP_SEGMENTS;
            float t1 = (i + 1) / (float) LOOP_SEGMENTS;
            float a0 = phase + (t0 - 0.5F) * arc;
            float a1 = phase + (t1 - 0.5F) * arc;
            Vector3f p0 = loopPoint(a0, rx, rz, baseY, lift);
            Vector3f p1 = loopPoint(a1, rx, rz, baseY, lift);
            float aMid = edgeFade(t0) * alpha;
            drawLineRibbon(vc, mat, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z,
                    width, red, green, blue, aMid);
        }
    }

    private static Vector3f loopPoint(float angle, float rx, float rz, float baseY, float lift) {
        float x = Mth.cos(angle) * rx;
        float z = Mth.sin(angle) * rz + 2.0F;
        float y = baseY + Mth.sin(angle * 0.62F + 0.7F) * lift + Mth.sin(angle * 1.7F) * 0.16F;
        return new Vector3f(x, y, z);
    }

    private static void drawRing(VertexConsumer vc, Matrix4f mat, float y, float radius, float width,
                                 float red, float green, float blue, float alpha) {
        drawRingInternal(vc, mat, y, radius, width, 0.0F, red, green, blue, alpha, false);
    }

    private static void drawBrokenRing(VertexConsumer vc, Matrix4f mat, float y, float radius, float width,
                                       float scatter, float red, float green, float blue, float alpha) {
        drawRingInternal(vc, mat, y, radius, width, scatter, red, green, blue, alpha, true);
    }

    private static void drawRingInternal(VertexConsumer vc, Matrix4f mat, float y, float radius, float width,
                                         float scatter, float red, float green, float blue, float alpha,
                                         boolean broken) {
        float inner = Math.max(0.04F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        int gapRate = Math.max(2, 8 - Mth.floor(scatter * 5.0F));
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float noise = 0.5F + 0.5F * Mth.sin(i * 1.73F + scatter * 9.0F);
            if (broken && scatter > 0.22F
                    && ((i + Mth.floor(scatter * 13.0F)) % gapRate == 0 || noise < scatter * 0.18F)) {
                continue;
            }

            float t0 = i / (float) RING_SEGMENTS;
            float t1 = (i + 1) / (float) RING_SEGMENTS;
            float a0 = t0 * Mth.TWO_PI;
            float a1 = t1 * Mth.TWO_PI;
            float localAlpha = alpha * (broken ? (0.72F + noise * 0.28F) * (1.0F - scatter * 0.28F) : 1.0F);
            quad(vc, mat,
                    Mth.cos(a0) * outer, y, Mth.sin(a0) * outer, 4.0F + t0, 0.0F,
                    Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, 4.0F + t1, 0.0F,
                    Mth.cos(a1) * inner, y, Mth.sin(a1) * inner, 4.0F + t1, 1.0F,
                    Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, 4.0F + t0, 1.0F,
                    red, green, blue, localAlpha);
        }
    }

    private static void drawShard(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                                  float size, float rotation,
                                  float red, float green, float blue, float alpha) {
        float c = Mth.cos(rotation);
        float s = Mth.sin(rotation);
        float mx = c * size;
        float mz = s * size;
        float sx = -s * size * 0.35F;
        float sz = c * size * 0.35F;
        float sy = size * 1.42F;
        quad(vc, mat,
                x - mx, y, z - mz, 8.0F, 0.5F,
                x + sx, y + sy, z + sz, 8.5F, 0.0F,
                x + mx, y, z + mz, 9.0F, 0.5F,
                x - sx, y - sy, z - sz, 8.5F, 1.0F,
                red, green, blue, alpha);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float red, float green, float blue, float alpha) {
        quadAlpha(vc, mat,
                x0, y0, z0, u0, v0, alpha,
                x1, y1, z1, u1, v1, alpha,
                x2, y2, z2, u2, v2, alpha,
                x3, y3, z3, u3, v3, alpha,
                red, green, blue);
    }

    private static void quadAlpha(VertexConsumer vc, Matrix4f mat,
                                  float x0, float y0, float z0, float u0, float v0, float a0,
                                  float x1, float y1, float z1, float u1, float v1, float a1,
                                  float x2, float y2, float z2, float u2, float v2, float a2,
                                  float x3, float y3, float z3, float u3, float v3, float a3,
                                  float red, float green, float blue) {
        vertex(vc, mat, x0, y0, z0, u0, v0, red, green, blue, a0);
        vertex(vc, mat, x1, y1, z1, u1, v1, red, green, blue, a1);
        vertex(vc, mat, x2, y2, z2, u2, v2, red, green, blue, a2);
        vertex(vc, mat, x3, y3, z3, u3, v3, red, green, blue, a3);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                               float u, float v, float red, float green, float blue, float alpha) {
        float safeAlpha = Mth.clamp(KabladeRenderTypes.fallbackAlpha(alpha, 1.28F), 0.0F, 1.0F);
        if (KabladeRenderTypes.useShaderFallbackTextures()) {
            vc.vertex(mat, x, y, z).color(red, green, blue, safeAlpha).endVertex();
            return;
        }
        vc.vertex(mat, x, y, z)
                .color(red, green, blue, safeAlpha)
                .uv(KabladeRenderTypes.swordEnlightenmentU(u), v)
                .endVertex();
    }

    private static float stage(float age, float start, float duration) {
        return Mth.clamp((age - start) / duration, 0.0F, 1.0F);
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float fastOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv * inv;
    }

    private static float edgeFade(float t) {
        float head = smootherStep(Mth.clamp(t / 0.12F, 0.0F, 1.0F));
        float tail = 1.0F - smootherStep(Mth.clamp((t - 0.88F) / 0.12F, 0.0F, 1.0F));
        return head * tail;
    }

    private static float deterministic(int index, float salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordEnlightenmentEntity entity) {
        return EMPTY_TEXTURE;
    }
}

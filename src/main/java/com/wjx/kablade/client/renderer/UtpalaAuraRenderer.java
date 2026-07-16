package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.UtpalaAuraEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Procedural renderer for Frozen Naraka's Honkai-style blue vortex, ice burst, and phantom blade. */
public final class UtpalaAuraRenderer extends EntityRenderer<UtpalaAuraEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    private static final float BLUE_R = 0.07F;
    private static final float BLUE_G = 0.54F;
    private static final float BLUE_B = 1.0F;
    private static final float CYAN_R = 0.18F;
    private static final float CYAN_G = 0.86F;
    private static final float CYAN_B = 1.0F;
    private static final float[] PRIMARY_ICE_SIDES = {-2.35F, -1.72F, -1.05F, -0.38F, 0.36F, 1.06F};

    public UtpalaAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(UtpalaAuraEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (BloodfyreOculusPipeline.enqueue(entity, partialTick)) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        float age = entity.tickCount + partialTick;
        VertexConsumer veil = buffer.getBuffer(KabladeRenderTypes.utpalaAuraVeil());
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.utpalaAura());

        poseStack.pushPose();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        renderLocalVeil(age, poseStack, veil);
        renderFootGlow(age, poseStack, vc);
        renderOpeningIceRing(age, poseStack, vc);
        renderOpeningWindRipples(age, poseStack, vc);
        renderVortex(age, poseStack, vc);
        renderIceBurst(age, poseStack, vc);
        renderPhantomBlade(age, poseStack, vc);
        renderResidualStream(age, poseStack, vc);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    static void renderOculusColor(PoseStack poseStack,
                                  BloodfyreOculusPipeline.DrawContext context,
                                  float age, float yaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.UTPALA,
                BloodfyreOculusPipeline.BlendMode.ALPHA,
                vc -> renderLocalVeil(age, poseStack, vc));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.UTPALA,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderOculusMain(age, poseStack, vc));
        poseStack.popPose();
    }

    static void renderOculusGlow(PoseStack poseStack,
                                 BloodfyreOculusPipeline.DrawContext context,
                                 float age, float yaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.UTPALA,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderOculusMain(age, poseStack, vc));
        poseStack.popPose();
    }

    private static void renderOculusMain(float age, PoseStack poseStack, VertexConsumer vc) {
        renderFootGlow(age, poseStack, vc);
        renderOpeningIceRing(age, poseStack, vc);
        renderOpeningWindRipples(age, poseStack, vc);
        renderVortex(age, poseStack, vc);
        renderIceBurst(age, poseStack, vc);
        renderPhantomBlade(age, poseStack, vc);
        renderResidualStream(age, poseStack, vc);
    }

    private static boolean useLegacyShaderFallback() {
        return KabladeRenderTypes.useShaderFallbackTextures()
                && !BloodfyreOculusPipeline.isPrivateGeometryPass();
    }

    private static void renderFootGlow(float age, PoseStack poseStack, VertexConsumer vc) {
        float appear = smootherStep(stage(age, 0.0F, 4.0F));
        float fade = 1.0F - smootherStep(stage(age, 46.0F, 12.0F));
        float alpha = appear * fade;
        if (alpha <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.38F);
        drawRing(mat, vc, 0.035F, 0.44F + pulse * 0.035F, 0.075F,
                CYAN_R, CYAN_G, CYAN_B, alpha * 0.42F);
        drawGroundFlame(mat, vc, age, alpha * 0.34F);
    }

    private static void renderOpeningIceRing(float age, PoseStack poseStack, VertexConsumer vc) {
        float open = smootherStep(stage(age, 0.0F, 18.0F));
        float fade = 1.0F - smootherStep(stage(age, 12.0F, 12.0F));
        float alpha = fade * (1.0F - open * 0.34F);
        if (alpha <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        float scatter = smootherStep(stage(age, 4.0F, 14.0F));
        drawBrokenRing(mat, vc, 0.052F, Mth.lerp(open, 0.38F, 5.85F),
                Mth.lerp(open, 0.08F, 0.58F), scatter,
                0.76F, 0.98F, 1.0F, alpha * 0.78F);
        drawBrokenRing(mat, vc, 0.064F, Mth.lerp(open, 0.52F, 6.65F),
                Mth.lerp(open, 0.06F, 0.46F), scatter,
                BLUE_R, 0.68F, 1.0F, alpha * 0.42F);
    }

    private static void renderOpeningWindRipples(float age, PoseStack poseStack, VertexConsumer vc) {
        float envelope = 1.0F - smootherStep(stage(age, 32.0F, 8.0F));
        if (envelope <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        for (int i = 0; i < 7; i++) {
            float delay = i * 3.0F;
            if (age < delay) {
                continue;
            }

            float t = stage(age, delay, 18.0F);
            if (t >= 1.0F) {
                continue;
            }

            float open = smootherStep(t);
            float fade = 1.0F - smootherStep(stage(age, delay + 8.0F, 10.0F));
            float scatter = smootherStep(stage(age, delay + 7.0F, 10.0F));
            float alpha = envelope * fade * (0.74F - i * 0.055F);
            if (alpha <= 0.01F) {
                continue;
            }

            float radius = 0.34F + open * (5.60F + i * 0.44F);
            float width = 0.07F + open * (0.30F + i * 0.024F);
            float y = 0.078F + (i & 1) * 0.034F;
            drawBrokenRing(mat, vc, y, radius, width, scatter * 0.72F,
                    i % 2 == 0 ? 0.74F : BLUE_R,
                    i % 2 == 0 ? 0.98F : 0.62F,
                    1.0F,
                    alpha * 0.56F);

            drawLiftedLoop(mat, vc, age * (0.10F + i * 0.012F) + i * 0.58F,
                    (0.58F + open * 0.36F) * Mth.TWO_PI,
                    radius * (0.86F + i * 0.012F), radius * 0.64F,
                    width * 0.58F, 0.12F + open * 0.10F,
                    0.16F + open * 0.42F,
                    alpha * 0.24F,
                    0.18F, 0.78F, 1.0F);
        }
    }

    private static void renderLocalVeil(float age, PoseStack poseStack, VertexConsumer vc) {
        float open = smootherStep(stage(age, 6.0F, 8.0F));
        float fade = 1.0F - smootherStep(stage(age, 34.0F, 10.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        quadWithUv(vc, mat, 8.0F,
                -6.8F, 0.045F, -4.5F, 0.01F, 0.05F, 0.18F, alpha * 0.07F,
                6.8F, 0.045F, -4.5F, 0.01F, 0.06F, 0.22F, alpha * 0.07F,
                6.8F, 0.045F, 6.6F, 0.03F, 0.12F, 0.30F, alpha * 0.11F,
                -6.8F, 0.045F, 6.6F, 0.03F, 0.12F, 0.30F, alpha * 0.11F);

        for (int i = 0; i < 2; i++) {
            float z = -3.2F + i * 4.6F;
            float drift = Mth.sin(age * 0.06F + i * 1.7F) * 0.35F;
            float a = alpha * (0.045F + i * 0.02F);
            quadWithUv(vc, mat, 8.0F,
                    -7.6F, 0.42F, z + drift, 0.005F, 0.03F, 0.11F, a,
                    7.6F, 0.42F, z - drift, 0.005F, 0.03F, 0.11F, a,
                    7.6F, 3.10F, z + 0.62F - drift, 0.02F, 0.10F, 0.24F, a * 0.62F,
                    -7.6F, 3.10F, z + 0.62F + drift, 0.02F, 0.10F, 0.24F, a * 0.62F);
        }
    }

    private static void renderVortex(float age, PoseStack poseStack, VertexConsumer vc) {
        float snap = dampedSnap(stage(age, 8.0F, 8.0F));
        float grow = smootherStep(stage(age, 7.0F, 3.0F));
        float fade = 1.0F - smootherStep(stage(age, 37.0F, 7.0F));
        float intensity = grow * fade;
        float burst = exitBurstEnvelope(age);
        if (intensity <= 0.01F && burst <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        if (intensity > 0.01F) {
            renderDenseWindWaves(age, mat, vc, intensity, snap);
        }

        for (int i = 0; i < 12; i++) {
            float delay = 9.5F + i * 0.36F;
            float wave = dampedSnap(stage(age, delay, 8.0F));
            float waveAlpha = Mth.clamp(wave, 0.0F, 1.0F);
            float local = waveAlpha * (1.0F - smootherStep(stage(age, 30.0F + (i % 4) * 0.35F, 8.0F))) * fade;
            if (local <= 0.01F) {
                continue;
            }

            float arc = (0.34F + (i % 4) * 0.10F) * Mth.PI;
            float radius = Mth.lerp(wave, 0.52F + i * 0.08F, 5.15F + i * 0.20F);
            float spin = age * (0.18F + i * 0.01F) + i * 0.74F;
            drawLiftedLoop(mat, vc, spin, arc, radius * 1.12F, radius * 0.82F,
                    0.05F + wave * 0.11F, 0.16F + (i % 3) * 0.12F,
                    0.12F + wave * 0.30F, local * 0.34F,
                    i % 2 == 0 ? 0.82F : BLUE_R, i % 2 == 0 ? 0.98F : 0.58F, 1.0F);
        }

        for (int i = 0; i < 26; i++) {
            float delay = 10.5F + i * 0.20F;
            float localOpen = dampedSnap(stage(age, delay, 7.2F));
            float openAlpha = Mth.clamp(localOpen, 0.0F, 1.0F);
            float localFade = 1.0F - smootherStep(stage(age, 33.0F + (i % 5) * 0.25F, 7.0F));
            float local = openAlpha * localFade;
            if (local <= 0.01F) {
                continue;
            }

            float seed = i * 1.618F;
            float radius = Mth.lerp(localOpen, 0.68F + (i % 3) * 0.10F,
                    4.65F + (i % 8) * 0.34F + Mth.sin(seed) * 0.24F);
            float width = Mth.lerp(openAlpha, 0.05F, 0.36F + (i % 5) * 0.056F);
            float baseY = 0.12F + (i % 8) * 0.19F + openAlpha * 0.16F;
            float lift = 0.05F + openAlpha * (0.28F + (i % 5) * 0.065F);
            float arc = (1.18F + openAlpha * 1.56F + (i % 5) * 0.08F) * Mth.PI;
            float spin = age * (0.24F + (i % 5) * 0.022F) + seed;
            float r = i % 3 == 0 ? 0.88F : BLUE_R;
            float g = i % 3 == 0 ? 1.00F : 0.62F;
            float b = 1.0F;
            drawSpiralRibbon(mat, vc, spin, arc, radius, width, baseY, lift,
                    local * fade * (i % 3 == 0 ? 0.42F : 0.32F), r, g, b);
        }

        float peak = smootherStep(stage(age, 22.0F, 8.0F))
                * (1.0F - smootherStep(stage(age, 36.0F, 9.0F)));
        if (peak > 0.01F) {
            for (int i = 0; i < 4; i++) {
                float phase = age * (0.13F + i * 0.02F) + i * 1.28F;
                drawLiftedLoop(mat, vc, phase, (1.04F + i * 0.08F) * Mth.TWO_PI,
                        3.5F + i * 0.54F, 2.52F + i * 0.34F,
                        0.20F + i * 0.03F, 0.44F + i * 0.19F,
                        0.72F + i * 0.18F, peak * (0.24F + i * 0.05F),
                        i % 2 == 0 ? 0.88F : 0.08F,
                        i % 2 == 0 ? 1.00F : 0.64F, 1.0F);
            }
        }

        if (burst > 0.01F) {
            renderVortexExitBurst(age, mat, vc, burst);
        }
    }

    private static void renderDenseWindWaves(float age, Matrix4f mat, VertexConsumer vc, float intensity, float snap) {
        float snapAlpha = Mth.clamp(snap, 0.0F, 1.0F);
        float reboundFlash = Mth.clamp((snap - 1.0F) * 3.4F, 0.0F, 0.55F);
        for (int i = 0; i < 9; i++) {
            float radius = Mth.lerp(snap, 0.54F + i * 0.10F, 4.35F + i * 0.34F);
            float width = Mth.lerp(snapAlpha, 0.035F, 0.20F + i * 0.018F);
            float alpha = intensity * (0.34F - i * 0.018F) * (0.78F + snapAlpha * 0.22F + reboundFlash);
            if (alpha <= 0.01F) {
                continue;
            }

            float red = i % 3 == 0 ? 0.82F : BLUE_R;
            float green = i % 3 == 0 ? 1.00F : 0.64F;
            drawRing(mat, vc, 0.06F + i * 0.030F, radius, width, red, green, 1.0F, alpha);
        }

        for (int i = 0; i < 14; i++) {
            float delay = 9.4F + i * 0.34F;
            float t = stage(age, delay, 8.5F);
            if (t >= 1.0F) {
                continue;
            }

            float wave = dampedSnap(t);
            float waveAlpha = Mth.clamp(wave, 0.0F, 1.0F);
            float fade = 1.0F - smootherStep(stage(age, delay + 6.5F, 5.5F));
            float alpha = intensity * fade * (0.26F - i * 0.009F);
            if (alpha <= 0.01F) {
                continue;
            }

            float radius = Mth.lerp(wave, 0.58F + (i % 3) * 0.08F, 5.30F + i * 0.16F);
            float width = Mth.lerp(waveAlpha, 0.050F, 0.34F + (i % 4) * 0.028F);
            float y = 0.07F + (i % 4) * 0.045F;
            float red = i % 3 == 0 ? 0.80F : BLUE_R;
            float green = i % 3 == 0 ? 0.98F : 0.62F;
            drawRing(mat, vc, y, radius, width, red, green, 1.0F, alpha);

            if ((i & 1) == 0) {
                drawLiftedLoop(mat, vc, age * (0.10F + i * 0.006F) + i * 0.64F,
                        (0.72F + waveAlpha * 0.34F) * Mth.TWO_PI,
                        radius * 0.96F, radius * 0.70F,
                        width * 0.44F, 0.18F + (i % 3) * 0.10F,
                        0.22F + waveAlpha * 0.42F,
                        alpha * 0.34F,
                        red, green, 1.0F);
            }
        }
    }

    private static void renderVortexExitBurst(float age, Matrix4f mat, VertexConsumer vc, float burst) {
        float charge = smootherStep(stage(age, 38.0F, 1.0F))
                * (1.0F - smootherStep(stage(age, 40.0F, 1.2F)));
        if (charge > 0.01F) {
            drawRing(mat, vc, 0.16F, Mth.lerp(charge, 6.25F, 4.95F),
                    Mth.lerp(charge, 0.70F, 0.26F),
                    0.92F, 1.0F, 1.0F, charge * 0.58F);
        }

        float spreadStart = 40.0F;
        float spread = fastOut(stage(age, spreadStart, 3.4F));
        float scatter = fastOut(stage(age, 40.8F, 3.0F));
        float shockFade = fadeDuringExpansion(age, spreadStart, 3.4F, 1.1F, 3.2F);
        float shock = burst * shockFade;
        if (shock > 0.01F) {
            float shockRadius = Mth.lerp(spread, 5.15F, 23.0F);
            drawRing(mat, vc, 0.12F, shockRadius, Mth.lerp(spread, 0.34F, 2.15F),
                    0.90F, 1.0F, 1.0F, shock * 0.74F);
            drawBrokenRing(mat, vc, 0.23F, shockRadius * 0.93F, Mth.lerp(spread, 0.26F, 1.38F),
                    Mth.clamp(scatter * 0.72F, 0.0F, 1.0F),
                    CYAN_R, CYAN_G, CYAN_B, shock * 0.62F);
        }

        for (int i = 0; i < 18; i++) {
            float seed = i * 2.39996F;
            float laneStart = 39.6F + (i % 5) * 0.08F;
            float lane = fastOut(stage(age, laneStart, 3.2F));
            float laneFade = fadeDuringExpansion(age, laneStart, 3.2F, 0.9F, 2.9F);
            float local = burst * laneFade;
            if (local <= 0.01F) {
                continue;
            }

            float head = Mth.lerp(lane, 6.2F + (i % 4) * 0.28F, 20.0F + (i % 6) * 1.15F);
            float tail = Math.max(3.8F, head - Mth.lerp(lane, 1.4F, 6.8F));
            float width = Mth.lerp(lane, 0.08F, 0.28F + (i % 3) * 0.06F);
            drawRadialStreak(mat, vc, seed + age * 0.035F, tail, head, width,
                    0.16F + (i % 3) * 0.08F, 0.32F + lane * 0.54F,
                    local * (0.30F + (i % 3) * 0.04F),
                    i % 3 == 0 ? 0.88F : BLUE_R,
                    i % 3 == 0 ? 1.0F : 0.68F,
                    1.0F);
        }

        for (int i = 0; i < 9; i++) {
            float offset = i * 0.16F;
            float ringStart = 40.1F + offset;
            float localSpread = fastOut(stage(age, ringStart, 3.8F));
            float localFade = fadeDuringExpansion(age, ringStart, 3.8F, 1.1F, 3.0F);
            float local = burst * localFade;
            if (local <= 0.01F) {
                continue;
            }

            float radius = Mth.lerp(localSpread, 5.20F + i * 0.20F, 16.80F + i * 1.55F);
            float width = Mth.lerp(localSpread, 0.18F, 1.10F + i * 0.10F);
            float alpha = local * (0.48F - i * 0.030F);
            drawBrokenRing(mat, vc, 0.10F + i * 0.045F, radius, width,
                    Mth.clamp(scatter + i * 0.06F, 0.0F, 1.0F),
                    i % 2 == 0 ? 0.86F : BLUE_R,
                    i % 2 == 0 ? 1.0F : 0.66F,
                    1.0F,
                    alpha);

            drawLiftedLoop(mat, vc, age * (0.12F + i * 0.010F) + i * 0.84F,
                    (0.86F + spread * 0.60F) * Mth.TWO_PI,
                    radius * 0.90F, radius * 0.60F,
                    width * 0.30F, 0.36F + i * 0.13F,
                    0.42F + spread * 1.28F,
                    alpha * 0.42F,
                    0.18F, 0.76F, 1.0F);
        }
    }

    private static void renderIceBurst(float age, PoseStack poseStack, VertexConsumer vc) {
        float appear = fastOut(stage(age, 27.0F, 2.6F));
        float fade = 1.0F - smootherStep(stage(age, 48.0F, 10.0F));
        float alpha = appear * fade;
        if (alpha <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        float fissureFade = 1.0F - smootherStep(stage(age, 39.0F, 6.0F));
        drawIceFissure(mat, vc, age, alpha * 0.34F * fissureFade);

        for (int i = 0; i < 8; i++) {
            float delay = i * 0.34F;
            float open = fastOut(stage(age, 27.0F + delay, 1.15F));
            float local = open * fade;
            if (local <= 0.01F) {
                continue;
            }

            float side = (-0.86F + (i % 3) * 0.86F) + Mth.sin(i * 1.47F) * 0.16F;
            float ahead = 1.18F + i * 0.84F;
            float height = (1.64F + i * 0.13F + (i % 3) * 0.42F + (i == 4 ? 0.46F : 0.0F))
                    * (0.82F + appear * 0.24F);
            float width = 0.26F + (i % 3) * 0.070F + i * 0.008F;

            poseStack.pushPose();
            poseStack.translate(side, Mth.lerp(open, -height * 0.74F, 0.05F), ahead);
            poseStack.mulPose(Axis.YP.rotationDegrees(-24.0F + i * 9.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-16.0F + (i % 3) * 7.0F));
            drawCrystal(poseStack.last().pose(), vc, height, width,
                    local * alpha * (0.82F + (i % 2) * 0.14F));
            poseStack.popPose();
        }

        for (int i = 0; i < 14; i++) {
            int row = i / 2;
            float delay = row * 0.32F + (i & 1) * 0.09F;
            float open = fastOut(stage(age, 27.35F + delay, 1.35F));
            float local = open * fade;
            if (local <= 0.01F) {
                continue;
            }

            float sideSign = (i & 1) == 0 ? -1.0F : 1.0F;
            float side = sideSign * (1.16F + (row % 3) * 0.34F) + Mth.sin(i * 1.7F) * 0.20F;
            float ahead = 1.52F + row * 0.86F + Math.abs(side) * 0.12F;
            float height = (1.08F + (i % 5) * 0.34F) * (0.80F + appear * 0.30F);
            float width = 0.24F + (i % 4) * 0.055F;

            poseStack.pushPose();
            poseStack.translate(side, Mth.lerp(open, -height * 0.78F, 0.08F), ahead);
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * -18.0F + i * 7.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F + (i % 3) * 6.0F));
            drawCrystal(poseStack.last().pose(), vc, height, width,
                    local * alpha * (0.62F + (i % 3) * 0.10F));
            poseStack.popPose();
        }

        for (int i = 0; i < 8; i++) {
            float open = fastOut(stage(age, 28.2F + i * 0.28F, 1.6F));
            float local = open * fade;
            if (local <= 0.01F) {
                continue;
            }

            float side = -2.35F + i * 0.66F + Mth.sin(i * 2.1F) * 0.22F;
            float ahead = 1.65F + i * 0.74F;
            float y = Mth.lerp(open, -0.22F, 0.72F + (i % 3) * 0.34F + open * 0.42F);
            poseStack.pushPose();
            poseStack.translate(side, y, ahead);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * (2.4F + i * 0.18F) + i * 31.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(24.0F + i * 17.0F));
            drawFloatingShard(poseStack.last().pose(), vc, 0.28F + (i % 3) * 0.08F,
                    local * alpha * 0.70F);
            poseStack.popPose();
        }
    }

    private static void renderPhantomBlade(float age, PoseStack poseStack, VertexConsumer vc) {
        float open = fastOut(stage(age, 35.0F, 2.4F));
        float fade = 1.0F - smootherStep(stage(age, 43.0F, 7.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        float travel = Math.max(0.0F, age - 36.8F) * 1.85F;
        float head = Mth.lerp(open, 1.35F, 9.6F) + travel;
        float tail = Math.max(0.18F, head - Mth.lerp(open, 2.0F, 5.8F));
        float widthFalloff = 1.0F - smootherStep(stage(age, 43.0F, 7.0F)) * 0.45F;
        drawForwardRibbon(mat, vc, Math.max(0.12F, tail - 0.30F), head + 0.32F,
                0.16F + open * 0.16F, (3.65F + open * 0.48F) * widthFalloff, 1.03F,
                age * 0.16F, alpha * 1.18F, 0.96F, 1.0F, 1.0F);
        drawForwardRibbon(mat, vc, Math.max(0.12F, tail - 0.10F), head + 0.10F,
                0.24F + open * 0.16F, (2.55F + open * 0.42F) * widthFalloff, 0.82F,
                age * 0.18F + 1.2F, alpha * 0.42F, 0.18F, 0.72F, 1.0F);

        for (int i = 0; i < 5; i++) {
            float phase = age * (0.12F + i * 0.015F) + i;
            float y = 0.74F + i * 0.10F;
            float width = (0.60F + i * 0.20F) * widthFalloff;
            drawForwardRibbon(mat, vc, tail - i * 0.10F, head - i * 0.06F,
                    width, (2.25F + i * 0.26F) * widthFalloff, y, phase,
                    alpha * (i == 0 ? 0.52F : 0.16F), i == 0 ? 0.92F : BLUE_R,
                    i == 0 ? 1.0F : 0.66F, 1.0F);
        }

        drawBladeArc(mat, vc, head + 0.18F, 1.02F, (3.10F + open * 0.90F) * widthFalloff,
                0.48F + open * 0.20F, alpha * 0.82F, 0.92F, 1.0F, 1.0F);
        drawBladeArc(mat, vc, head - 0.32F, 0.94F, (3.45F + open * 1.10F) * widthFalloff,
                0.86F + open * 0.26F, alpha * 0.24F, 0.10F, 0.62F, 1.0F);
    }

    private static void renderResidualStream(float age, PoseStack poseStack, VertexConsumer vc) {
        float open = smootherStep(stage(age, 45.0F, 4.0F));
        float fade = 1.0F - smootherStep(stage(age, 68.0F, 6.0F));
        float alpha = open * fade;
        if (alpha <= 0.01F) {
            return;
        }

        Matrix4f mat = poseStack.last().pose();
        for (int i = 0; i < 3; i++) {
            float phase = age * (0.08F + i * 0.012F) + i * 1.1F;
            drawLiftedLoop(mat, vc, phase, (0.62F + i * 0.08F) * Mth.TWO_PI,
                    2.6F + i * 0.72F, 1.7F + i * 0.34F,
                    0.10F + i * 0.025F, 0.46F + i * 0.18F,
                    0.34F + i * 0.13F, alpha * (0.08F + i * 0.025F),
                    0.16F, 0.62F, 1.0F);
        }
    }

    private static void renderOculusBloomOverlay(float age, PoseStack poseStack, VertexConsumer vc) {
        Matrix4f mat = poseStack.last().pose();

        float vortex = smootherStep(stage(age, 7.0F, 3.0F))
                * (1.0F - smootherStep(stage(age, 37.0F, 7.0F)));
        if (vortex > 0.01F) {
            float snap = dampedSnap(stage(age, 8.0F, 8.0F));
            drawRing(mat, vc, 0.115F, Mth.lerp(snap, 0.80F, 5.45F), 1.10F,
                    0.58F, 0.98F, 1.0F, vortex * 0.34F);
            drawRing(mat, vc, 0.135F, Mth.lerp(snap, 1.05F, 6.65F), 0.92F,
                    0.16F, 0.70F, 1.0F, vortex * 0.22F);
            for (int i = 0; i < 4; i++) {
                drawLiftedLoop(mat, vc, age * (0.085F + i * 0.012F) + i * 1.4F,
                        (0.82F + i * 0.08F) * Mth.TWO_PI,
                        4.2F + i * 0.55F, 2.9F + i * 0.30F,
                        0.34F + i * 0.06F, 0.34F + i * 0.12F,
                        0.86F + i * 0.20F, vortex * (0.12F + i * 0.026F),
                        0.36F, 0.92F, 1.0F);
            }
        }

        float ice = fastOut(stage(age, 27.0F, 2.6F))
                * (1.0F - smootherStep(stage(age, 48.0F, 10.0F)));
        if (ice > 0.01F) {
            drawIceFissure(mat, vc, age, ice * 0.28F);
            drawForwardRibbon(mat, vc, 1.0F, 8.2F, 0.55F, 1.95F, 0.22F,
                    age * 0.10F, ice * 0.25F, 0.56F, 0.98F, 1.0F);
        }

        float bladeOpen = fastOut(stage(age, 35.0F, 2.4F));
        float blade = bladeOpen * (1.0F - smootherStep(stage(age, 43.0F, 7.0F)));
        if (blade > 0.01F) {
            float travel = Math.max(0.0F, age - 36.8F) * 1.85F;
            float head = Mth.lerp(bladeOpen, 1.35F, 9.6F) + travel;
            float tail = Math.max(0.18F, head - Mth.lerp(bladeOpen, 2.0F, 5.8F));
            drawForwardRibbon(mat, vc, Math.max(0.12F, tail - 0.60F), head + 0.65F,
                    0.46F, 4.90F, 1.00F, age * 0.12F,
                    blade * 0.46F, 0.74F, 0.98F, 1.0F);
            drawBladeArc(mat, vc, head + 0.26F, 1.02F, 4.90F,
                    0.86F, blade * 0.34F, 0.88F, 1.0F, 1.0F);
        }

        float burst = exitBurstEnvelope(age);
        if (burst > 0.01F) {
            float spread = fastOut(stage(age, 40.0F, 3.4F));
            float fade = fadeDuringExpansion(age, 40.0F, 3.4F, 1.1F, 3.2F);
            drawRing(mat, vc, 0.16F, Mth.lerp(spread, 5.0F, 23.5F),
                    Mth.lerp(spread, 0.90F, 3.20F),
                    0.64F, 0.98F, 1.0F, burst * fade * 0.25F);
        }
    }

    private static void drawGroundFlame(Matrix4f mat, VertexConsumer vc, float age, float alpha) {
        int points = 14;
        float inner = 0.26F;
        for (int i = 0; i < points; i++) {
            float a0 = Mth.TWO_PI * i / points + age * 0.05F;
            float a1 = Mth.TWO_PI * (i + 1) / points + age * 0.05F;
            float r0 = 0.72F + 0.18F * Mth.sin(age * 0.23F + i * 1.31F);
            float r1 = 0.72F + 0.18F * Mth.sin(age * 0.23F + (i + 1) * 1.31F);
            quad(vc, mat,
                    Mth.cos(a0) * inner, 0.04F, Mth.sin(a0) * inner, CYAN_R, CYAN_G, CYAN_B, alpha * 0.10F,
                    Mth.cos(a1) * inner, 0.04F, Mth.sin(a1) * inner, CYAN_R, CYAN_G, CYAN_B, alpha * 0.10F,
                    Mth.cos(a1) * r1, 0.04F, Mth.sin(a1) * r1, CYAN_R, CYAN_G, CYAN_B, alpha * 0.42F,
                    Mth.cos(a0) * r0, 0.04F, Mth.sin(a0) * r0, CYAN_R, CYAN_G, CYAN_B, alpha * 0.42F);
        }
    }

    private static void drawSpiralRibbon(Matrix4f mat, VertexConsumer vc, float startAngle, float arc,
                                         float radius, float width, float baseY, float lift,
                                         float alpha, float red, float green, float blue) {
        int segments = 28;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = startAngle + arc * t0;
            float a1 = startAngle + arc * t1;
            float r0 = radius + (t0 - 0.5F) * 0.58F + Mth.sin(a0 * 1.7F) * 0.06F;
            float r1 = radius + (t1 - 0.5F) * 0.58F + Mth.sin(a1 * 1.7F) * 0.06F;
            float y0 = baseY + lift * t0 + Mth.sin(a0 * 1.3F) * 0.10F;
            float y1 = baseY + lift * t1 + Mth.sin(a1 * 1.3F) * 0.10F;
            float aStart = edgeFade(t0);
            float aEnd = edgeFade(t1);
            float wobble0 = width * (0.84F + 0.16F * Mth.sin(a0 * 2.0F));
            float wobble1 = width * (0.84F + 0.16F * Mth.sin(a1 * 2.0F));
            quadWithUvRange(vc, mat, 0.0F, t0, t1,
                    Mth.cos(a0) * (r0 + wobble0), y0, Mth.sin(a0) * (r0 + wobble0),
                    red, green, blue, alpha * aStart,
                    Mth.cos(a1) * (r1 + wobble1), y1, Mth.sin(a1) * (r1 + wobble1),
                    red, green, blue, alpha * aEnd,
                    Mth.cos(a1) * (r1 - wobble1), y1, Mth.sin(a1) * (r1 - wobble1),
                    0.78F, 0.98F, 1.0F, alpha * aEnd * 0.58F,
                    Mth.cos(a0) * (r0 - wobble0), y0, Mth.sin(a0) * (r0 - wobble0),
                    0.78F, 0.98F, 1.0F, alpha * aStart * 0.58F);
        }
    }

    private static void drawLiftedLoop(Matrix4f mat, VertexConsumer vc, float startAngle, float arc,
                                       float radiusX, float radiusZ, float width,
                                       float baseY, float lift, float alpha,
                                       float red, float green, float blue) {
        int segments = 36;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = startAngle + arc * (t0 - 0.5F);
            float a1 = startAngle + arc * (t1 - 0.5F);
            float wobble0 = width * (0.88F + 0.12F * Mth.sin(a0 * 2.3F));
            float wobble1 = width * (0.88F + 0.12F * Mth.sin(a1 * 2.3F));
            float y0 = baseY + lift * (0.46F + 0.54F * Mth.sin(a0 + 0.82F))
                    + Mth.sin(a0 * 1.7F) * 0.04F;
            float y1 = baseY + lift * (0.46F + 0.54F * Mth.sin(a1 + 0.82F))
                    + Mth.sin(a1 * 1.7F) * 0.04F;
            float aStart = alpha * edgeFade(t0);
            float aEnd = alpha * edgeFade(t1);
            quadWithUvRange(vc, mat, 0.0F, t0, t1,
                    Mth.cos(a0) * (radiusX + wobble0), y0, Mth.sin(a0) * (radiusZ + wobble0 * 0.72F),
                    red, green, blue, aStart,
                    Mth.cos(a1) * (radiusX + wobble1), y1, Mth.sin(a1) * (radiusZ + wobble1 * 0.72F),
                    red, green, blue, aEnd,
                    Mth.cos(a1) * (radiusX - wobble1), y1, Mth.sin(a1) * (radiusZ - wobble1 * 0.72F),
                    0.86F, 0.98F, 1.0F, aEnd * 0.74F,
                    Mth.cos(a0) * (radiusX - wobble0), y0, Mth.sin(a0) * (radiusZ - wobble0 * 0.72F),
                    0.86F, 0.98F, 1.0F, aStart * 0.74F);
        }
    }

    private static void drawIceFissure(Matrix4f mat, VertexConsumer vc, float age, float alpha) {
        for (int i = 0; i < 9; i++) {
            float t0 = i / 9.0F;
            float t1 = (i + 1) / 9.0F;
            float z0 = Mth.lerp(t0, 1.0F, 6.0F);
            float z1 = Mth.lerp(t1, 1.0F, 6.0F);
            float w0 = 0.18F + t0 * 0.72F + Mth.sin(age * 0.1F + i) * 0.05F;
            float w1 = 0.18F + t1 * 0.72F + Mth.sin(age * 0.1F + i + 1) * 0.05F;
            float a0 = alpha * (1.0F - t0 * 0.55F);
            float a1 = alpha * (1.0F - t1 * 0.55F);
            quadWithUvRange(vc, mat, 6.0F, t0, t1,
                    -w0, 0.055F, z0, CYAN_R, CYAN_G, CYAN_B, a0,
                    -w1, 0.055F, z1, CYAN_R, CYAN_G, CYAN_B, a1,
                    w1, 0.055F, z1, 0.88F, 1.0F, 1.0F, a1 * 0.82F,
                    w0, 0.055F, z0, 0.88F, 1.0F, 1.0F, a0 * 0.82F);
        }
    }

    private static void drawCrystal(Matrix4f mat, VertexConsumer vc, float height, float width, float alpha) {
        quadWithUv(vc, mat, 4.0F,
                -width, 0.0F, 0.0F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.45F,
                0.0F, height, 0.0F, 0.92F, 1.0F, 1.0F, alpha,
                width, 0.0F, 0.0F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.45F,
                0.0F, -0.12F, 0.0F, BLUE_R, BLUE_G, BLUE_B, 0.0F);
        quadWithUv(vc, mat, 4.0F,
                0.0F, 0.0F, -width * 0.82F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.42F,
                0.0F, height, 0.0F, 0.92F, 1.0F, 1.0F, alpha * 0.9F,
                0.0F, 0.0F, width * 0.82F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.42F,
                0.0F, -0.12F, 0.0F, BLUE_R, BLUE_G, BLUE_B, 0.0F);
    }

    private static void drawFloatingShard(Matrix4f mat, VertexConsumer vc, float size, float alpha) {
        quadWithUv(vc, mat, 4.0F,
                0.0F, size, 0.0F, 0.92F, 1.0F, 1.0F, alpha,
                size * 0.62F, 0.0F, 0.0F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.72F,
                0.0F, -size, 0.0F, BLUE_R, BLUE_G, BLUE_B, alpha * 0.45F,
                -size * 0.62F, 0.0F, 0.0F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.72F);
        quadWithUv(vc, mat, 4.0F,
                0.0F, size, 0.0F, 0.92F, 1.0F, 1.0F, alpha * 0.8F,
                0.0F, 0.0F, size * 0.62F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.58F,
                0.0F, -size, 0.0F, BLUE_R, BLUE_G, BLUE_B, alpha * 0.36F,
                0.0F, 0.0F, -size * 0.62F, CYAN_R, CYAN_G, CYAN_B, alpha * 0.58F);
    }

    private static void drawForwardRibbon(Matrix4f mat, VertexConsumer vc, float tail, float head,
                                          float startWidth, float endWidth, float y, float phase,
                                          float alpha, float red, float green, float blue) {
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float z0 = Mth.lerp(t0, tail, head);
            float z1 = Mth.lerp(t1, tail, head);
            float w0 = Mth.lerp(t0, startWidth, endWidth);
            float w1 = Mth.lerp(t1, startWidth, endWidth);
            float y0 = y + Mth.sin(phase + t0 * Mth.PI) * 0.09F;
            float y1 = y + Mth.sin(phase + t1 * Mth.PI) * 0.09F;
            float a0 = alpha * edgeFade(t0);
            float a1 = alpha * edgeFade(t1);
            quadWithUvRange(vc, mat, 2.0F, t0, t1,
                    -w0, y0, z0, red, green, blue, a0,
                    -w1, y1, z1, red, green, blue, a1,
                    w1, y1, z1, 0.92F, 1.0F, 1.0F, a1 * 0.82F,
                    w0, y0, z0, 0.92F, 1.0F, 1.0F, a0 * 0.82F);
        }
    }

    private static void drawRadialStreak(Matrix4f mat, VertexConsumer vc, float angle,
                                         float tailRadius, float headRadius, float halfWidth,
                                         float y0, float y1, float alpha,
                                         float red, float green, float blue) {
        if (useLegacyShaderFallback()) {
            drawRadialStreakFallback(mat, vc, angle, tailRadius, headRadius, halfWidth,
                    y0, y1, alpha, red, green, blue);
            return;
        }

        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);
        float tangentX = -sin * halfWidth;
        float tangentZ = cos * halfWidth;
        float tailX = cos * tailRadius;
        float tailZ = sin * tailRadius;
        float headX = cos * headRadius;
        float headZ = sin * headRadius;
        quadWithUvRange(vc, mat, 1.5F, 0.0F, 1.0F,
                tailX - tangentX * 0.52F, y0, tailZ - tangentZ * 0.52F, red, green, blue, alpha * 0.0F,
                headX - tangentX, y1, headZ - tangentZ, red, green, blue, alpha,
                headX + tangentX, y1, headZ + tangentZ, 0.92F, 1.0F, 1.0F, alpha * 0.74F,
                tailX + tangentX * 0.52F, y0, tailZ + tangentZ * 0.52F, 0.92F, 1.0F, 1.0F, alpha * 0.0F);
    }

    private static void drawRadialStreakFallback(Matrix4f mat, VertexConsumer vc, float angle,
                                                 float tailRadius, float headRadius, float halfWidth,
                                                 float y0, float y1, float alpha,
                                                 float red, float green, float blue) {
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);
        int segments = 7;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float tm = (t0 + t1) * 0.5F;
            float r0 = Mth.lerp(t0, tailRadius, headRadius);
            float r1 = Mth.lerp(t1, tailRadius, headRadius);
            float width0 = halfWidth * Mth.lerp(t0, 0.52F, 1.0F);
            float width1 = halfWidth * Mth.lerp(t1, 0.52F, 1.0F);
            float tangentX0 = -sin * width0;
            float tangentZ0 = cos * width0;
            float tangentX1 = -sin * width1;
            float tangentZ1 = cos * width1;
            float x0 = cos * r0;
            float z0 = sin * r0;
            float x1 = cos * r1;
            float z1 = sin * r1;
            float yStart = Mth.lerp(t0, y0, y1);
            float yEnd = Mth.lerp(t1, y0, y1);
            float localAlpha = alpha * smootherStep(tm) * (1.0F - tm * 0.10F);
            float localRed = Mth.lerp(tm, red, 0.92F);
            float localGreen = Mth.lerp(tm, green, 1.0F);
            quadWithUvRange(vc, mat, 1.5F, t0, t1,
                    x0 - tangentX0, yStart, z0 - tangentZ0, localRed, localGreen, blue, localAlpha,
                    x1 - tangentX1, yEnd, z1 - tangentZ1, localRed, localGreen, blue, localAlpha,
                    x1 + tangentX1, yEnd, z1 + tangentZ1, localRed, localGreen, blue, localAlpha * 0.92F,
                    x0 + tangentX0, yStart, z0 + tangentZ0, localRed, localGreen, blue, localAlpha * 0.92F);
        }
    }

    private static void drawBladeArc(Matrix4f mat, VertexConsumer vc, float z, float y,
                                     float halfWidth, float thickness, float alpha,
                                     float red, float green, float blue) {
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = Mth.lerp(t0, -1.18F, 1.18F);
            float a1 = Mth.lerp(t1, -1.18F, 1.18F);
            float x0 = Mth.sin(a0) * halfWidth;
            float x1 = Mth.sin(a1) * halfWidth;
            float z0 = z + Mth.cos(a0) * thickness;
            float z1 = z + Mth.cos(a1) * thickness;
            float y0 = y + Mth.cos(a0) * 0.18F;
            float y1 = y + Mth.cos(a1) * 0.18F;
            float aEdge0 = alpha * edgeFade(t0);
            float aEdge1 = alpha * edgeFade(t1);
            quadWithUvRange(vc, mat, 2.0F, t0, t1,
                    x0, y0 + 0.11F, z0, red, green, blue, aEdge0,
                    x1, y1 + 0.11F, z1, red, green, blue, aEdge1,
                    x1, y1 - 0.11F, z1 - 0.22F, 0.92F, 1.0F, 1.0F, aEdge1 * 0.86F,
                    x0, y0 - 0.11F, z0 - 0.22F, 0.92F, 1.0F, 1.0F, aEdge0 * 0.86F);
        }
    }

    private static void drawBrokenRing(Matrix4f mat, VertexConsumer vc, float y, float radius, float width,
                                       float scatter, float red, float green, float blue, float alpha) {
        if (useLegacyShaderFallback()) {
            drawBrokenRingBands(mat, vc, y, radius, width, scatter, red, green, blue, alpha);
            return;
        }

        int segments = 72;
        float outer = radius + width * 0.5F;
        float inner = Math.max(0.01F, radius - width * 0.5F);
        int gapRate = 7 - Mth.floor(scatter * 4.0F);
        for (int i = 0; i < segments; i++) {
            float noise = 0.5F + 0.5F * Mth.sin(i * 1.73F + scatter * 8.0F);
            if (scatter > 0.28F && ((i + Mth.floor(scatter * 9.0F)) % Math.max(2, gapRate) == 0 || noise < scatter * 0.24F)) {
                continue;
            }

            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = Mth.TWO_PI * t0;
            float a1 = Mth.TWO_PI * t1;
            float localAlpha = alpha * (1.0F - scatter * 0.56F) * (0.72F + noise * 0.28F);
            quadWithUvRange(vc, mat, 0.0F, t0, t1,
                    Mth.cos(a0) * outer, y, Mth.sin(a0) * outer, red, green, blue, localAlpha,
                    Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, red, green, blue, localAlpha,
                    Mth.cos(a1) * inner, y, Mth.sin(a1) * inner, 0.92F, 1.0F, 1.0F, localAlpha * 0.48F,
                    Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, 0.92F, 1.0F, 1.0F, localAlpha * 0.48F);
        }
    }

    private static void drawRing(Matrix4f mat, VertexConsumer vc, float y, float radius, float width,
                                 float red, float green, float blue, float alpha) {
        if (useLegacyShaderFallback()) {
            drawRingBands(mat, vc, y, radius, width, red, green, blue, alpha);
            return;
        }

        int segments = 80;
        float outer = radius + width * 0.5F;
        float inner = Math.max(0.01F, radius - width * 0.5F);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            quadWithUvRange(vc, mat, 0.0F, t0, t1,
                    Mth.cos(a0) * outer, y, Mth.sin(a0) * outer, red, green, blue, alpha,
                    Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, red, green, blue, alpha,
                    Mth.cos(a1) * inner, y, Mth.sin(a1) * inner, 0.92F, 1.0F, 1.0F, alpha * 0.46F,
                    Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, 0.92F, 1.0F, 1.0F, alpha * 0.46F);
        }
    }

    private static void drawRingBands(Matrix4f mat, VertexConsumer vc, float y, float radius, float width,
                                      float red, float green, float blue, float alpha) {
        int segments = Mth.clamp(Mth.ceil(radius * 10.0F), 64, 112);
        int bands = Mth.clamp(Mth.ceil(width / 0.22F), 3, 10);
        float inner = Math.max(0.01F, radius - width * 0.5F);
        for (int band = 0; band < bands; band++) {
            float p0 = band / (float) bands;
            float p1 = (band + 1) / (float) bands;
            float pm = (p0 + p1) * 0.5F;
            float r0 = inner + width * p0;
            float r1 = inner + width * p1;
            float edge = Mth.sin(pm * Mth.PI);
            float bandAlpha = alpha * (0.16F + edge * 0.84F);
            float bandRed = Mth.lerp(pm, 0.88F, red);
            float bandGreen = Mth.lerp(pm, 0.96F, green);

            for (int i = 0; i < segments; i++) {
                float a0 = Mth.TWO_PI * i / segments;
                float a1 = Mth.TWO_PI * (i + 1) / segments;
                float t0 = i / (float) segments;
                float t1 = (i + 1) / (float) segments;
                quadWithUvRange(vc, mat, 0.0F, t0, t1,
                        Mth.cos(a0) * r1, y, Mth.sin(a0) * r1, bandRed, bandGreen, blue, bandAlpha,
                        Mth.cos(a1) * r1, y, Mth.sin(a1) * r1, bandRed, bandGreen, blue, bandAlpha,
                        Mth.cos(a1) * r0, y, Mth.sin(a1) * r0, bandRed, bandGreen, blue, bandAlpha,
                        Mth.cos(a0) * r0, y, Mth.sin(a0) * r0, bandRed, bandGreen, blue, bandAlpha);
            }
        }
    }

    private static void drawBrokenRingBands(Matrix4f mat, VertexConsumer vc, float y, float radius, float width,
                                            float scatter, float red, float green, float blue, float alpha) {
        int segments = Mth.clamp(Mth.ceil(radius * 9.0F), 64, 112);
        int bands = Mth.clamp(Mth.ceil(width / 0.24F), 3, 9);
        float inner = Math.max(0.01F, radius - width * 0.5F);
        int gapRate = 7 - Mth.floor(scatter * 4.0F);
        for (int i = 0; i < segments; i++) {
            float noise = 0.5F + 0.5F * Mth.sin(i * 1.73F + scatter * 8.0F);
            if (scatter > 0.28F && ((i + Mth.floor(scatter * 9.0F)) % Math.max(2, gapRate) == 0 || noise < scatter * 0.24F)) {
                continue;
            }

            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = Mth.TWO_PI * t0;
            float a1 = Mth.TWO_PI * t1;
            float localAlpha = alpha * (1.0F - scatter * 0.46F) * (0.72F + noise * 0.28F);
            for (int band = 0; band < bands; band++) {
                float p0 = band / (float) bands;
                float p1 = (band + 1) / (float) bands;
                float pm = (p0 + p1) * 0.5F;
                float r0 = inner + width * p0;
                float r1 = inner + width * p1;
                float edge = Mth.sin(pm * Mth.PI);
                float bandAlpha = localAlpha * (0.18F + edge * 0.82F);
                float bandRed = Mth.lerp(pm, 0.88F, red);
                float bandGreen = Mth.lerp(pm, 0.96F, green);
                quadWithUvRange(vc, mat, 0.0F, t0, t1,
                        Mth.cos(a0) * r1, y, Mth.sin(a0) * r1, bandRed, bandGreen, blue, bandAlpha,
                        Mth.cos(a1) * r1, y, Mth.sin(a1) * r1, bandRed, bandGreen, blue, bandAlpha,
                        Mth.cos(a1) * r0, y, Mth.sin(a1) * r0, bandRed, bandGreen, blue, bandAlpha,
                        Mth.cos(a0) * r0, y, Mth.sin(a0) * r0, bandRed, bandGreen, blue, bandAlpha);
            }
        }
    }

    private static float stage(float age, float start, float duration) {
        return Mth.clamp((age - start) / duration, 0.0F, 1.0F);
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float dampedSnap(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float value = 1.0F - (float) (Math.exp(-5.5D * t) * Math.cos(10.2D * t));
        return Mth.clamp(value, 0.0F, 1.16F);
    }

    private static float fastOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv * inv;
    }

    private static float exitBurstEnvelope(float age) {
        return smootherStep(stage(age, 38.4F, 1.0F))
                * (1.0F - smootherStep(stage(age, 42.8F, 3.4F)));
    }

    private static float fadeDuringExpansion(float age, float start, float expandDuration,
                                             float leadTicks, float fadeDuration) {
        return 1.0F - smootherStep(stage(age, start + expandDuration - leadTicks, fadeDuration));
    }

    private static float edgeFade(float t) {
        float head = smootherStep(Mth.clamp(t / 0.16F, 0.0F, 1.0F));
        float tail = 1.0F - smootherStep(Mth.clamp((t - 0.84F) / 0.16F, 0.0F, 1.0F));
        return head * tail;
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                             float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                             float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                             float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        quadWithUv(vc, mat, 0.0F,
                x0, y0, z0, r0, g0, b0, a0,
                x1, y1, z1, r1, g1, b1, a1,
                x2, y2, z2, r2, g2, b2, a2,
                x3, y3, z3, r3, g3, b3, a3);
    }

    private static void quadWithUv(VertexConsumer vc, Matrix4f mat, float uvBase,
                                   float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                                   float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                                   float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                                   float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        if (useLegacyShaderFallback()) {
            fallbackQuad(vc, mat,
                    x0, y0, z0, r0, g0, b0, a0,
                    x1, y1, z1, r1, g1, b1, a1,
                    x2, y2, z2, r2, g2, b2, a2,
                    x3, y3, z3, r3, g3, b3, a3);
            return;
        }

        vertex(vc, mat, x0, y0, z0, r0, g0, b0, a0, uvBase, 0.0F);
        vertex(vc, mat, x1, y1, z1, r1, g1, b1, a1, uvBase + 1.0F, 0.0F);
        vertex(vc, mat, x2, y2, z2, r2, g2, b2, a2, uvBase + 1.0F, 1.0F);
        vertex(vc, mat, x3, y3, z3, r3, g3, b3, a3, uvBase, 1.0F);
    }

    private static void quadWithUvRange(VertexConsumer vc, Matrix4f mat, float uvBase, float u0, float u1,
                                        float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                                        float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                                        float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                                        float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        if (useLegacyShaderFallback()) {
            fallbackQuad(vc, mat,
                    x0, y0, z0, r0, g0, b0, a0,
                    x1, y1, z1, r1, g1, b1, a1,
                    x2, y2, z2, r2, g2, b2, a2,
                    x3, y3, z3, r3, g3, b3, a3);
            return;
        }

        vertex(vc, mat, x0, y0, z0, r0, g0, b0, a0, uvBase + u0, 0.0F);
        vertex(vc, mat, x1, y1, z1, r1, g1, b1, a1, uvBase + u1, 0.0F);
        vertex(vc, mat, x2, y2, z2, r2, g2, b2, a2, uvBase + u1, 1.0F);
        vertex(vc, mat, x3, y3, z3, r3, g3, b3, a3, uvBase + u0, 1.0F);
    }

    private static void fallbackQuad(VertexConsumer vc, Matrix4f mat,
                                     float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                                     float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                                     float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                                     float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        float alpha = (a0 + a1 + a2 + a3) * 0.25F;
        if (alpha <= 0.001F) {
            return;
        }

        float weight = Math.max(0.001F, a0 + a1 + a2 + a3);
        float red = (r0 * a0 + r1 * a1 + r2 * a2 + r3 * a3) / weight;
        float green = (g0 * a0 + g1 * a1 + g2 * a2 + g3 * a3) / weight;
        float blue = (b0 * a0 + b1 * a1 + b2 * a2 + b3 * a3) / weight;
        fallbackVertex(vc, mat, x0, y0, z0, red, green, blue, alpha);
        fallbackVertex(vc, mat, x1, y1, z1, red, green, blue, alpha);
        fallbackVertex(vc, mat, x2, y2, z2, red, green, blue, alpha);
        fallbackVertex(vc, mat, x3, y3, z3, red, green, blue, alpha);
    }

    private static void fallbackVertex(VertexConsumer vc, Matrix4f mat,
                                       float x, float y, float z,
                                       float red, float green, float blue, float alpha) {
        vc.vertex(mat, x, y, z)
                .color(red, green, blue, Mth.clamp(alpha * 1.32F, 0.0F, 1.0F))
                .endVertex();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha,
                               float u, float v) {
        float safeAlpha = useLegacyShaderFallback()
                ? Mth.clamp(alpha * 1.32F, 0.0F, 1.0F)
                : alpha;
        if (useLegacyShaderFallback()) {
            fallbackVertex(vc, mat, x, y, z, red, green, blue, alpha);
            return;
        }

        vc.vertex(mat, x, y, z)
                .color(red, green, blue, safeAlpha)
                .uv(u, v)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(UtpalaAuraEntity entity) {
        return TEXTURE;
    }
}

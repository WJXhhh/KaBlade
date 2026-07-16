package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.BloodfyreFrenzyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Layered client renderer for Ruinous Sakura's Bloodfyre Frenzy. */
public final class BloodfyreFrenzyRenderer extends EntityRenderer<BloodfyreFrenzyEntity> {
    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/empty.png");
    private static final ResourceLocation PETAL_MODEL =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "effects/rain/petal.obj");

    private static final int ARC_SEGMENTS = 160;
    private static final int SMOKE_COUNT = 18;
    private static final int FRAGMENT_COUNT = 30;
    private static final int PETAL_COUNT = 42;
    private static final int EMBER_COUNT = 34;
    private static final float SCAR_RADIUS = 4.34F;
    // Only the hot emitters feed bloom. Smoke, char and the dark scar stay in the color pass.
    private static final float OCULUS_MAIN_BLOOM_ALPHA = 0.68F;
    private static final float OCULUS_RUPTURE_BLOOM_ALPHA = 0.64F;
    private static final float[] RUPTURE_ANCHORS = {
            0.05F, 0.13F, 0.22F, 0.31F, 0.41F, 0.52F, 0.63F, 0.73F,
            0.81F, 0.87F, 0.91F, 0.945F, 0.972F
    };
    private static final BranchSpec[] BRANCHES = {
            new BranchSpec(0.09F, -1.0F, 0.52F), new BranchSpec(0.18F, 1.0F, 0.66F),
            new BranchSpec(0.29F, -1.0F, 0.48F), new BranchSpec(0.41F, 1.0F, 0.72F),
            new BranchSpec(0.56F, -1.0F, 0.58F), new BranchSpec(0.69F, 1.0F, 0.78F),
            new BranchSpec(0.79F, -1.0F, 0.70F), new BranchSpec(0.86F, 1.0F, 0.92F),
            new BranchSpec(0.91F, -1.0F, 0.74F), new BranchSpec(0.955F, 1.0F, 1.08F)
    };

    private static final Map<BloodfyreFrenzyEntity, GroundTrace> GROUND_TRACES = new WeakHashMap<>();
    private static PetalMesh petalMesh;

    public BloodfyreFrenzyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(BloodfyreFrenzyEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // The timeline anchor is tiny, while the visible slash reaches roughly five blocks
        // from it and rises through the smoke/rupture layer. Cull against the presentation.
        return frustum.isVisible(entity.getBoundingBox().inflate(6.25D, 4.5D, 6.25D));
    }

    @Override
    public void render(BloodfyreFrenzyEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.getRenderAge(partialTick);
        if (age >= BloodfyreFrenzyEntity.LIFETIME) {
            return;
        }

        poseStack.pushPose();
        // Minecraft's renderer faces this local effect opposite to the HTML +Z convention.
        // Rotate the complete presentation so the slash occupies the caster's forward side.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        Matrix4f matrix = poseStack.last().pose();
        int seed = entity.getId() * 31 + 0x5F3759DF;
        boolean shaderFallback = KabladeRenderTypes.useShaderFallbackTextures();

        if (shaderFallback) {
            if (BloodfyreOculusPipeline.enqueue(entity, partialTick)) {
                poseStack.popPose();
                super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
                return;
            }
            renderShaderPackFallback(entity, buffer, matrix, age, seed);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        // Custom RenderTypes share the immediate buffer builder. Acquire each consumer only
        // when its pass is about to emit vertices; retaining several consumers aliases the
        // same builder and causes bright geometry to be submitted with a later dark shader.
        renderGuide(buffer.getBuffer(KabladeRenderTypes.bloodfyreBlade()), matrix, age, seed);
        renderMainSlash(buffer.getBuffer(KabladeRenderTypes.bloodfyreBladeDark()),
                matrix, age, seed, false);
        renderRupture(buffer.getBuffer(KabladeRenderTypes.bloodfyreRupture()),
                matrix, age, seed, true, false, false);
        renderSmoke(buffer.getBuffer(KabladeRenderTypes.bloodfyreSmoke()), matrix, age, seed);
        renderFragments(buffer.getBuffer(KabladeRenderTypes.bloodfyreParticle()), matrix, age, seed);
        renderGroundScar(entity, buffer, matrix, age, seed, 1.0F, false);
        renderEmbers(buffer.getBuffer(KabladeRenderTypes.bloodfyreParticle()), matrix, age, seed);
        renderPetals(poseStack, buffer.getBuffer(KabladeRenderTypes.bloodfyreParticle()), age, seed);

        renderVanillaGlowLayers(entity, poseStack, buffer, matrix, age, seed);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Conservative shader-pack renderer. It intentionally uses one stock translucent
     * buffer and only planar, non-overlapping quads. None of the analytic flame/smoke
     * sheets are submitted because shader packs cannot reproduce their fragment discard.
     */
    static void renderShaderPackFallback(BloodfyreFrenzyEntity entity,
                                         MultiBufferSource buffer, Matrix4f matrix,
                                         float age, int seed) {
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.bloodfyreShaderPackFallback());
        renderShaderFallbackGuide(vc, matrix, age);
        renderShaderFallbackBlade(vc, matrix, age, seed);
        renderShaderFallbackBurst(vc, matrix, age, seed);
        renderShaderFallbackScar(entity, vc, matrix, age, seed);

        VertexConsumer glow = buffer.getBuffer(KabladeRenderTypes.bloodfyreShaderPackGlow());
        renderShaderFallbackGlow(entity, glow, matrix, age, seed);
    }

    /** Full-detail color pass used by the frame-level Oculus renderer. */
    static void renderOculusColor(BloodfyreFrenzyEntity entity, PoseStack poseStack,
                                  BloodfyreOculusPipeline.DrawContext context,
                                  float age, int seed) {
        Matrix4f matrix = poseStack.last().pose();
        context.draw(BloodfyreOculusPipeline.AnalyticShader.FRENZY,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderGuide(vc, matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.FRENZY,
                BloodfyreOculusPipeline.BlendMode.ALPHA,
                vc -> renderMainSlash(vc, matrix, age, seed, false));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.RUPTURE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderRupture(vc, matrix, age, seed, true, false, false));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.SMOKE,
                BloodfyreOculusPipeline.BlendMode.ALPHA,
                vc -> renderSmoke(vc, matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.PARTICLE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderFragments(vc, matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.SCAR,
                BloodfyreOculusPipeline.BlendMode.ALPHA,
                vc -> renderGroundScarBase(entity, vc, matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.SCAR,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderGroundScarHeat(entity, vc, matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.PARTICLE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderEmbers(vc, matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.PARTICLE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderPetals(poseStack, vc, age, seed));
    }

    /** Selected emissive layers; rendered into a separate HDR mask for controlled bloom. */
    static void renderOculusGlow(BloodfyreFrenzyEntity entity, PoseStack poseStack,
                                 BloodfyreOculusPipeline.DrawContext context,
                                 float age, int seed) {
        Matrix4f matrix = poseStack.last().pose();
        float intensity = BloodfyreTimeline.glowIntensity(age);
        context.draw(BloodfyreOculusPipeline.AnalyticShader.FRENZY,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderGuide(scaled(vc, 0.34F * intensity), matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.FRENZY,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderMainGlowMask(scaled(vc, OCULUS_MAIN_BLOOM_ALPHA * intensity),
                        matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.RUPTURE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderRupture(scaled(vc, OCULUS_RUPTURE_BLOOM_ALPHA * intensity),
                        matrix, age, seed, true, false, true));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.PARTICLE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderFragments(scaled(vc, 0.32F * intensity), matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.SCAR,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderGroundScarHeat(entity, scaled(vc, 0.38F * intensity), matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.PARTICLE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderEmbers(scaled(vc, 0.42F * intensity), matrix, age, seed));
        context.draw(BloodfyreOculusPipeline.AnalyticShader.PARTICLE,
                BloodfyreOculusPipeline.BlendMode.ADDITIVE,
                vc -> renderPetals(poseStack, scaled(vc, 0.18F * intensity), age, seed));
    }

    /** Stock-renderer approximation of bloom: broad red shell, orange middle, white-hot core. */
    private static void renderVanillaGlowLayers(BloodfyreFrenzyEntity entity, PoseStack poseStack,
                                                MultiBufferSource buffer, Matrix4f matrix,
                                                float age, int seed) {
        VertexConsumer glow = buffer.getBuffer(KabladeRenderTypes.bloodfyreVanillaGlow());
        float intensity = BloodfyreTimeline.glowIntensity(age);

        renderGuide(scaled(glow, 0.44F * intensity), matrix, age, seed);
        renderVanillaMainHalo(glow, matrix, age, seed, intensity);
        renderMainGlowMask(scaled(glow, 0.80F * intensity), matrix, age, seed);
        renderRupture(scaled(glow, 0.72F * intensity), matrix, age, seed,
                true, false, true);
        renderFragments(scaled(glow, 0.45F * intensity), matrix, age, seed);
        renderGroundScarVanillaGlow(entity, scaled(glow, 0.48F * intensity), matrix, age, seed);
        renderEmbers(scaled(glow, 0.54F * intensity), matrix, age, seed);
        renderPetals(poseStack, scaled(glow, 0.20F * intensity), age, seed);
    }

    private static void renderVanillaMainHalo(VertexConsumer glow, Matrix4f matrix,
                                              float age, int seed, float intensity) {
        float alpha = BloodfyreTimeline.mainAlpha(age);
        float head = BloodfyreTimeline.slashProgress(age);
        if (alpha <= 0.002F || head <= 0.001F) {
            return;
        }
        float tail = 0.12F * BloodfyreTimeline.smooth((age - 24.0F) / 8.0F);
        drawArcBand(glow, matrix, tail, head, 3.08F, 5.04F, 0.050F,
                alpha * 0.14F * intensity, 0.82F, 0.006F, 0.020F,
                3, seed + 1201, 0.0F);
        drawArcBand(glow, matrix, tail, head, 3.26F, 4.94F, 0.060F,
                alpha * 0.23F * intensity, 1.0F, 0.12F, 0.018F,
                3, seed + 1223, 0.0F);
        drawArcBand(glow, matrix, tail, head, 3.48F, 4.88F, 0.072F,
                alpha * 0.20F * intensity, 1.0F, 0.54F, 0.10F,
                3, seed + 1249, 0.0F);
    }

    /** Bright blade surface and filaments only; excludes the black/red body and erosion char. */
    private static void renderMainGlowMask(VertexConsumer glow, Matrix4f matrix,
                                           float age, int seed) {
        float alpha = BloodfyreTimeline.mainAlpha(age);
        float head = BloodfyreTimeline.slashProgress(age);
        if (alpha <= 0.002F || head <= 0.001F) {
            return;
        }

        float impactAge = Math.max(0.0F, age - BloodfyreTimeline.SLASH_END);
        float overshoot = age < BloodfyreTimeline.SLASH_END ? 0.0F
                : 0.046F * (float) Math.exp(-impactAge * 1.22F)
                * Mth.sin(impactAge * 3.45F + 0.72F);
        float movingHead = Mth.clamp(head + overshoot, 0.0F, 1.0F);
        drawErodingBrightBand(glow, matrix, age, 0.0F, movingHead,
                3.42F, 4.84F, 0.074F, alpha, seed + 163);

        for (int i = 0; i < 4; i++) {
            float filamentTail = head < 0.995F ? Math.max(0.0F, head - (0.30F + i * 0.045F))
                    : 0.055F * BloodfyreTimeline.smooth((age - 18.0F - i * 0.35F) / 8.0F);
            float filamentHead = head < 0.995F ? Math.max(0.0F, head - i * 0.018F)
                    : 1.0F - 0.035F * BloodfyreTimeline.smooth((age - 18.0F - i * 0.25F) / 7.0F);
            float fade = 1.0F - BloodfyreTimeline.smooth((age - 24.0F - i * 0.6F) / 5.5F);
            float radius = 4.20F + i * 0.135F;
            drawArcBand(glow, matrix, filamentTail, filamentHead,
                    radius - 0.016F, radius + 0.016F, 0.096F + i * 0.01F,
                    alpha * fade * 0.92F, 1.0F, 0.80F, 0.32F,
                    3, seed + 211 + i * 19, 0.0F);
        }

        float pulseDistance = (age - 13.2F) / 0.78F;
        float pulse = (float) Math.exp(-pulseDistance * pulseDistance);
        float headFade = 1.0F - BloodfyreTimeline.smooth((age - 14.2F) / 2.6F);
        if (headFade > 0.002F) {
            BloodfyreGeometry.Point center = BloodfyreGeometry.arc(movingHead, 4.58F, 0.10F);
            BloodfyreGeometry.Point tangent = BloodfyreGeometry.tangent(movingHead, 4.58F);
            drawJetSheet(glow, matrix, center, tangent, 1.30F + pulse * 0.65F,
                    0.22F + pulse * 0.16F, alpha * headFade,
                    1.0F, 0.96F, 0.70F, 2);
        }
    }

    /** Shader-pack-safe bright pass using explicit POSITION_COLOR triangles only. */
    private static void renderShaderFallbackGlow(BloodfyreFrenzyEntity entity, VertexConsumer glow,
                                                 Matrix4f matrix, float age, int seed) {
        float intensity = BloodfyreTimeline.glowIntensity(age);
        float guideAlpha = BloodfyreTimeline.window(age, BloodfyreTimeline.GUIDE_START,
                BloodfyreTimeline.GUIDE_END, 0.65F, 0.8F);
        if (guideAlpha > 0.002F) {
            float progress = BloodfyreTimeline.smooth((age - BloodfyreTimeline.GUIDE_START) / 3.0F);
            float radius = Mth.lerp(progress, 1.72F, 4.04F);
            float span = Mth.lerp(progress, 0.20F, 1.0F);
            float start = (1.0F - span) * 0.5F;
            float end = 1.0F - start;
            drawSimpleArcBand(glow, matrix, start, end, radius - 0.065F, radius + 0.065F,
                    0.058F, 1.0F, 0.12F, 0.025F,
                    guideAlpha * 0.22F * intensity, 0);
            drawSimpleArcBand(glow, matrix, start, end, radius - 0.016F, radius + 0.016F,
                    0.072F, 1.0F, 0.94F, 0.68F,
                    guideAlpha * 0.70F * intensity, 0);
        }

        float mainAlpha = BloodfyreTimeline.mainAlpha(age);
        float head = BloodfyreTimeline.slashProgress(age);
        if (mainAlpha > 0.002F && head > 0.001F) {
            float tail = 0.12F * BloodfyreTimeline.smooth((age - 24.0F) / 8.0F);
            drawSimpleArcBand(glow, matrix, tail, head, 3.05F, 5.04F, 0.052F,
                    0.86F, 0.004F, 0.018F, mainAlpha * 0.13F * intensity, 0);
            drawSimpleArcBand(glow, matrix, tail, head, 3.26F, 4.95F, 0.064F,
                    1.0F, 0.16F, 0.022F, mainAlpha * 0.22F * intensity, 0);
            drawSimpleArcBand(glow, matrix, tail, head, 4.48F, 4.88F, 0.078F,
                    1.0F, 0.95F, 0.72F, mainAlpha * 0.72F * intensity, 0);
        }

        renderShaderFallbackBurst(scaled(glow, 0.72F * intensity), matrix, age, seed);
        float flash = BloodfyreTimeline.window(age, 11.2F, 15.8F, 0.22F, 2.6F);
        if (flash > 0.002F) {
            BloodfyreGeometry.Point endpoint = BloodfyreGeometry.arc(0.975F, 4.62F, 0.55F);
            fallbackDiamond(glow, matrix, endpoint, 1.28F + flash * 1.20F,
                    age * 0.08F, 0.95F, 0.015F, 0.025F, flash * 0.20F * intensity);
            fallbackDiamond(glow, matrix, endpoint, 0.42F + flash * 0.58F,
                    -age * 0.13F, 1.0F, 0.94F, 0.72F, flash * 0.88F * intensity);
        }

        float scarAlpha = BloodfyreTimeline.scarAlpha(age);
        if (scarAlpha > 0.002F) {
            GroundTrace trace = groundTrace(entity);
            float scarHead = BloodfyreTimeline.slashProgress(age - 0.10F);
            float decay = BloodfyreTimeline.smooth((age - 43.0F) / 9.0F);
            float heat = 1.0F - BloodfyreTimeline.smooth((age - 24.0F) / 9.0F);
            drawFallbackGroundArc(glow, matrix, trace, scarHead, 0.105F,
                    scarAlpha * (0.16F + heat * 0.22F) * intensity,
                    0.92F, 0.015F, 0.020F, seed + 607, decay * 0.82F, 0.016F);
            drawFallbackGroundArc(glow, matrix, trace, scarHead, 0.026F,
                    scarAlpha * heat * 0.76F * intensity,
                    1.0F, 0.78F, 0.40F, seed + 613, decay, 0.022F);
        }

    }

    private static void renderShaderFallbackGuide(VertexConsumer vc, Matrix4f matrix, float age) {
        float alpha = BloodfyreTimeline.window(age, BloodfyreTimeline.GUIDE_START,
                BloodfyreTimeline.GUIDE_END, 0.65F, 0.8F);
        if (alpha <= 0.002F) {
            return;
        }
        float progress = BloodfyreTimeline.smooth((age - BloodfyreTimeline.GUIDE_START) / 3.0F);
        float radius = Mth.lerp(progress, 1.72F, 4.04F);
        float span = Mth.lerp(progress, 0.20F, 1.0F);
        drawSimpleArcBand(vc, matrix, (1.0F - span) * 0.5F, (1.0F + span) * 0.5F,
                radius - 0.018F, radius + 0.018F, 0.045F,
                1.0F, 0.34F, 0.055F, alpha * 0.48F, 3);
    }

    private static void renderShaderFallbackBlade(VertexConsumer vc, Matrix4f matrix,
                                                   float age, int seed) {
        float alpha = BloodfyreTimeline.mainAlpha(age);
        float head = BloodfyreTimeline.slashProgress(age);
        if (alpha <= 0.002F || head <= 0.001F) {
            return;
        }

        int visible = Mth.clamp(Mth.ceil(head * ARC_SEGMENTS), 0, ARC_SEGMENTS);
        for (int i = 0; i < visible; i++) {
            float u0 = i / (float) ARC_SEGMENTS;
            float u1 = Math.min(head, (i + 1) / (float) ARC_SEGMENTS);
            if (u1 <= u0) {
                continue;
            }
            float localAge0 = age - BloodfyreTimeline.slashBirth(u0);
            float localAge1 = age - BloodfyreTimeline.slashBirth(u1);
            float bright0 = brightVisibility(localAge0);
            float bright1 = brightVisibility(localAge1);
            float boundary0 = Mth.lerp(brightBoundary(age, u0, localAge0, seed), 3.44F, 4.62F);
            float boundary1 = Mth.lerp(brightBoundary(age, u1, localAge1, seed), 3.44F, 4.62F);

            // The two radial regions meet at the boundary but never overlap, avoiding
            // shader-pack z-fighting and triangle-level bloom discontinuities.
            fallbackArcCell(vc, matrix, u0, u1, 3.12F, 3.12F, boundary0, boundary1,
                    0.040F, 0.34F, 0.006F, 0.018F, alpha * 0.72F, 0);
            if (Math.max(bright0, bright1) > 0.002F) {
                fallbackArcCell(vc, matrix, u0, u1, boundary0, boundary1, 4.66F, 4.66F,
                        0.040F, 1.0F, 0.43F, 0.075F,
                        alpha * Math.max(bright0, bright1) * 0.58F, 2);
            }
        }
    }

    private static void renderShaderFallbackBurst(VertexConsumer vc, Matrix4f matrix,
                                                   float age, int seed) {
        float alpha = BloodfyreTimeline.window(age, BloodfyreTimeline.RUPTURE_START,
                BloodfyreTimeline.RUPTURE_END, 0.55F, 5.5F);
        if (alpha <= 0.002F) {
            return;
        }
        // Small detached diamonds communicate the rupture without long sheets or
        // degenerate tips. Their maximum extent is bounded regardless of lifetime.
        for (int i = 0; i < 14; i++) {
            float u = 0.08F + i / 13.0F * 0.86F;
            float birth = BloodfyreTimeline.slashBirth(u) + 0.38F + (i % 3) * 0.10F;
            float localAge = age - birth;
            if (localAge <= 0.0F || localAge >= 7.0F) {
                continue;
            }
            float fade = 1.0F - BloodfyreTimeline.smooth((localAge - 3.0F) / 4.0F);
            float random = BloodfyreGeometry.deterministic(seed + i * 47, 3.8F);
            BloodfyreGeometry.Point center = BloodfyreGeometry.arc(u,
                    4.70F + Math.min(localAge * (0.045F + random * 0.025F), 0.42F),
                    0.10F + Math.min(localAge * (0.055F + random * 0.025F), 0.52F));
            fallbackDiamond(vc, matrix, center, 0.035F + random * 0.025F,
                    u * 8.0F + localAge * 0.55F,
                    0.92F, 0.055F, 0.018F, alpha * fade * 0.42F);
        }
    }

    private static void renderShaderFallbackScar(BloodfyreFrenzyEntity entity, VertexConsumer vc,
                                                  Matrix4f matrix, float age, int seed) {
        float alpha = BloodfyreTimeline.scarAlpha(age);
        if (alpha <= 0.002F) {
            return;
        }
        GroundTrace trace = groundTrace(entity);
        float head = BloodfyreTimeline.slashProgress(age - 0.10F);
        float decay = BloodfyreTimeline.smooth((age - 43.0F) / 9.0F);
        drawFallbackGroundArc(vc, matrix, trace, head, 0.040F, alpha * 0.52F,
                0.38F, 0.006F, 0.016F, seed + 607, decay * 0.88F, 0.022F);
    }

    private static void fallbackArcCell(VertexConsumer vc, Matrix4f matrix, float u0, float u1,
                                        float inner0, float inner1, float outer0, float outer1,
                                        float y, float red, float green, float blue,
                                        float alpha, int kind) {
        fallbackQuad(vc, matrix,
                BloodfyreGeometry.arc(u0, outer0, y), u0, 1.0F,
                BloodfyreGeometry.arc(u1, outer1, y), u1, 1.0F,
                BloodfyreGeometry.arc(u1, inner1, y), u1, 0.0F,
                BloodfyreGeometry.arc(u0, inner0, y), u0, 0.0F,
                red, green, blue, alpha);
    }

    private static void drawSimpleArcBand(VertexConsumer vc, Matrix4f matrix,
                                          float fromU, float toU,
                                          float innerRadius, float outerRadius, float y,
                                          float red, float green, float blue,
                                          float alpha, int kind) {
        int start = Mth.clamp(Mth.floor(fromU * ARC_SEGMENTS), 0, ARC_SEGMENTS - 1);
        int end = Mth.clamp(Mth.ceil(toU * ARC_SEGMENTS), 1, ARC_SEGMENTS);
        for (int i = start; i < end; i++) {
            float u0 = Math.max(fromU, i / (float) ARC_SEGMENTS);
            float u1 = Math.min(toU, (i + 1) / (float) ARC_SEGMENTS);
            fallbackArcCell(vc, matrix, u0, u1,
                    innerRadius, innerRadius, outerRadius, outerRadius,
                    y, red, green, blue, alpha, kind);
        }
    }

    private static void drawFallbackGroundArc(VertexConsumer vc, Matrix4f matrix,
                                              GroundTrace trace, float head,
                                              float halfWidth, float alpha,
                                              float red, float green, float blue,
                                              int seed, float erosion, float lift) {
        int visible = Mth.clamp(Mth.ceil(head * ARC_SEGMENTS), 0, ARC_SEGMENTS);
        for (int i = 0; i < visible; i++) {
            float grain = BloodfyreGeometry.deterministic(seed + i * 41, 5.3F);
            if (grain < erosion * 0.74F) {
                continue;
            }
            float u0 = i / (float) ARC_SEGMENTS;
            float u1 = (i + 1) / (float) ARC_SEGMENTS;
            BloodfyreGeometry.Point inner0 = withY(
                    BloodfyreGeometry.groundArc(u0, SCAR_RADIUS - halfWidth),
                    trace.mainHeights[i] + lift);
            BloodfyreGeometry.Point outer0 = withY(
                    BloodfyreGeometry.groundArc(u0, SCAR_RADIUS + halfWidth),
                    trace.mainHeights[i] + lift);
            BloodfyreGeometry.Point inner1 = withY(
                    BloodfyreGeometry.groundArc(u1, SCAR_RADIUS - halfWidth),
                    trace.mainHeights[i + 1] + lift);
            BloodfyreGeometry.Point outer1 = withY(
                    BloodfyreGeometry.groundArc(u1, SCAR_RADIUS + halfWidth),
                    trace.mainHeights[i + 1] + lift);
            fallbackQuad(vc, matrix, outer0, 0.0F, 0.0F, outer1, 0.0F, 0.0F,
                    inner1, 0.0F, 0.0F, inner0, 0.0F, 0.0F,
                    red, green, blue, alpha);
        }
    }

    private static void fallbackDiamond(VertexConsumer vc, Matrix4f matrix,
                                        BloodfyreGeometry.Point center, float size, float rotation,
                                        float red, float green, float blue, float alpha) {
        float cx = Mth.cos(rotation) * size;
        float cz = Mth.sin(rotation) * size;
        float sx = -Mth.sin(rotation) * size * 0.42F;
        float sz = Mth.cos(rotation) * size * 0.42F;
        fallbackQuad(vc, matrix,
                new BloodfyreGeometry.Point(center.x() - cx, center.y(), center.z() - cz), 0.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x() + sx, center.y() + size * 0.52F, center.z() + sz), 0.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x() + cx, center.y(), center.z() + cz), 0.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x() - sx, center.y() - size * 0.52F, center.z() - sz), 0.0F, 0.0F,
                red, green, blue, alpha);
    }

    private static void fallbackQuad(VertexConsumer vc, Matrix4f matrix,
                                     BloodfyreGeometry.Point p0, float ignoredU0, float ignoredV0,
                                     BloodfyreGeometry.Point p1, float ignoredU1, float ignoredV1,
                                     BloodfyreGeometry.Point p2, float ignoredU2, float ignoredV2,
                                     BloodfyreGeometry.Point p3, float ignoredU3, float ignoredV3,
                                     float red, float green, float blue, float alpha) {
        // Explicitly emit two triangles with identical attributes. This bypasses the
        // shader pack's QUADS triangulation, the source of the yellow diagonal seams.
        fallbackTriangle(vc, matrix, p0, p1, p2, red, green, blue, alpha);
        fallbackTriangle(vc, matrix, p0, p2, p3, red, green, blue, alpha);
    }

    private static void fallbackTriangle(VertexConsumer vc, Matrix4f matrix,
                                         BloodfyreGeometry.Point p0,
                                         BloodfyreGeometry.Point p1,
                                         BloodfyreGeometry.Point p2,
                                         float red, float green, float blue, float alpha) {
        fallbackVertex(vc, matrix, p0, red, green, blue, alpha);
        fallbackVertex(vc, matrix, p1, red, green, blue, alpha);
        fallbackVertex(vc, matrix, p2, red, green, blue, alpha);
    }

    private static void fallbackVertex(VertexConsumer vc, Matrix4f matrix,
                                       BloodfyreGeometry.Point point,
                                       float red, float green, float blue, float alpha) {
        vc.vertex(matrix, point.x(), point.y(), point.z())
                .color(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F))
                .endVertex();
    }

    private static void renderGuide(VertexConsumer vc, Matrix4f matrix, float age, int seed) {
        float alpha = BloodfyreTimeline.window(age, BloodfyreTimeline.GUIDE_START,
                BloodfyreTimeline.GUIDE_END, 0.65F, 0.8F);
        if (alpha <= 0.002F) {
            return;
        }
        float progress = BloodfyreTimeline.smooth((age - BloodfyreTimeline.GUIDE_START) / 3.0F);
        float radius = Mth.lerp(progress, 1.72F, 4.04F);
        float span = Mth.lerp(progress, 0.20F, 1.0F);
        float start = (1.0F - span) * 0.5F;
        float end = 1.0F - start;
        drawArcBand(vc, matrix, start, end, radius - 0.026F, radius + 0.026F,
                0.06F, alpha * 0.84F, 1.0F, 0.66F, 0.14F, 3, seed, 0.0F);
        drawArcBand(vc, matrix, start, end, radius - 0.008F, radius + 0.008F,
                0.075F, alpha, 1.0F, 0.96F, 0.72F, 3, seed + 17, 0.0F);
    }

    private static void renderMainSlash(VertexConsumer darkVc, Matrix4f matrix, float age, int seed,
                                        boolean shaderFallback) {
        float alpha = BloodfyreTimeline.mainAlpha(age);
        float head = BloodfyreTimeline.slashProgress(age);
        if (alpha <= 0.002F || head <= 0.001F) {
            return;
        }

        float impactAge = Math.max(0.0F, age - BloodfyreTimeline.SLASH_END);
        float overshoot = age < BloodfyreTimeline.SLASH_END ? 0.0F
                : 0.046F * (float) Math.exp(-impactAge * 1.22F) * Mth.sin(impactAge * 3.45F + 0.72F);
        float movingHead = Mth.clamp(head + overshoot, 0.0F, 1.0F);
        float pulseDistance = (age - 13.2F) / 0.78F;
        float pulse = (float) Math.exp(-pulseDistance * pulseDistance);

        float erosion = BloodfyreTimeline.bodyErosion(age);
        float bodyTail = 0.16F * BloodfyreTimeline.smooth((age - 23.0F) / 8.0F);
        float darkTail = 0.12F * BloodfyreTimeline.smooth((age - 24.5F) / 8.0F);
        float darkFade = 1.0F - 0.22F * BloodfyreTimeline.smooth((age - 19.0F) / 12.0F);

        drawArcBand(darkVc, matrix, darkTail, movingHead, 3.02F, 4.94F, -0.015F,
                alpha * darkFade * 0.72F, 0.30F, 0.008F, 0.018F, 0, seed + 101,
                Mth.clamp(erosion * 1.08F, 0.0F, 1.0F));
        drawArcBand(darkVc, matrix, bodyTail, movingHead, 3.34F, 4.78F, 0.028F,
                alpha * darkFade * 0.90F, 0.94F, 0.012F, 0.022F, 1, seed + 131,
                erosion);

        // Keep all main-blade layers in one buffer.  Splitting the dark and bright
        // sheets across RenderTypes allowed deferred batching to draw the dark body
        // last, hiding the bright sheet except for its overhanging outer rim.
        drawErodingBrightBand(darkVc, matrix, age, 0.0F, movingHead, 3.42F, 4.84F, 0.072F,
                alpha * (0.98F + pulse * 0.02F), seed + 163);

        for (int i = 0; i < 4; i++) {
            float filamentTail = head < 0.995F ? Math.max(0.0F, head - (0.30F + i * 0.045F))
                    : 0.055F * BloodfyreTimeline.smooth((age - 18.0F - i * 0.35F) / 8.0F);
            float filamentHead = head < 0.995F ? Math.max(0.0F, head - i * 0.018F)
                    : 1.0F - 0.035F * BloodfyreTimeline.smooth((age - 18.0F - i * 0.25F) / 7.0F);
            float fade = 1.0F - BloodfyreTimeline.smooth((age - 24.0F - i * 0.6F) / 5.5F);
            float radius = 4.20F + i * 0.135F;
            drawArcBand(darkVc, matrix, filamentTail, filamentHead, radius - 0.012F, radius + 0.012F,
                    0.092F + i * 0.01F, alpha * fade * 0.78F,
                    1.0F, 0.58F, 0.12F, 3, seed + 211 + i * 19, 0.0F);
        }

        float headFade = 1.0F - BloodfyreTimeline.smooth((age - 14.2F) / 2.6F);
        if (headFade > 0.002F) {
            BloodfyreGeometry.Point center = BloodfyreGeometry.arc(movingHead, 4.58F, 0.10F);
            BloodfyreGeometry.Point tangent = BloodfyreGeometry.tangent(movingHead, 4.58F);
            if (shaderFallback) {
                drawFallbackJetStrip(darkVc, matrix, center, tangent,
                        0.72F + pulse * 0.22F, 0.035F,
                        alpha * headFade * 0.54F, 1.0F, 0.48F, 0.10F, 2);
            } else {
                drawJetSheet(darkVc, matrix, center, tangent, 1.25F + pulse * 0.55F,
                        0.26F + pulse * 0.16F, alpha * headFade, 1.0F, 0.84F, 0.42F, 2);
            }
        }

        renderErosionFront(darkVc, matrix, age, 0.0F, movingHead, alpha, seed + 181);
    }

    private static void drawErodingBrightBand(VertexConsumer vc, Matrix4f matrix, float age,
                                               float fromU, float toU,
                                               float innerRadius, float outerRadius, float yOffset,
                                               float alpha, int seed) {
        int start = Mth.clamp(Mth.floor(fromU * ARC_SEGMENTS), 0, ARC_SEGMENTS - 1);
        int end = Mth.clamp(Mth.ceil(toU * ARC_SEGMENTS), 1, ARC_SEGMENTS);
        for (int i = start; i < end; i++) {
            float u0 = Math.max(fromU, i / (float) ARC_SEGMENTS);
            float u1 = Math.min(toU, (i + 1) / (float) ARC_SEGMENTS);
            float localAge0 = age - BloodfyreTimeline.slashBirth(u0);
            float localAge1 = age - BloodfyreTimeline.slashBirth(u1);
            float visibility0 = brightVisibility(localAge0);
            float visibility1 = brightVisibility(localAge1);
            if (Math.max(visibility0, visibility1) <= 0.002F) {
                continue;
            }

            float boundary0 = brightBoundary(age, u0, localAge0, seed);
            float boundary1 = brightBoundary(age, u1, localAge1, seed);
            float outerNoise0 = brightOuterNoise(age, u0, seed);
            float outerNoise1 = brightOuterNoise(age, u1, seed);
            float inner0Radius = Mth.lerp(boundary0, innerRadius, outerRadius) - outerNoise0 * 0.12F;
            float inner1Radius = Mth.lerp(boundary1, innerRadius, outerRadius) - outerNoise1 * 0.12F;
            float outer0Radius = outerRadius + outerNoise0;
            float outer1Radius = outerRadius + outerNoise1;

            BloodfyreGeometry.Point inner0 = BloodfyreGeometry.arc(u0, inner0Radius, yOffset);
            BloodfyreGeometry.Point outer0 = BloodfyreGeometry.arc(u0, outer0Radius, yOffset);
            BloodfyreGeometry.Point inner1 = BloodfyreGeometry.arc(u1, inner1Radius, yOffset);
            BloodfyreGeometry.Point outer1 = BloodfyreGeometry.arc(u1, outer1Radius, yOffset);
            vertex(vc, matrix, outer0, u0, 1.0F, 1.0F, 0.90F, 0.48F, alpha * visibility0, 2);
            vertex(vc, matrix, outer1, u1, 1.0F, 1.0F, 0.90F, 0.48F, alpha * visibility1, 2);
            // Geometry already moved the inner edge to the erosion boundary.  UV.v
            // must span the surviving strip again; feeding boundary here made the
            // shader's own edge falloff erode the sheet a second time.
            vertex(vc, matrix, inner1, u1, 0.0F, 1.0F, 0.74F, 0.20F, alpha * visibility1, 2);
            vertex(vc, matrix, inner0, u0, 0.0F, 1.0F, 0.74F, 0.20F, alpha * visibility0, 2);
        }
    }

    private static void renderErosionFront(VertexConsumer vc, Matrix4f matrix, float age,
                                           float fromU, float toU, float alpha, int seed) {
        int start = Mth.clamp(Mth.floor(fromU * ARC_SEGMENTS), 0, ARC_SEGMENTS - 1);
        int end = Mth.clamp(Mth.ceil(toU * ARC_SEGMENTS), 1, ARC_SEGMENTS);
        for (int i = start; i < end; i++) {
            float u0 = Math.max(fromU, i / (float) ARC_SEGMENTS);
            float u1 = Math.min(toU, (i + 1) / (float) ARC_SEGMENTS);
            float localAge0 = age - BloodfyreTimeline.slashBirth(u0);
            float localAge1 = age - BloodfyreTimeline.slashBirth(u1);
            float erosion0 = brightErosionAmount(localAge0);
            float erosion1 = brightErosionAmount(localAge1);
            if (Math.max(erosion0, erosion1) <= 0.003F) {
                continue;
            }

            float strength0 = brightVisibility(localAge0)
                    * BloodfyreTimeline.smooth(erosion0 / 0.16F);
            float strength1 = brightVisibility(localAge1)
                    * BloodfyreTimeline.smooth(erosion1 / 0.16F);
            if (Math.max(strength0, strength1) <= 0.002F) {
                continue;
            }

            float boundary0 = brightBoundary(age, u0, localAge0, seed - 18);
            float boundary1 = brightBoundary(age, u1, localAge1, seed - 18);
            float radius0 = Mth.lerp(boundary0, 3.42F, 4.84F);
            float radius1 = Mth.lerp(boundary1, 3.42F, 4.84F);
            float width0 = 0.046F + Mth.sin(u0 * 91.0F + age * 0.42F) * 0.012F;
            float width1 = 0.046F + Mth.sin(u1 * 91.0F + age * 0.42F) * 0.012F;
            BloodfyreGeometry.Point inner0 = BloodfyreGeometry.arc(u0, radius0 - width0, 0.073F);
            BloodfyreGeometry.Point outer0 = BloodfyreGeometry.arc(u0, radius0 + width0, 0.073F);
            BloodfyreGeometry.Point inner1 = BloodfyreGeometry.arc(u1, radius1 - width1, 0.073F);
            BloodfyreGeometry.Point outer1 = BloodfyreGeometry.arc(u1, radius1 + width1, 0.073F);
            vertex(vc, matrix, outer0, u0, 1.0F, 0.52F, 0.005F, 0.012F,
                    alpha * strength0 * 0.96F, 4);
            vertex(vc, matrix, outer1, u1, 1.0F, 0.52F, 0.005F, 0.012F,
                    alpha * strength1 * 0.96F, 4);
            vertex(vc, matrix, inner1, u1, 0.0F, 0.52F, 0.005F, 0.012F,
                    alpha * strength1 * 0.96F, 4);
            vertex(vc, matrix, inner0, u0, 0.0F, 0.52F, 0.005F, 0.012F,
                    alpha * strength0 * 0.96F, 4);
        }
    }

    private static float brightVisibility(float localAge) {
        if (localAge <= 0.0F) {
            return 0.0F;
        }
        float reveal = BloodfyreTimeline.fastOut(localAge / 0.22F);
        float fade = 1.0F - BloodfyreTimeline.smooth((localAge - 7.0F) / 5.5F);
        return reveal * fade;
    }

    private static float brightErosionAmount(float localAge) {
        // Let each freshly drawn section read as a solid white-hot cut before the
        // corrosion starts eating it from the inner edge.  The old 0.72/3.6 curve
        // had already removed most of the bright body while the slash was still
        // extending, leaving only the outer rim visible at the impact frame.
        return BloodfyreTimeline.smooth((localAge - 1.30F) / 5.20F);
    }

    private static float brightBoundary(float age, float u, float localAge, int seed) {
        float erosion = brightErosionAmount(localAge);
        float finish = BloodfyreTimeline.smooth((age - BloodfyreTimeline.SLASH_END) / 4.0F);
        float reach = Mth.lerp(finish, 0.60F, 0.90F);
        float wave = Mth.sin(u * 31.0F + seed * 0.006F + age * 0.31F) * 0.064F
                + Mth.sin(u * 79.0F - seed * 0.003F - age * 0.23F) * 0.030F;
        return Mth.clamp(erosion * reach + wave * erosion * (1.0F - erosion * 0.35F),
                0.0F, 0.92F);
    }

    private static float brightOuterNoise(float age, float u, int seed) {
        return Mth.sin(u * 37.0F + seed * 0.004F + age * 0.22F) * 0.028F
                + Mth.sin(u * 103.0F - seed * 0.002F - age * 0.16F) * 0.012F;
    }

    private static void renderRupture(VertexConsumer vc, Matrix4f matrix, float age, int seed,
                                      boolean includeEndpointFlash, boolean shaderFallback,
                                      boolean emissiveOnly) {
        float layerAlpha = BloodfyreTimeline.window(age, BloodfyreTimeline.RUPTURE_START,
                BloodfyreTimeline.RUPTURE_END, 0.55F, 5.5F);
        if (layerAlpha <= 0.002F) {
            return;
        }

        for (int i = 0; i < RUPTURE_ANCHORS.length; i++) {
            float u = RUPTURE_ANCHORS[i];
            float birth = BloodfyreTimeline.slashBirth(u) + 0.32F + (i % 3) * 0.08F;
            float localAge = age - birth;
            if (localAge <= 0.0F || localAge >= 14.0F) {
                continue;
            }
            float random = BloodfyreGeometry.deterministic(seed + i * 47, 3.8F);
            float grow = BloodfyreTimeline.fastOut(localAge / 1.2F);
            float erode = BloodfyreTimeline.smooth((localAge - 4.2F) / 7.0F);
            float fade = 1.0F - BloodfyreTimeline.smooth((localAge - 7.0F) / 7.0F);
            float distance = localAge * (0.16F + random * 0.10F)
                    + (float) Math.pow(localAge, 1.28D) * (0.045F + random * 0.025F);
            float width = (0.16F + random * 0.24F) * grow * (1.0F - erode * 0.48F);
            BloodfyreGeometry.Point anchor = BloodfyreGeometry.arc(u, 4.56F, 0.02F);
            BloodfyreGeometry.Point tangent = BloodfyreGeometry.tangent(u, 4.56F);
            BloodfyreGeometry.Point radial = BloodfyreGeometry.radial(u);
            float endpointBoost = BloodfyreTimeline.smooth((u - 0.78F) / 0.20F);
            BloodfyreGeometry.Point direction = tangent.scale(0.44F + random * 0.26F)
                    .add(radial.scale(0.70F + endpointBoost * 0.58F))
                    .add(new BloodfyreGeometry.Point(0.0F, 0.30F + random * 0.42F, 0.0F))
                    .normalize();
            float strength = layerAlpha * fade;

            if (shaderFallback) {
                // Never submit the crossed flame sheets to a shader pack. Their
                // vertical plane is deliberately oversized for analytic clipping and
                // becomes the giant yellow wedges seen in the fallback screenshot.
                float fallbackLength = Math.min(distance * 0.62F + 0.12F, 1.08F);
                if (!emissiveOnly) {
                    drawFallbackJetStrip(vc, matrix, anchor, direction, fallbackLength,
                            0.026F + random * 0.012F, strength * 0.42F,
                            0.92F, 0.055F, 0.018F, 1);
                }
                drawFallbackJetStrip(vc, matrix, anchor.add(direction.scale(0.035F)), direction,
                        fallbackLength * 0.72F, 0.010F,
                        strength * (1.0F - erode * 0.66F) * 0.48F,
                        1.0F, 0.38F, 0.08F, 2);
            } else {
                if (!emissiveOnly) {
                    drawJetSheet(vc, matrix, anchor, direction, distance + 0.20F,
                            width * 1.85F + 0.035F, strength * 0.72F,
                            0.88F, 0.015F, 0.018F, 0);
                    drawJetSheet(vc, matrix, anchor, direction, distance * 1.08F + 0.18F,
                            width * 0.72F + 0.018F, strength,
                            1.0F, 0.30F, 0.028F, 1);
                }
                drawJetSheet(vc, matrix, anchor, direction, distance * 1.14F + 0.12F,
                        width * (emissiveOnly ? 0.30F : 0.20F) + 0.010F,
                        strength * (1.0F - erode * 0.62F),
                        1.0F, 0.96F, 0.72F, 2);
            }
        }

        float flash = BloodfyreTimeline.window(age, 11.2F, 15.8F, 0.22F, 2.6F);
        if (includeEndpointFlash && flash > 0.002F) {
            BloodfyreGeometry.Point endpoint = BloodfyreGeometry.arc(0.975F, 4.62F, 0.55F);
            crossSprite(vc, matrix, endpoint, 0.65F + flash * 1.55F,
                    0.48F + flash * 1.20F, 1.0F, 0.36F, 0.05F, flash, 3);
        }
    }

    private static void renderSmoke(VertexConsumer vc, Matrix4f matrix, float age, int seed) {
        float layerAlpha = BloodfyreTimeline.window(age, BloodfyreTimeline.SMOKE_START,
                BloodfyreTimeline.SMOKE_END, 1.0F, 7.0F);
        if (layerAlpha <= 0.002F) {
            return;
        }

        for (int i = 0; i < SMOKE_COUNT; i++) {
            float u = 0.035F + i / (float) (SMOKE_COUNT - 1) * 0.94F;
            float birth = BloodfyreTimeline.slashBirth(u) + 0.72F + (i % 4) * 0.11F;
            float localAge = age - birth;
            if (localAge <= 0.0F || localAge >= 19.0F) {
                continue;
            }
            float random = BloodfyreGeometry.deterministic(seed + i * 59, 8.1F);
            float grow = BloodfyreTimeline.fastOut(localAge / 2.1F);
            float fade = 1.0F - BloodfyreTimeline.smooth((localAge - 9.0F) / 10.0F);
            BloodfyreGeometry.Point anchor = BloodfyreGeometry.arc(u, 4.45F, 0.22F);
            BloodfyreGeometry.Point tangent = BloodfyreGeometry.tangent(u, 4.45F);
            BloodfyreGeometry.Point radial = BloodfyreGeometry.radial(u);
            BloodfyreGeometry.Point center = anchor
                    .add(tangent.scale(localAge * (0.055F + random * 0.035F)))
                    .add(radial.scale(localAge * (0.065F + random * 0.045F)))
                    .add(new BloodfyreGeometry.Point(0.0F,
                            localAge * (0.08F + random * 0.045F), 0.0F));
            float size = 0.18F + grow * (0.34F + random * 0.34F) + localAge * 0.045F;
            crossSprite(vc, matrix, center, size * (1.20F + (i % 3) * 0.10F), size,
                    0.10F, 0.018F, 0.024F, layerAlpha * fade * (0.48F + random * 0.24F), 0);

            if ((i & 1) == 0) {
                BloodfyreGeometry.Point end = center.add(tangent.scale(0.55F + localAge * 0.08F))
                        .add(new BloodfyreGeometry.Point(0.0F, 0.28F + localAge * 0.025F, 0.0F));
                drawJetSheet(vc, matrix, anchor, end.subtract(anchor).normalize(),
                        end.subtract(anchor).length(), size * 0.34F,
                        layerAlpha * fade * 0.38F, 0.12F, 0.025F, 0.03F, 1);
            }
        }
    }

    private static void renderFragments(VertexConsumer vc, Matrix4f matrix, float age, int seed) {
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            int cluster = i / 3;
            float u = 0.055F + cluster / 9.0F * 0.90F + (i % 3 - 1) * 0.009F;
            float birth = BloodfyreTimeline.slashBirth(u) + 1.10F + (i % 3) * 0.10F;
            float localAge = age - birth;
            float life = 7.0F + (i % 4) * 0.55F;
            if (localAge <= 0.0F || localAge >= life) {
                continue;
            }
            float fade = 1.0F - BloodfyreTimeline.smooth((localAge - life * 0.58F) / (life * 0.42F));
            float random = BloodfyreGeometry.deterministic(seed + i * 71, 4.4F);
            BloodfyreGeometry.Point origin = BloodfyreGeometry.arc(u, 4.58F, 0.16F);
            BloodfyreGeometry.Point radial = BloodfyreGeometry.radial(u);
            BloodfyreGeometry.Point tangent = BloodfyreGeometry.tangent(u, 4.58F);
            BloodfyreGeometry.Point velocity = radial.scale(0.14F + random * 0.12F)
                    .add(tangent.scale(0.07F + (i % 3) * 0.035F))
                    .add(new BloodfyreGeometry.Point(0.0F, 0.15F + (i % 4) * 0.045F, 0.0F));
            BloodfyreGeometry.Point center = origin.add(velocity.scale(localAge))
                    .add(new BloodfyreGeometry.Point(0.0F, -localAge * localAge * 0.038F, 0.0F));
            float size = 0.09F + random * 0.085F;
            shard(vc, matrix, center, size, localAge * (0.48F + random * 0.42F),
                    i % 4 == 0 ? 0.22F : 0.95F, i % 4 == 0 ? 0.035F : 0.18F,
                    0.025F, fade * 0.86F, 1);
        }
    }

    private static void renderGroundScar(BloodfyreFrenzyEntity entity, MultiBufferSource buffer,
                                         Matrix4f matrix, float age, int seed, float alphaScale,
                                         boolean shaderFallback) {
        float alpha = BloodfyreTimeline.scarAlpha(age) * alphaScale;
        if (alpha <= 0.002F) {
            return;
        }
        GroundTrace trace = groundTrace(entity);
        float head = BloodfyreTimeline.slashProgress(age - 0.10F);
        float decay = BloodfyreTimeline.smooth((age - 43.0F) / 9.0F);
        float heat = 1.0F - BloodfyreTimeline.smooth((age - 24.0F) / 9.0F);

        VertexConsumer scar = buffer.getBuffer(KabladeRenderTypes.bloodfyreScar());
        if (shaderFallback) {
            // Shader packs expose z-fighting between the three overlapping scar
            // bands as alternating bright triangles. Use one lifted band and one
            // set of narrow branches; its orange-red color still receives pack bloom.
            drawGroundArc(scar, matrix, trace, head, 0.080F, alpha * 0.70F,
                    0.42F, 0.008F, 0.014F, 1, seed + 607, decay * 0.82F, 0.018F);
            for (int i = 0; i < BRANCHES.length; i++) {
                BranchSpec branch = BRANCHES[i];
                if (head < branch.u + 0.006F) {
                    continue;
                }
                float birth = BloodfyreTimeline.slashBirth(branch.u) + 0.20F;
                float grow = BloodfyreTimeline.smooth((age - birth) / 0.60F);
                float branchFade = 1.0F - BloodfyreTimeline.smooth((age - 34.0F - i % 3) / 12.0F);
                drawGroundBranch(scar, matrix, entity, branch, grow,
                        0.026F, alpha * branchFade * 0.62F,
                        0.50F, 0.010F, 0.018F, 1, seed + 701 + i * 17);
            }
            return;
        }

        drawGroundArc(scar, matrix, trace, head, 0.205F, alpha * 0.86F,
                0.08F, 0.004F, 0.012F, 0, seed + 601, decay * 0.72F, 0.004F);
        drawGroundArc(scar, matrix, trace, head, 0.092F, alpha * 0.94F,
                0.56F, 0.018F, 0.028F, 1, seed + 607, decay * 0.86F, 0.008F);

        for (int i = 0; i < BRANCHES.length; i++) {
            BranchSpec branch = BRANCHES[i];
            if (head < branch.u + 0.006F) {
                continue;
            }
            float birth = BloodfyreTimeline.slashBirth(branch.u) + 0.20F;
            float grow = BloodfyreTimeline.smooth((age - birth) / 0.60F);
            float branchFade = 1.0F - BloodfyreTimeline.smooth((age - 34.0F - i % 3) / 12.0F);
            drawGroundBranch(scar, matrix, entity, branch, grow,
                    shaderFallback ? 0.036F : 0.068F, alpha * branchFade,
                    0.48F, 0.012F, 0.022F, 1, seed + 701 + i * 17);
        }

        if (heat > 0.002F) {
            VertexConsumer glow = buffer.getBuffer(KabladeRenderTypes.bloodfyreScarGlow());
            drawGroundArc(glow, matrix, trace, head, 0.026F, alpha * heat,
                    1.0F, 0.36F, 0.065F, 2, seed + 613, decay, 0.012F);
            for (int i = 5; i < BRANCHES.length; i++) {
                BranchSpec branch = BRANCHES[i];
                if (head < branch.u + 0.006F) {
                    continue;
                }
                float birth = BloodfyreTimeline.slashBirth(branch.u) + 0.20F;
                float grow = BloodfyreTimeline.smooth((age - birth) / 0.60F);
                float branchFade = 1.0F - BloodfyreTimeline.smooth((age - 34.0F - i % 3) / 12.0F);
                drawGroundBranch(glow, matrix, entity, branch, grow,
                        0.015F, alpha * heat * branchFade, 1.0F, 0.32F, 0.045F, 2,
                        seed + 811 + i * 19);
            }
        }
    }

    /** The non-emissive scar layers, split out so the Oculus path can preserve alpha blending. */
    private static void renderGroundScarBase(BloodfyreFrenzyEntity entity, VertexConsumer scar,
                                             Matrix4f matrix, float age, int seed) {
        float alpha = BloodfyreTimeline.scarAlpha(age);
        if (alpha <= 0.002F) {
            return;
        }
        GroundTrace trace = groundTrace(entity);
        float head = BloodfyreTimeline.slashProgress(age - 0.10F);
        float decay = BloodfyreTimeline.smooth((age - 43.0F) / 9.0F);
        drawGroundArc(scar, matrix, trace, head, 0.205F, alpha * 0.86F,
                0.08F, 0.004F, 0.012F, 0, seed + 601, decay * 0.72F, 0.004F);
        drawGroundArc(scar, matrix, trace, head, 0.092F, alpha * 0.94F,
                0.56F, 0.018F, 0.028F, 1, seed + 607, decay * 0.86F, 0.008F);
        for (int i = 0; i < BRANCHES.length; i++) {
            BranchSpec branch = BRANCHES[i];
            if (head < branch.u + 0.006F) {
                continue;
            }
            float birth = BloodfyreTimeline.slashBirth(branch.u) + 0.20F;
            float grow = BloodfyreTimeline.smooth((age - birth) / 0.60F);
            float branchFade = 1.0F - BloodfyreTimeline.smooth((age - 34.0F - i % 3) / 12.0F);
            drawGroundBranch(scar, matrix, entity, branch, grow,
                    0.068F, alpha * branchFade,
                    0.48F, 0.012F, 0.022F, 1, seed + 701 + i * 17);
        }
    }

    /** White-hot scar core, kept separate from its black/red base to avoid additive washout. */
    private static void renderGroundScarHeat(BloodfyreFrenzyEntity entity, VertexConsumer glow,
                                             Matrix4f matrix, float age, int seed) {
        float alpha = BloodfyreTimeline.scarAlpha(age);
        float heat = 1.0F - BloodfyreTimeline.smooth((age - 24.0F) / 9.0F);
        if (alpha <= 0.002F || heat <= 0.002F) {
            return;
        }
        GroundTrace trace = groundTrace(entity);
        float head = BloodfyreTimeline.slashProgress(age - 0.10F);
        float decay = BloodfyreTimeline.smooth((age - 43.0F) / 9.0F);
        drawGroundArc(glow, matrix, trace, head, 0.026F, alpha * heat,
                1.0F, 0.36F, 0.065F, 2, seed + 613, decay, 0.012F);
        for (int i = 5; i < BRANCHES.length; i++) {
            BranchSpec branch = BRANCHES[i];
            if (head < branch.u + 0.006F) {
                continue;
            }
            float birth = BloodfyreTimeline.slashBirth(branch.u) + 0.20F;
            float grow = BloodfyreTimeline.smooth((age - birth) / 0.60F);
            float branchFade = 1.0F - BloodfyreTimeline.smooth((age - 34.0F - i % 3) / 12.0F);
            drawGroundBranch(glow, matrix, entity, branch, grow,
                    0.015F, alpha * heat * branchFade,
                    1.0F, 0.32F, 0.045F, 2, seed + 811 + i * 19);
        }
    }

    private static void renderGroundScarVanillaGlow(BloodfyreFrenzyEntity entity, VertexConsumer glow,
                                                     Matrix4f matrix, float age, int seed) {
        float alpha = BloodfyreTimeline.scarAlpha(age);
        if (alpha <= 0.002F) {
            return;
        }
        GroundTrace trace = groundTrace(entity);
        float head = BloodfyreTimeline.slashProgress(age - 0.10F);
        float decay = BloodfyreTimeline.smooth((age - 43.0F) / 9.0F);
        float heat = 1.0F - BloodfyreTimeline.smooth((age - 24.0F) / 9.0F);
        float residual = 0.28F + heat * 0.72F;

        drawGroundArc(glow, matrix, trace, head, 0.145F, alpha * residual * 0.72F,
                0.82F, 0.055F, 0.018F, 1, seed + 607, decay * 0.82F, 0.010F);
        drawGroundArc(glow, matrix, trace, head, 0.032F, alpha * residual,
                1.0F, 0.42F, 0.075F, 2, seed + 613, decay, 0.013F);
        for (int i = 0; i < BRANCHES.length; i++) {
            BranchSpec branch = BRANCHES[i];
            if (head < branch.u + 0.006F) {
                continue;
            }
            float birth = BloodfyreTimeline.slashBirth(branch.u) + 0.20F;
            float grow = BloodfyreTimeline.smooth((age - birth) / 0.60F);
            float branchFade = 1.0F - BloodfyreTimeline.smooth((age - 34.0F - i % 3) / 12.0F);
            drawGroundBranch(glow, matrix, entity, branch, grow,
                    0.024F, alpha * residual * branchFade, 1.0F, 0.30F, 0.045F, 2,
                    seed + 811 + i * 19);
        }
    }

    private static void renderEmbers(VertexConsumer vc, Matrix4f matrix, float age, int seed) {
        for (int i = 0; i < EMBER_COUNT; i++) {
            float u = BloodfyreGeometry.deterministic(seed + i * 131, 4.2F);
            float birth = BloodfyreTimeline.slashBirth(u) + 2.0F + (i % 5) * 0.35F;
            float localAge = age - birth;
            float life = 9.0F + (i % 4) * 1.2F;
            if (localAge <= 0.0F || localAge >= life) {
                continue;
            }
            float fade = 1.0F - BloodfyreTimeline.smooth((localAge - life * 0.52F) / (life * 0.48F));
            BloodfyreGeometry.Point origin = BloodfyreGeometry.arc(u, 4.20F, 0.34F);
            BloodfyreGeometry.Point radial = BloodfyreGeometry.radial(u);
            BloodfyreGeometry.Point center = origin.add(radial.scale(localAge * (0.035F + (i % 3) * 0.014F)))
                    .add(new BloodfyreGeometry.Point(
                            Mth.sin(localAge * 0.7F + i) * 0.08F,
                            localAge * (0.09F + (i % 4) * 0.014F),
                            Mth.cos(localAge * 0.6F + i) * 0.08F));
            float size = 0.025F + (i % 4) * 0.009F;
            crossSprite(vc, matrix, center, size, size,
                    1.0F, 0.36F, 0.05F, fade * 0.88F, 2);
        }
    }

    private static void renderPetals(PoseStack poseStack, VertexConsumer vc, float age, int seed) {
        PetalMesh mesh = getPetalMesh();
        if (mesh.faces.isEmpty()) {
            return;
        }
        for (int i = 0; i < PETAL_COUNT; i++) {
            float spawn = i < 18 ? (i % 6) * 0.46F : 4.0F + (i % 10) * 0.58F;
            if (i >= 34) {
                spawn = 10.6F + (i % 4) * 0.34F;
            }
            float localAge = age - spawn;
            float life = 9.0F + BloodfyreGeometry.deterministic(seed + i * 89, 4.7F) * 8.0F;
            if (localAge <= 0.0F || localAge >= life) {
                continue;
            }
            float fade = 1.0F - BloodfyreTimeline.smooth((localAge - life * 0.52F) / (life * 0.48F));
            float angle = BloodfyreGeometry.deterministic(seed + i * 97, 7.1F) * Mth.TWO_PI;
            float speed = 0.045F + BloodfyreGeometry.deterministic(seed + i * 101, 1.9F) * 0.095F;
            float travel = localAge * (0.76F - localAge * 0.012F);
            float originZ = i >= 34 ? 4.2F : 0.0F;
            float x = Mth.cos(angle) * speed * travel;
            float z = originZ + Mth.sin(angle) * speed * travel + (i >= 34 ? localAge * 0.055F : 0.0F);
            float y = 0.16F + BloodfyreGeometry.deterministic(seed + i * 109, 5.5F) * 0.68F
                    + localAge * (0.028F + (i % 3) * 0.012F) - localAge * localAge * 0.0038F;
            float scale = 0.015F + BloodfyreGeometry.deterministic(seed + i * 113, 9.6F) * 0.009F;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.XP.rotation(localAge * (0.20F + (i % 4) * 0.10F)));
            poseStack.mulPose(Axis.YP.rotation(angle + localAge * (0.24F + (i % 3) * 0.11F)));
            poseStack.mulPose(Axis.ZP.rotation(localAge * (0.18F + (i % 5) * 0.08F)));
            poseStack.scale(scale, scale, scale);
            float red = i % 3 == 0 ? 0.44F : i % 3 == 1 ? 0.66F : 0.82F;
            mesh.render(poseStack.last().pose(), vc, red, 0.008F, 0.025F, fade * 0.88F);
            poseStack.popPose();
        }
    }

    private static void drawArcBand(VertexConsumer vc, Matrix4f matrix,
                                    float fromU, float toU, float innerRadius, float outerRadius,
                                    float yOffset, float alpha,
                                    float red, float green, float blue, int kind,
                                    int seed, float erosion) {
        if (toU <= fromU + 0.001F || alpha <= 0.002F) {
            return;
        }
        int start = Mth.clamp(Mth.floor(fromU * ARC_SEGMENTS), 0, ARC_SEGMENTS - 1);
        int end = Mth.clamp(Mth.ceil(toU * ARC_SEGMENTS), 1, ARC_SEGMENTS);
        boolean layeredBlade = kind <= 2 && outerRadius - innerRadius > 0.30F;
        int radialSlices = layeredBlade ? 32 : 1;
        for (int i = start; i < end; i++) {
            float u0 = Math.max(fromU, i / (float) ARC_SEGMENTS);
            float u1 = Math.min(toU, (i + 1) / (float) ARC_SEGMENTS);
            float grain = BloodfyreGeometry.deterministic(seed + i * 37, 6.1F)
                    + Mth.sin(i * 0.73F + seed * 0.003F) * 0.12F;
            if (!layeredBlade && erosion > 0.02F && grain < erosion * 0.64F) {
                continue;
            }
            float edgeNoise0 = (BloodfyreGeometry.deterministic(seed + i * 17, 2.8F) - 0.5F) * 0.10F;
            float edgeNoise1 = (BloodfyreGeometry.deterministic(seed + (i + 1) * 17, 2.8F) - 0.5F) * 0.10F;
            for (int slice = 0; slice < radialSlices; slice++) {
                float v0 = slice / (float) radialSlices;
                float v1 = (slice + 1) / (float) radialSlices;
                float vCenter = (v0 + v1) * 0.5F;
                float clippedV0 = v0;
                float cellGrain = BloodfyreGeometry.deterministic(
                        seed + i * 97 + slice * 29, 9.4F);
                if (layeredBlade && erosion > 0.08F) {
                    float cellU = (u0 + u1) * 0.5F;
                    float edgeWave = Mth.sin(vCenter * 18.0F + seed * 0.006F) * 0.5F
                            + Mth.sin(vCenter * 41.0F - seed * 0.003F) * 0.25F;
                    float headBite = erosion * (0.010F + (1.0F - vCenter) * 0.032F
                            + Math.max(0.0F, edgeWave) * 0.012F);
                    float tailBite = erosion * (0.016F + (1.0F - vCenter) * 0.054F
                            + Math.max(0.0F, -edgeWave) * 0.022F);
                    if (cellU > toU - headBite || cellU < fromU + tailBite) {
                        continue;
                    }
                }

                if (layeredBlade && kind == 2) {
                    float boundaryNoise = Mth.sin(i * 0.17F + seed * 0.002F + erosion * 4.8F) * 0.072F
                            + Mth.sin(i * 0.43F + seed * 0.004F - erosion * 6.1F) * 0.038F;
                    float erodedBoundary = Mth.clamp(erosion * 0.86F + boundaryNoise, 0.0F, 0.90F);
                    boolean eatenFromInside = vCenter < erodedBoundary;
                    boolean perforated = erosion > 0.24F && vCenter < 0.90F
                            && cellGrain < erosion * (0.12F + (1.0F - vCenter) * 0.30F);
                    if (eatenFromInside || perforated) {
                        continue;
                    }
                } else if (layeredBlade && erosion > 0.03F) {
                    float dissolve = BloodfyreTimeline.smooth((erosion - 0.03F) / 0.95F);
                    float boundaryWave = Mth.sin(i * 0.21F + seed * 0.004F) * 0.055F
                            + Mth.sin(i * 0.47F - seed * 0.002F) * 0.025F;
                    float innerBite = dissolve * (kind == 1 ? 0.42F : 0.56F)
                            + boundaryWave * dissolve;
                    float threshold = dissolve * (kind == 1 ? 0.70F : 0.58F)
                            * (0.70F + (1.0F - vCenter) * 0.50F);
                    // Clip the currently intersected radial cell at the live erosion boundary.
                    // Deleting only complete cells made 16-frame-sized visual plateaus followed
                    // by jumps. The boundary now advances inside a cell every rendered frame.
                    if (v1 <= innerBite || cellGrain < threshold) {
                        continue;
                    }
                    clippedV0 = Math.max(v0, Mth.clamp(innerBite, 0.0F, v1));
                }

                float radius00 = Mth.lerp(clippedV0, innerRadius, outerRadius)
                        + Mth.lerp(clippedV0, -edgeNoise0 * 0.22F, edgeNoise0);
                float radius01 = Mth.lerp(v1, innerRadius, outerRadius)
                        + Mth.lerp(v1, -edgeNoise0 * 0.22F, edgeNoise0);
                float radius10 = Mth.lerp(clippedV0, innerRadius, outerRadius)
                        + Mth.lerp(clippedV0, -edgeNoise1 * 0.22F, edgeNoise1);
                float radius11 = Mth.lerp(v1, innerRadius, outerRadius)
                        + Mth.lerp(v1, -edgeNoise1 * 0.22F, edgeNoise1);
                BloodfyreGeometry.Point p00 = BloodfyreGeometry.arc(u0, radius00, yOffset);
                BloodfyreGeometry.Point p01 = BloodfyreGeometry.arc(u0, radius01, yOffset);
                BloodfyreGeometry.Point p10 = BloodfyreGeometry.arc(u1, radius10, yOffset);
                BloodfyreGeometry.Point p11 = BloodfyreGeometry.arc(u1, radius11, yOffset);
                quad(vc, matrix, p01, u0, v1, p11, u1, v1,
                        p10, u1, clippedV0, p00, u0, clippedV0,
                        red, green, blue, alpha, kind);
            }
        }
    }

    private static void drawJetSheet(VertexConsumer vc, Matrix4f matrix,
                                     BloodfyreGeometry.Point anchor, BloodfyreGeometry.Point direction,
                                     float length, float width, float alpha,
                                     float red, float green, float blue, int kind) {
        if (length <= 0.01F || width <= 0.001F || alpha <= 0.002F) {
            return;
        }
        BloodfyreGeometry.Point dir = direction.normalize();
        BloodfyreGeometry.Point side = new BloodfyreGeometry.Point(-dir.z(), 0.0F, dir.x()).normalize();
        BloodfyreGeometry.Point upSide = new BloodfyreGeometry.Point(0.0F, 1.0F, 0.0F);
        BloodfyreGeometry.Point tip = anchor.add(dir.scale(length));
        float rootWidth = width * 0.22F;
        float tipWidth = width * 0.08F;
        quad(vc, matrix,
                anchor.add(side.scale(-rootWidth)), 0.0F, 0.0F,
                tip.add(side.scale(-tipWidth)), 1.0F, 0.20F,
                tip.add(side.scale(tipWidth)), 1.0F, 0.80F,
                anchor.add(side.scale(rootWidth)), 0.0F, 1.0F,
                red, green, blue, alpha, kind);
        quad(vc, matrix,
                anchor.add(upSide.scale(-rootWidth)), 0.0F, 0.0F,
                tip.add(upSide.scale(-tipWidth)), 1.0F, 0.20F,
                tip.add(upSide.scale(tipWidth)), 1.0F, 0.80F,
                anchor.add(upSide.scale(rootWidth)), 0.0F, 1.0F,
                red, green, blue, alpha * 0.86F, kind);
    }

    /** A single flat, non-degenerate ribbon for shader-pack fallback rendering. */
    private static void drawFallbackJetStrip(VertexConsumer vc, Matrix4f matrix,
                                             BloodfyreGeometry.Point anchor,
                                             BloodfyreGeometry.Point direction,
                                             float length, float width, float alpha,
                                             float red, float green, float blue, int kind) {
        if (length <= 0.01F || width <= 0.001F || alpha <= 0.002F) {
            return;
        }
        BloodfyreGeometry.Point dir = direction.normalize();
        BloodfyreGeometry.Point side = new BloodfyreGeometry.Point(-dir.z(), 0.0F, dir.x()).normalize();
        BloodfyreGeometry.Point end = anchor.add(dir.scale(length));
        float endWidth = Math.max(0.006F, width * 0.42F);
        quad(vc, matrix,
                anchor.add(side.scale(-width)), 0.0F, 0.0F,
                end.add(side.scale(-endWidth)), 1.0F, 0.0F,
                end.add(side.scale(endWidth)), 1.0F, 1.0F,
                anchor.add(side.scale(width)), 0.0F, 1.0F,
                red, green, blue, alpha, kind);
    }

    private static void drawGroundArc(VertexConsumer vc, Matrix4f matrix, GroundTrace trace,
                                      float head, float halfWidth, float alpha,
                                      float red, float green, float blue, int kind,
                                      int seed, float erosion, float lift) {
        int visible = Mth.clamp(Mth.ceil(head * ARC_SEGMENTS), 0, ARC_SEGMENTS);
        for (int i = 0; i < visible; i++) {
            float grain = BloodfyreGeometry.deterministic(seed + i * 41, 5.3F);
            if (grain < erosion * 0.74F) {
                continue;
            }
            float u0 = i / (float) ARC_SEGMENTS;
            float u1 = (i + 1) / (float) ARC_SEGMENTS;
            BloodfyreGeometry.Point inner0 = withY(BloodfyreGeometry.groundArc(u0, SCAR_RADIUS - halfWidth),
                    trace.mainHeights[i] + lift);
            BloodfyreGeometry.Point outer0 = withY(BloodfyreGeometry.groundArc(u0, SCAR_RADIUS + halfWidth),
                    trace.mainHeights[i] + lift);
            BloodfyreGeometry.Point inner1 = withY(BloodfyreGeometry.groundArc(u1, SCAR_RADIUS - halfWidth),
                    trace.mainHeights[i + 1] + lift);
            BloodfyreGeometry.Point outer1 = withY(BloodfyreGeometry.groundArc(u1, SCAR_RADIUS + halfWidth),
                    trace.mainHeights[i + 1] + lift);
            quad(vc, matrix, outer0, u0, 1.0F, outer1, u1, 1.0F,
                    inner1, u1, 0.0F, inner0, u0, 0.0F,
                    red, green, blue, alpha, kind);
        }
    }

    private static void drawGroundBranch(VertexConsumer vc, Matrix4f matrix,
                                         BloodfyreFrenzyEntity entity, BranchSpec spec,
                                         float reveal, float width, float alpha,
                                         float red, float green, float blue, int kind, int seed) {
        List<BloodfyreGeometry.Point> points = branchPoints(spec, seed);
        int visible = Mth.clamp(Mth.ceil((points.size() - 1) * reveal), 0, points.size() - 1);
        for (int i = 0; i < visible; i++) {
            BloodfyreGeometry.Point a = points.get(i);
            BloodfyreGeometry.Point b = points.get(i + 1);
            BloodfyreGeometry.Point direction = b.subtract(a).normalize();
            BloodfyreGeometry.Point side = new BloodfyreGeometry.Point(-direction.z(), 0.0F, direction.x());
            float taperA = width * (1.0F - i / (float) (points.size() - 1));
            float taperB = width * (1.0F - (i + 1) / (float) (points.size() - 1));
            float yA = groundOffset(entity, a.x(), a.z()) + 0.014F;
            float yB = groundOffset(entity, b.x(), b.z()) + 0.014F;
            BloodfyreGeometry.Point a0 = withY(a.add(side.scale(-taperA)), yA);
            BloodfyreGeometry.Point a1 = withY(a.add(side.scale(taperA)), yA);
            BloodfyreGeometry.Point b0 = withY(b.add(side.scale(-taperB)), yB);
            BloodfyreGeometry.Point b1 = withY(b.add(side.scale(taperB)), yB);
            quad(vc, matrix, a0, 0.0F, 0.0F, b0, 1.0F, 0.0F,
                    b1, 1.0F, 1.0F, a1, 0.0F, 1.0F,
                    red, green, blue, alpha, kind);
        }
    }

    private static List<BloodfyreGeometry.Point> branchPoints(BranchSpec spec, int seed) {
        List<BloodfyreGeometry.Point> points = new ArrayList<>(7);
        BloodfyreGeometry.Point origin = BloodfyreGeometry.groundArc(spec.u, SCAR_RADIUS);
        BloodfyreGeometry.Point radial = BloodfyreGeometry.radial(spec.u).scale(spec.side);
        BloodfyreGeometry.Point tangent = BloodfyreGeometry.tangent(spec.u, SCAR_RADIUS);
        BloodfyreGeometry.Point direction = radial.scale(0.86F)
                .add(tangent.scale((BloodfyreGeometry.deterministic(seed, 3.1F) - 0.5F) * 0.72F))
                .normalize();
        points.add(origin);
        for (int i = 1; i <= 6; i++) {
            float progress = i / 6.0F;
            float bend = Mth.sin(seed * 0.013F + i * 2.4F) * spec.length * 0.075F * progress;
            float jitterX = (BloodfyreGeometry.deterministic(seed + i * 13, 4.7F) - 0.5F) * 0.12F;
            float jitterZ = (BloodfyreGeometry.deterministic(seed + i * 19, 8.2F) - 0.5F) * 0.12F;
            points.add(origin.add(direction.scale(spec.length * progress))
                    .add(tangent.scale(bend))
                    .add(new BloodfyreGeometry.Point(jitterX, 0.0F, jitterZ)));
        }
        return points;
    }

    private static GroundTrace groundTrace(BloodfyreFrenzyEntity entity) {
        GroundTrace trace = GROUND_TRACES.get(entity);
        if (trace != null && entity.tickCount > 8) {
            return trace;
        }
        float[] heights = new float[ARC_SEGMENTS + 1];
        for (int i = 0; i <= ARC_SEGMENTS; i++) {
            BloodfyreGeometry.Point point = BloodfyreGeometry.groundArc(i / (float) ARC_SEGMENTS, SCAR_RADIUS);
            heights[i] = groundOffset(entity, point.x(), point.z());
        }
        trace = new GroundTrace(heights);
        GROUND_TRACES.put(entity, trace);
        return trace;
    }

    private static float groundOffset(BloodfyreFrenzyEntity entity, float localX, float localZ) {
        // Match the ground projection to the 180-degree render-space correction above.
        float yaw = (entity.getYRot() + 180.0F) * Mth.DEG_TO_RAD;
        double worldX = entity.getX() + Mth.cos(yaw) * localX - Mth.sin(yaw) * localZ;
        double worldZ = entity.getZ() + Mth.sin(yaw) * localX + Mth.cos(yaw) * localZ;
        int top = Mth.floor(entity.getY() + 2.5D);
        int bottom = Mth.floor(entity.getY() - 3.0D);
        for (int y = top; y >= bottom; y--) {
            BlockPos position = BlockPos.containing(worldX, y, worldZ);
            VoxelShape shape = entity.level().getBlockState(position)
                    .getCollisionShape(entity.level(), position, CollisionContext.empty());
            if (!shape.isEmpty()) {
                double surface = position.getY() + shape.max(Direction.Axis.Y);
                return (float) (surface - entity.getY());
            }
        }
        return 0.0F;
    }

    private static BloodfyreGeometry.Point withY(BloodfyreGeometry.Point point, float y) {
        return new BloodfyreGeometry.Point(point.x(), y, point.z());
    }

    private static void crossSprite(VertexConsumer vc, Matrix4f matrix, BloodfyreGeometry.Point center,
                                    float halfWidth, float halfHeight,
                                    float red, float green, float blue, float alpha, int kind) {
        quad(vc, matrix,
                new BloodfyreGeometry.Point(center.x() - halfWidth, center.y() - halfHeight, center.z()), 0.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x() + halfWidth, center.y() - halfHeight, center.z()), 1.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x() + halfWidth, center.y() + halfHeight, center.z()), 1.0F, 1.0F,
                new BloodfyreGeometry.Point(center.x() - halfWidth, center.y() + halfHeight, center.z()), 0.0F, 1.0F,
                red, green, blue, alpha, kind);
        quad(vc, matrix,
                new BloodfyreGeometry.Point(center.x(), center.y() - halfHeight, center.z() - halfWidth), 0.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x(), center.y() - halfHeight, center.z() + halfWidth), 1.0F, 0.0F,
                new BloodfyreGeometry.Point(center.x(), center.y() + halfHeight, center.z() + halfWidth), 1.0F, 1.0F,
                new BloodfyreGeometry.Point(center.x(), center.y() + halfHeight, center.z() - halfWidth), 0.0F, 1.0F,
                red, green, blue, alpha * 0.86F, kind);
    }

    private static void shard(VertexConsumer vc, Matrix4f matrix, BloodfyreGeometry.Point center,
                              float size, float rotation,
                              float red, float green, float blue, float alpha, int kind) {
        float cx = Mth.cos(rotation) * size;
        float cz = Mth.sin(rotation) * size;
        float sx = -Mth.sin(rotation) * size * 0.42F;
        float sz = Mth.cos(rotation) * size * 0.42F;
        quad(vc, matrix,
                new BloodfyreGeometry.Point(center.x() - cx, center.y(), center.z() - cz), 0.0F, 0.5F,
                new BloodfyreGeometry.Point(center.x() + sx, center.y() + size * 0.52F, center.z() + sz), 0.5F, 0.0F,
                new BloodfyreGeometry.Point(center.x() + cx, center.y(), center.z() + cz), 1.0F, 0.5F,
                new BloodfyreGeometry.Point(center.x() - sx, center.y() - size * 0.52F, center.z() - sz), 0.5F, 1.0F,
                red, green, blue, alpha, kind);
    }

    private static void quad(VertexConsumer vc, Matrix4f matrix,
                             BloodfyreGeometry.Point p0, float u0, float v0,
                             BloodfyreGeometry.Point p1, float u1, float v1,
                             BloodfyreGeometry.Point p2, float u2, float v2,
                             BloodfyreGeometry.Point p3, float u3, float v3,
                             float red, float green, float blue, float alpha, int kind) {
        vertex(vc, matrix, p0, u0, v0, red, green, blue, alpha, kind);
        vertex(vc, matrix, p1, u1, v1, red, green, blue, alpha, kind);
        vertex(vc, matrix, p2, u2, v2, red, green, blue, alpha, kind);
        vertex(vc, matrix, p3, u3, v3, red, green, blue, alpha, kind);
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix, BloodfyreGeometry.Point point,
                               float u, float v, float red, float green, float blue,
                               float alpha, int kind) {
        vc.vertex(matrix, point.x(), point.y(), point.z())
                .color(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F))
                .uv(kind * 2.0F + Mth.clamp(u, 0.0F, 1.0F), Mth.clamp(v, 0.0F, 1.0F))
                .endVertex();
    }

    private static VertexConsumer scaled(VertexConsumer consumer, float alphaScale) {
        return Math.abs(alphaScale - 1.0F) <= 0.001F
                ? consumer : new AlphaScaleVertexConsumer(consumer, alphaScale);
    }

    private static final class AlphaScaleVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alphaScale;

        private AlphaScaleVertexConsumer(VertexConsumer delegate, float alphaScale) {
            this.delegate = delegate;
            this.alphaScale = alphaScale;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, Mth.clamp(Math.round(alpha * alphaScale), 0, 255));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, Mth.clamp(Math.round(alpha * alphaScale), 0, 255));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }


    private static PetalMesh getPetalMesh() {
        if (petalMesh == null) {
            petalMesh = PetalMesh.load(PETAL_MODEL);
        }
        return petalMesh;
    }

    @Override
    public ResourceLocation getTextureLocation(BloodfyreFrenzyEntity entity) {
        return EMPTY_TEXTURE;
    }

    private record BranchSpec(float u, float side, float length) {
    }

    private record GroundTrace(float[] mainHeights) {
    }

    private record PetalVertex(float x, float y, float z, float u, float v) {
    }

    private static final class PetalMesh {
        private static final PetalMesh EMPTY = new PetalMesh(List.of());
        private final List<PetalVertex[]> faces;

        private PetalMesh(List<PetalVertex[]> faces) {
            this.faces = faces;
        }

        private static PetalMesh load(ResourceLocation location) {
            List<float[]> positions = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<PetalVertex[]> faces = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Minecraft.getInstance().getResourceManager().open(location), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("v ")) {
                        String[] parts = line.split("\\s+");
                        positions.add(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])});
                    } else if (line.startsWith("vt ")) {
                        String[] parts = line.split("\\s+");
                        uvs.add(new float[]{Float.parseFloat(parts[1]), 1.0F - Float.parseFloat(parts[2])});
                    } else if (line.startsWith("f ")) {
                        String[] parts = line.substring(2).trim().split("\\s+");
                        if (parts.length >= 3) {
                            PetalVertex[] face = new PetalVertex[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                String[] indices = parts[i].split("/");
                                float[] position = positions.get(resolveIndex(indices[0], positions.size()));
                                float[] uv = indices.length > 1 && !indices[1].isEmpty()
                                        ? uvs.get(resolveIndex(indices[1], uvs.size())) : new float[]{0.0F, 0.0F};
                                face[i] = new PetalVertex(position[0], position[1], position[2], uv[0], uv[1]);
                            }
                            faces.add(face);
                        }
                    }
                }
            } catch (Exception exception) {
                Main.LOGGER.warn("Unable to load Bloodfyre Frenzy petal mesh {}", location, exception);
                return EMPTY;
            }
            return new PetalMesh(faces);
        }

        private static int resolveIndex(String raw, int size) {
            int index = Integer.parseInt(raw);
            return index < 0 ? size + index : index - 1;
        }

        private void render(Matrix4f matrix, VertexConsumer vc,
                            float red, float green, float blue, float alpha) {
            for (PetalVertex[] face : this.faces) {
                if (face.length == 4) {
                    emit(matrix, vc, face[0], red, green, blue, alpha);
                    emit(matrix, vc, face[1], red, green, blue, alpha);
                    emit(matrix, vc, face[2], red, green, blue, alpha);
                    emit(matrix, vc, face[3], red, green, blue, alpha);
                } else if (face.length == 3) {
                    emit(matrix, vc, face[0], red, green, blue, alpha);
                    emit(matrix, vc, face[1], red, green, blue, alpha);
                    emit(matrix, vc, face[2], red, green, blue, alpha);
                    emit(matrix, vc, face[2], red, green, blue, alpha);
                }
            }
        }

        private static void emit(Matrix4f matrix, VertexConsumer vc, PetalVertex vertex,
                                 float red, float green, float blue, float alpha) {
            BloodfyreFrenzyRenderer.vertex(vc, matrix,
                    new BloodfyreGeometry.Point(vertex.x, vertex.y, vertex.z),
                    vertex.u, vertex.v, red, green, blue, alpha, 0);
        }
    }
}

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.NarukamiDivinityEntity;
import com.wjx.kablade.slasharts.NarukamiDivinityTimeline;
import com.wjx.kablade.slasharts.NarukamiDivinityTimeline.Cue;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.wjx.kablade.client.renderer.ThunderboltCallGeometry.hash01;

/**
 * Data-driven world-space reconstruction of the supplied 41-frame
 * Narukami Divinity reference.
 */
public final class NarukamiDivinityRenderer extends EntityRenderer<NarukamiDivinityEntity> {

    private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "textures/entity/empty.png");
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private static final int BLACK_PURPLE = 0x160A22;
    private static final int DEEP_PURPLE = 0x3A126B;
    private static final int PURPLE = 0x7623C7;
    private static final int VIOLET = 0xAF50F2;
    private static final int PINK = 0xE39BFF;
    private static final int PALE = 0xF5DCFF;
    private static final int WHITE = 0xFFFFFF;

    public NarukamiDivinityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(NarukamiDivinityEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return frustum.isVisible(entity.getBoundingBoxForCulling());
    }

    @Override
    public void render(NarukamiDivinityEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (ThunderboltCallOculusPipeline.enqueue(entity, partialTick)) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        renderGeometry(entity, partialTick, poseStack.last().pose(),
                this.entityRenderDispatcher.camera.getPosition(),
                buffer.getBuffer(KabladeRenderTypes.thunderboltCallComposite()),
                buffer.getBuffer(KabladeRenderTypes.thunderboltCallEnergy()),
                buffer.getBuffer(KabladeRenderTypes.thunderboltCallCross()),
                buffer.getBuffer(KabladeRenderTypes.thunderboltCallLightning()),
                buffer.getBuffer(KabladeRenderTypes.thunderboltCallParticle()));
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Material-separated entry point shared by the normal renderer and the
     * private Oculus explicit-triangle path.
     */
    static void renderGeometry(NarukamiDivinityEntity entity, float partialTick,
                               Matrix4f matrix, Vec3 cameraPosition,
                               VertexConsumer dark, VertexConsumer energy,
                               VertexConsumer slash, VertexConsumer lightning,
                               VertexConsumer particles) {
        Vec3 entityPosition = entity.getPosition(partialTick);
        Vec3 camera = cameraPosition.subtract(entityPosition);
        Vec3 owner = entity.getOwnerAnchor(partialTick).subtract(entityPosition);
        Vec3 storedTarget = entity.getStoredTargetAnchor().subtract(entityPosition);
        Basis basis = basis(entity.getInitialForward());
        float frame = entity.getReferenceFrame(partialTick);
        long seed = entity.getSeed();

        renderOpening(matrix, dark, energy, lightning, frame, owner, basis, camera, seed);
        renderCrossSequence(matrix, dark, energy, slash, lightning, frame,
                owner, storedTarget, basis, camera, seed);
        renderCage(matrix, dark, energy, slash, lightning, frame, owner,
                basis, camera, seed);
        renderGroundField(matrix, dark, energy, lightning, frame, owner, storedTarget,
                basis, camera, seed);
        renderTimedAccentParticles(matrix, dark, particles, frame, owner, storedTarget,
                basis, camera, seed);
        renderResidueParticles(matrix, dark, particles, frame, owner, basis, camera, seed);
    }

    private static void renderOpening(Matrix4f matrix, VertexConsumer dark,
                                      VertexConsumer energy, VertexConsumer lightning,
                                      float frame, Vec3 owner, Basis basis,
                                      Vec3 camera, long seed) {
        float alpha = NarukamiDivinityTimeline.plateau(frame, 0.0F, 0.18F, 3.55F, 5.0F);
        if (alpha <= 0.001F) {
            return;
        }
        Vec3 center = owner.add(WORLD_UP.scale(1.18D));
        float impact = NarukamiDivinityTimeline.gaussian(frame, 0.65F, 0.62F);
        float secondary = NarukamiDivinityTimeline.gaussian(frame, 2.15F, 0.86F) * 0.55F;
        float pulse = Math.min(1.0F, impact + secondary);
        float radius = 0.34F + pulse * 1.62F;

        ThunderboltCallGeometry.discBillboard(dark, matrix, center, camera,
                radius * 1.52F, radius * 1.52F, frame * 0.09F,
                PURPLE, alpha * (0.28F + pulse * 0.34F));
        ThunderboltCallGeometry.discBillboard(energy, matrix, center, camera,
                radius, radius, -frame * 0.13F,
                PINK, alpha * (0.42F + pulse * 0.58F));
        ThunderboltCallGeometry.starBurst(energy, matrix, center, camera,
                18, 1.05F, 4.05F, 0.082F, VIOLET,
                alpha * (0.52F + pulse * 0.48F), seed + 91L, frame * 0.035F);
        ThunderboltCallGeometry.starBurst(energy, matrix, center, camera,
                16, 0.88F, 3.62F, 0.034F, PALE,
                alpha * (0.60F + pulse * 0.40F), seed + 101L, frame * 0.035F);

        ThunderboltCallGeometry.beam(dark, matrix,
                center.subtract(basis.right.scale(5.1D)),
                center.add(basis.right.scale(5.1D)),
                0.15F, camera, PURPLE, alpha * 0.68F);
        ThunderboltCallGeometry.beam(energy, matrix,
                center.subtract(basis.right.scale(4.65D)),
                center.add(basis.right.scale(4.65D)),
                0.030F, camera, PALE, alpha);

        long dynamicSeed = seed + 170L + (long) (frame * 5.0F) * 101L;
        for (int i = 0; i < 15; i++) {
            double angle = i * Mth.TWO_PI / 15.0D
                    + (hash01(seed + i * 29L) - 0.5D) * 0.30D;
            double horizontal = 0.90D + hash01(seed + 220L + i * 31L) * 3.15D;
            Vec3 direction = basis.right.scale(Math.cos(angle) * horizontal)
                    .add(WORLD_UP.scale(Math.sin(angle) * (0.78D + i % 4 * 0.30D)))
                    .add(basis.forward.scale((hash01(seed + 260L + i * 37L) - 0.5D) * 2.0D));
            float flicker = 0.30F + 0.70F * sineSquared(frame * 4.2F + i * 1.31F);
            ThunderboltCallGeometry.lightning(dark, lightning, matrix, center,
                    center.add(direction), 8 + i % 5, 0.20F + i % 3 * 0.038F,
                    0.022F + i % 2 * 0.006F, camera, PURPLE,
                    i % 4 == 0 ? PALE : PINK, alpha * flicker,
                    dynamicSeed + i * 127L);
        }
    }

    private static void renderCrossSequence(Matrix4f matrix, VertexConsumer dark,
                                            VertexConsumer energy, VertexConsumer slash,
                                            VertexConsumer lightning,
                                            float frame, Vec3 owner, Vec3 target,
                                            Basis basis, Vec3 camera, long seed) {
        float forwardTravel = crossForwardTravel(frame);
        float previousTravel = crossForwardTravel(frame - 0.55F);
        float motionLag = 0.22F + Math.max(0.0F, forwardTravel - previousTravel) * 1.35F;
        Vec3 crossCenter = target.add(basis.forward.scale(forwardTravel));
        Vec3 echoCenter = crossCenter.subtract(basis.forward.scale(motionLag));
        ThunderboltCallGeometry.Curve crossA = crossCurve(crossCenter, basis, false);
        ThunderboltCallGeometry.Curve crossB = crossCurve(crossCenter, basis, true);
        ThunderboltCallGeometry.Curve echoA = crossCurve(echoCenter, basis, false);
        ThunderboltCallGeometry.Curve echoB = crossCurve(echoCenter, basis, true);

        float alphaA = NarukamiDivinityTimeline.plateau(frame, 4.8F, 5.8F, 13.0F, 16.0F);
        float alphaB = NarukamiDivinityTimeline.plateau(frame, 5.35F, 6.25F, 13.4F, 16.2F);
        float headA = NarukamiDivinityTimeline.smooth(frame, 4.8F, 6.85F);
        float headB = NarukamiDivinityTimeline.smooth(frame, 5.35F, 7.45F);
        float tailA = 0.90F * NarukamiDivinityTimeline.smooth(frame, 11.8F, 15.8F);
        float tailB = 0.90F * NarukamiDivinityTimeline.smooth(frame, 12.2F, 16.0F);
        float erodeA = NarukamiDivinityTimeline.smooth(frame, 12.0F, 16.0F);
        float erodeB = NarukamiDivinityTimeline.smooth(frame, 12.4F, 16.2F);
        renderCrossMotionEcho(matrix, dark, slash, echoA, headA, tailA,
                alphaA, erodeA, camera, seed + 481L, frame, 0.46F);
        renderCrossMotionEcho(matrix, dark, slash, echoB, headB, tailB,
                alphaB, erodeB, camera, seed + 581L, frame + 4.0F, 0.43F);
        renderSlashLayers(matrix, dark, slash, energy, crossA, headA, tailA,
                alphaA, erodeA,
                camera, seed + 501L, frame, 0.46F, true);
        renderSlashLayers(matrix, dark, slash, energy, crossB, headB, tailB,
                alphaB, erodeB,
                camera, seed + 601L, frame + 4.0F, 0.43F, true);

        float crossPulse = Mth.clamp(
                NarukamiDivinityTimeline.gaussian(frame, 6.70F, 0.52F) * 0.82F
                        + NarukamiDivinityTimeline.gaussian(frame, 7.35F, 0.58F),
                0.0F, 1.0F);
        if (crossPulse > 0.002F) {
            ThunderboltCallGeometry.discBillboard(dark, matrix, crossCenter, camera,
                    1.55F + crossPulse * 1.10F, 1.55F + crossPulse * 1.10F,
                    frame * 0.06F, DEEP_PURPLE, crossPulse * 0.24F);
            ThunderboltCallGeometry.discBillboard(energy, matrix, crossCenter, camera,
                    0.72F + crossPulse * 0.58F, 0.72F + crossPulse * 0.58F,
                    -frame * 0.09F, PINK, crossPulse * 0.44F);
            ThunderboltCallGeometry.starBurst(energy, matrix, crossCenter, camera,
                    10, 0.48F, 2.15F, 0.026F, PALE,
                    crossPulse * 0.76F, seed + 651L, frame * 0.08F);
            long dynamicSeed = seed + 670L + (long) (frame * 5.0F) * 83L;
            for (int i = 0; i < 8; i++) {
                double angle = i * Mth.TWO_PI / 8.0D
                        + (hash01(seed + 680L + i * 17L) - 0.5D) * 0.35D;
                Vec3 end = crossCenter
                        .add(basis.right.scale(Math.cos(angle) * (1.3D + i % 3 * 0.34D)))
                        .add(WORLD_UP.scale(Math.sin(angle) * (0.9D + i % 2 * 0.30D)))
                        .add(basis.forward.scale((hash01(seed + 700L + i * 23L) - 0.5D) * 1.2D));
                ThunderboltCallGeometry.lightning(dark, lightning, matrix,
                        crossCenter, end, 6 + i % 3, 0.14F, 0.014F,
                        camera, PURPLE, i % 3 == 0 ? PALE : VIOLET,
                        crossPulse * (0.46F + 0.34F
                                * sineSquared(frame * 4.1F + i)),
                        dynamicSeed + i * 97L);
            }
        }

        float ringAlpha = NarukamiDivinityTimeline.plateau(
                frame, 5.15F, 6.0F, 10.5F, 13.1F);
        if (ringAlpha > 0.001F) {
            Vec3 center = owner.add(WORLD_UP.scale(1.70D))
                    .subtract(basis.forward.scale(0.25D));
            Vec3 axisU = localVector(basis, -4.42D, 0.49D, 0.93D);
            Vec3 axisV = localVector(basis, 0.37D, -0.26D, 1.75D);
            Vec3 normal = axisU.cross(axisV).normalize();
            float aoePulse = Mth.clamp(
                    NarukamiDivinityTimeline.gaussian(frame, 7.35F, 0.78F)
                            + NarukamiDivinityTimeline.gaussian(frame, 9.0F, 1.05F) * 0.55F,
                    0.0F, 1.0F);
            ThunderboltCallGeometry.discOriented(dark, matrix, center,
                    axisU, axisV, 4.75F, 1.92F,
                    PURPLE, ringAlpha * aoePulse * 0.10F);
            ThunderboltCallGeometry.discOriented(energy, matrix, center,
                    axisU, axisV, 4.48F, 1.75F,
                    VIOLET, ringAlpha * aoePulse * 0.055F);
            ThunderboltCallGeometry.Curve ring = u -> {
                double angle = Mth.lerp(u, -Math.PI, Math.PI * 0.5D);
                return center.add(axisU.scale(Math.cos(angle)))
                        .add(axisV.scale(Math.sin(angle)));
            };
            float head = NarukamiDivinityTimeline.smooth(frame, 5.45F, 7.35F);
            float tail = 0.94F * NarukamiDivinityTimeline.smooth(frame, 9.0F, 12.65F);
            ThunderboltCallGeometry.ribbonFixed(dark, matrix, ring, 64, head, tail,
                    0.34F, normal, BLACK_PURPLE, ringAlpha * 0.62F,
                    NarukamiDivinityTimeline.smooth(frame, 10.0F, 13.0F) * 0.42F,
                    seed + 701L, frame);
            ThunderboltCallGeometry.ribbonFixed(slash, matrix, ring, 64, head, tail * 0.94F,
                    0.20F, normal, VIOLET, ringAlpha * 0.94F,
                    NarukamiDivinityTimeline.smooth(frame, 10.0F, 13.0F) * 0.60F,
                    seed + 702L, frame + 1.2F);
            ThunderboltCallGeometry.ribbonFixed(energy, matrix, ring, 64, head, tail * 0.86F,
                    0.052F, normal, PALE, ringAlpha,
                    NarukamiDivinityTimeline.smooth(frame, 10.0F, 13.0F) * 0.74F,
                    seed + 703L, frame + 2.4F);
        }
    }

    private static void renderCage(Matrix4f matrix, VertexConsumer dark,
                                   VertexConsumer energy, VertexConsumer slash,
                                   VertexConsumer lightning, float frame, Vec3 owner,
                                   Basis basis, Vec3 camera, long seed) {
        if (frame < 13.5F || frame > 29.2F) {
            return;
        }
        Vec3 center = owner.add(WORLD_UP.scale(1.58D))
                .subtract(basis.forward.scale(0.05D));

        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.ENTRY_FRONT,
                orbit(center, basis, -75.0D, 75.0D, 3.68D, 2.42D,
                        -0.18D, 0.02D, 0.04D, 0.040D, 0.025D),
                camera, seed + 1001L, 0.205F);
        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.ENTRY_UPPER,
                orbit(center, basis, 100.0D, 240.0D, 3.32D, 2.20D,
                        0.24D, 0.40D, 0.035D, 0.030D, 0.020D),
                camera, seed + 1031L, 0.165F);
        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.ORBIT_A,
                orbit(center, basis, 116.0D, 246.0D, 3.22D, 2.08D,
                        0.48D, 0.12D, 0.09D, 0.025D, 0.045D),
                camera, seed + 1061L, 0.095F);
        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.ORBIT_B,
                orbit(center, basis, 280.0D, 400.0D, 3.36D, 2.18D,
                        0.04D, 0.40D, 0.08D, 0.018D, -0.040D),
                camera, seed + 1091L, 0.085F);
        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.ORBIT_C,
                orbit(center, basis, 298.0D, 414.0D, 3.06D, 2.00D,
                        -0.30D, 0.10D, 0.07D, 0.020D, 0.035D),
                camera, seed + 1121L, 0.075F);

        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.RING_BACK_A,
                orbit(center, basis, 120.0D, 260.0D, 3.28D, 2.12D,
                        0.20D, 0.02D, 0.10D, 0.025D, 0.040D),
                camera, seed + 1151L, 0.070F);
        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.RING_BACK_B,
                orbit(center, basis, -66.0D, 76.0D, 3.12D, 2.02D,
                        0.34D, 0.24D, 0.08D, 0.020D, -0.035D),
                camera, seed + 1181L, 0.065F);
        renderTrack(matrix, dark, slash, frame, NarukamiDivinityTimeline.RING_BACK_C,
                orbit(center, basis, 100.0D, 240.0D, 3.42D, 2.20D,
                        -0.08D, 0.14D, 0.07D, 0.018D, 0.030D),
                camera, seed + 1211L, 0.060F);

        Cue dominant = NarukamiDivinityTimeline.dominantCue(frame);
        renderPierce(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.PIERCE_VERTICAL,
                axis(center, basis, 0.76D, -0.30D, -0.46D,
                        0.82D, 0.78D, -0.43D, 0.03D, -0.03D),
                axis(center, basis, 0.80D, -1.20D, -0.50D,
                        0.92D, 1.58D, -0.42D, 0.07D, -0.10D),
                axis(center, basis, 0.72D, -1.08D, -0.56D,
                        1.02D, 1.46D, -0.34D, 0.11D, -0.14D),
                camera, seed + 1401L, 0.180F, dominant);
        renderPierce(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.PIERCE_RISING,
                orbit(center, basis, -165.0D, -130.0D, 3.05D, 2.02D,
                        -0.72D, -0.18D, 0.08D, 0.020D, 0.025D),
                orbit(center, basis, -165.0D, -25.0D, 3.28D, 2.20D,
                        -1.02D, 1.08D, 0.18D, 0.055D, 0.050D),
                orbit(center, basis, -155.0D, -16.0D, 3.38D, 2.26D,
                        -0.88D, 1.18D, 0.14D, 0.040D, 0.035D),
                camera, seed + 1501L, 0.205F, dominant);
        renderPierce(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.PIERCE_HORIZONTAL,
                orbit(center, basis, -58.0D, -22.0D, 3.30D, 2.18D,
                        -0.10D, -0.04D, 0.05D, 0.020D, 0.020D),
                orbit(center, basis, -58.0D, 66.0D, 3.58D, 2.34D,
                        -0.16D, 0.18D, 0.12D, 0.045D, -0.040D),
                orbit(center, basis, -48.0D, 74.0D, 3.66D, 2.40D,
                        -0.08D, 0.26D, 0.08D, 0.030D, -0.025D),
                camera, seed + 1601L, 0.165F, dominant);
        renderPierce(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.PIERCE_FALLING,
                orbit(center, basis, 44.0D, 80.0D, 3.10D, 2.08D,
                        0.74D, 0.22D, 0.06D, 0.020D, 0.020D),
                orbit(center, basis, 44.0D, 166.0D, 3.36D, 2.28D,
                        1.18D, -1.02D, 0.16D, 0.052D, -0.045D),
                orbit(center, basis, 54.0D, 176.0D, 3.44D, 2.34D,
                        1.06D, -0.90D, 0.12D, 0.035D, -0.030D),
                camera, seed + 1701L, 0.180F, dominant);
        renderPierce(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.PIERCE_OFFSET,
                orbit(center, basis, 146.0D, 182.0D, 2.96D, 2.00D,
                        -0.52D, -0.12D, 0.05D, 0.020D, 0.020D),
                orbit(center, basis, 146.0D, 256.0D, 3.22D, 2.18D,
                        -0.88D, 0.78D, 0.15D, 0.050D, 0.045D),
                orbit(center, basis, 156.0D, 264.0D, 3.30D, 2.24D,
                        -0.78D, 0.86D, 0.10D, 0.035D, 0.030D),
                camera, seed + 1801L, 0.145F, dominant);

        ThunderboltCallGeometry.Curve collapseMain =
                orbit(center, basis, 100.0D, 250.0D, 3.58D, 2.34D,
                        -0.68D, -0.46D, 0.08D, 0.035D, 0.030D);
        ThunderboltCallGeometry.Curve collapseEcho =
                orbit(center, basis, 110.0D, 230.0D, 3.28D, 2.14D,
                        -0.72D, -0.52D, 0.055D, 0.020D, -0.025D);
        renderSingleCue(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.COLLAPSE_MAIN, collapseMain,
                camera, seed + 1901L, 0.255F, dominant);
        renderTrack(matrix, dark, slash, frame,
                NarukamiDivinityTimeline.COLLAPSE_ECHO, collapseEcho,
                camera, seed + 1941L, 0.135F);

        ThunderboltCallGeometry.Curve residualDiagonal = axis(center, basis,
                -5.35D, -1.95D, -0.10D,
                5.65D, 3.15D, 0.10D, 0.0D, -0.18D);
        renderSingleCue(matrix, dark, energy, slash, frame,
                NarukamiDivinityTimeline.RESIDUAL_DIAGONAL, residualDiagonal,
                camera, seed + 1981L, 0.075F, dominant);
        renderTrack(matrix, dark, slash, frame,
                NarukamiDivinityTimeline.RESIDUAL_HOOK,
                orbit(center, basis, 150.0D, 260.0D, 3.02D, 2.00D,
                        -0.72D, -0.28D, 0.09D, 0.020D, 0.025D),
                camera, seed + 2021L, 0.080F);

        float electricityAlpha = NarukamiDivinityTimeline.plateau(
                frame, 14.0F, 15.2F, 25.8F, 29.0F);
        long dynamicSeed = seed + 2300L + (long) (frame * 4.0F) * 131L;
        for (int i = 0; i < 14 && electricityAlpha > 0.001F; i++) {
            double angleA = hash01(seed + i * 37L) * Mth.TWO_PI;
            double angleB = angleA + 0.55D + hash01(seed + 80L + i * 41L) * 0.80D;
            Vec3 start = center.add(basis.right.scale(Math.cos(angleA) * (2.4D + i % 4 * 0.25D)))
                    .add(basis.forward.scale(Math.sin(angleA) * (1.55D + i % 3 * 0.20D)))
                    .add(WORLD_UP.scale(-0.85D + hash01(seed + 120L + i * 43L) * 2.1D));
            Vec3 end = center.add(basis.right.scale(Math.cos(angleB) * (2.8D + i % 3 * 0.24D)))
                    .add(basis.forward.scale(Math.sin(angleB) * (1.80D + i % 4 * 0.16D)))
                    .add(WORLD_UP.scale(-0.75D + hash01(seed + 160L + i * 47L) * 2.0D));
            float flicker = 0.24F + 0.76F * sineSquared(frame * 3.6F + i * 1.17F);
            ThunderboltCallGeometry.lightning(dark, lightning, matrix, start, end,
                    7 + i % 4, 0.16F, 0.017F + i % 2 * 0.004F,
                    camera, PURPLE, i % 5 == 0 ? PALE : VIOLET,
                    electricityAlpha * flicker * 0.72F,
                    dynamicSeed + i * 149L);
        }
    }

    private static void renderGroundField(Matrix4f matrix, VertexConsumer dark,
                                          VertexConsumer energy, VertexConsumer lightning,
                                          float frame, Vec3 owner, Vec3 storedTarget,
                                          Basis basis, Vec3 camera, long seed) {
        float alpha = NarukamiDivinityTimeline.plateau(frame, 13.4F, 14.4F, 33.6F, 40.0F);
        if (alpha > 0.001F) {
            Vec3 ground = owner.add(WORLD_UP.scale(0.035D));
            float pulse = groundPulse(frame);
            ThunderboltCallGeometry.discOriented(dark, matrix, ground,
                    basis.right, basis.forward, 4.2F + pulse * 0.55F,
                    3.2F + pulse * 0.40F, DEEP_PURPLE, alpha * (0.055F + pulse * 0.10F));
            for (int i = 0; i < 4; i++) {
                int ringIndex = i;
                float radius = 1.55F + i * 0.78F + pulse * (0.12F + i * 0.045F);
                ThunderboltCallGeometry.Curve ring = u -> {
                    double angle = u * Mth.TWO_PI + ringIndex * 0.37D;
                    return ground.add(basis.right.scale(Math.cos(angle) * radius))
                            .add(basis.forward.scale(Math.sin(angle) * radius * 0.76D));
                };
                ThunderboltCallGeometry.ribbonFixed(i == 3 ? dark : energy, matrix,
                        ring, 56, 0.94F, 0.05F + i * 0.09F, 0.018F + i * 0.004F,
                        WORLD_UP, i == 3 ? DEEP_PURPLE : VIOLET,
                        alpha * (0.26F - i * 0.035F + pulse * 0.18F), 0.24F,
                        seed + 2600L + i * 31L, frame);
            }

            long dynamicSeed = seed + 2800L + (long) (frame * 3.0F) * 97L;
            for (int i = 0; i < 11; i++) {
                double angle = i * Mth.TWO_PI / 11.0D
                        + (hash01(seed + 2820L + i * 19L) - 0.5D) * 0.36D;
                double length = 1.6D + hash01(seed + 2860L + i * 23L) * 3.4D;
                Vec3 end = ground.add(basis.right.scale(Math.cos(angle) * length))
                        .add(basis.forward.scale(Math.sin(angle) * length * 0.72D));
                float flicker = 0.24F + 0.76F * sineSquared(frame * 3.0F + i * 1.37F);
                ThunderboltCallGeometry.lightning(dark, lightning, matrix, ground, end,
                        8 + i % 4, 0.14F, 0.015F + i % 2 * 0.004F,
                        camera, DEEP_PURPLE, i % 4 == 0 ? PALE : VIOLET,
                        alpha * flicker * (0.40F + pulse * 0.42F),
                        dynamicSeed + i * 113L);
            }
        }

        float horizon = NarukamiDivinityTimeline.plateau(frame, 34.2F, 35.0F, 37.0F, 40.0F);
        if (horizon > 0.001F) {
            Vec3 center = storedTarget.subtract(WORLD_UP.scale(0.55D))
                    .subtract(basis.forward.scale(2.9D));
            ThunderboltCallGeometry.beam(dark, matrix,
                    center.subtract(basis.right.scale(8.6D)),
                    center.add(basis.right.scale(8.6D)), 0.13F,
                    camera, DEEP_PURPLE, horizon * 0.46F);
            ThunderboltCallGeometry.beam(energy, matrix,
                    center.subtract(basis.right.scale(7.8D)),
                    center.add(basis.right.scale(7.8D)), 0.025F,
                    camera, PALE, horizon * 0.72F);
        }
    }

    /**
     * Short-lived accents modeled after Thunderbolt Call's deterministic particle
     * batches. Each group only exists around its own cue instead of filling the
     * complete 41-frame presentation.
     */
    private static void renderTimedAccentParticles(Matrix4f matrix, VertexConsumer dark,
                                                   VertexConsumer particles, float frame,
                                                   Vec3 owner, Vec3 storedTarget,
                                                   Basis basis, Vec3 camera, long seed) {
        Vec3 openingCenter = owner.add(WORLD_UP.scale(1.18D));
        float openingAlpha = NarukamiDivinityTimeline.plateau(
                frame, 0.0F, 0.20F, 3.9F, 5.8F);
        for (int i = 0; i < 34 && openingAlpha > 0.001F; i++) {
            float birth = hash01(seed + 4000L + i * 17L) * 1.15F;
            float age = frame - birth;
            float life = 2.8F + hash01(seed + 4040L + i * 19L) * 2.5F;
            float born = NarukamiDivinityTimeline.smooth(age, 0.0F, 0.18F);
            float fade = 1.0F - NarukamiDivinityTimeline.smooth(age, life * 0.50F, life);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            double angle = hash01(seed + 4080L + i * 23L) * Mth.TWO_PI;
            double planarSpeed = 0.22D + hash01(seed + 4120L + i * 29L) * 0.38D;
            double verticalSpeed = (hash01(seed + 4160L + i * 31L) - 0.42D) * 0.42D;
            Vec3 velocity = basis.right.scale(Math.cos(angle) * planarSpeed)
                    .add(basis.forward.scale(Math.sin(angle) * planarSpeed * 0.74D))
                    .add(WORLD_UP.scale(verticalSpeed));
            Vec3 position = openingCenter.add(velocity.scale(age))
                    .add(WORLD_UP.scale(-age * age * 0.006D));
            float size = 0.025F + i % 5 * 0.005F;
            ThunderboltCallGeometry.billboard(i % 9 == 0 ? dark : particles,
                    matrix, position, camera, size, size * (1.55F + i % 3 * 0.24F),
                    (float) angle + age * 0.22F,
                    i % 9 == 0 ? DEEP_PURPLE : (i % 4 == 0 ? PALE : VIOLET),
                    openingAlpha * born * fade * (0.68F
                            + 0.32F * sineSquared(frame * 3.5F + i)));
        }

        float crossAlpha = NarukamiDivinityTimeline.plateau(
                frame, 4.65F, 5.25F, 14.0F, 16.7F);
        Vec3 crossCenter = storedTarget.add(basis.forward.scale(crossForwardTravel(frame)));
        ThunderboltCallGeometry.Curve crossA = crossCurve(crossCenter, basis, false);
        ThunderboltCallGeometry.Curve crossB = crossCurve(crossCenter, basis, true);
        float crossHeadA = NarukamiDivinityTimeline.smooth(frame, 4.8F, 6.85F);
        float crossHeadB = NarukamiDivinityTimeline.smooth(frame, 5.35F, 7.45F);
        float crossTailA = 0.90F * NarukamiDivinityTimeline.smooth(frame, 11.8F, 15.8F);
        float crossTailB = 0.90F * NarukamiDivinityTimeline.smooth(frame, 12.2F, 16.0F);
        for (int i = 0; i < 44 && crossAlpha > 0.001F; i++) {
            boolean reverse = (i & 1) != 0;
            float u = hash01(seed + 4300L + i * 23L);
            float head = reverse ? crossHeadB : crossHeadA;
            float tail = reverse ? crossTailB : crossTailA;
            if (u > head || u < tail) {
                continue;
            }
            float birth = 4.75F + hash01(seed + 4340L + i * 29L) * 4.2F;
            float age = frame - birth;
            float life = 4.5F + hash01(seed + 4380L + i * 31L) * 4.0F;
            float fade = 1.0F - NarukamiDivinityTimeline.smooth(age, life * 0.45F, life);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            Vec3 base = (reverse ? crossB : crossA).point(u);
            double sideJitter = (hash01(seed + 4420L + i * 37L) - 0.5D) * 0.22D;
            double rise = (hash01(seed + 4460L + i * 41L) - 0.36D) * 0.055D * age;
            Vec3 position = base
                    .subtract(basis.forward.scale(age * (0.055D + i % 4 * 0.012D)))
                    .add(basis.right.scale(sideJitter))
                    .add(WORLD_UP.scale(rise));
            float size = 0.018F + i % 6 * 0.004F;
            ThunderboltCallGeometry.billboard(particles, matrix, position, camera,
                    size, size * (1.45F + i % 3 * 0.25F),
                    age * (0.32F + i % 5 * 0.08F),
                    i % 6 == 0 ? PALE : (i % 3 == 0 ? PINK : VIOLET),
                    crossAlpha * fade * (0.52F
                            + 0.48F * sineSquared(frame * 3.0F + i * 0.8F)));
        }

        float ringAlpha = NarukamiDivinityTimeline.plateau(
                frame, 5.15F, 6.0F, 10.5F, 13.1F);
        if (ringAlpha > 0.001F) {
            Vec3 ringCenter = owner.add(WORLD_UP.scale(1.70D))
                    .subtract(basis.forward.scale(0.25D));
            Vec3 axisU = localVector(basis, -4.42D, 0.49D, 0.93D);
            Vec3 axisV = localVector(basis, 0.37D, -0.26D, 1.75D);
            float head = NarukamiDivinityTimeline.smooth(frame, 5.45F, 7.35F);
            float tail = 0.94F * NarukamiDivinityTimeline.smooth(frame, 9.0F, 12.65F);
            for (int i = 0; i < 30; i++) {
                float u = (i + 0.35F) / 30.0F;
                if (u > head || u < tail) {
                    continue;
                }
                double angle = Mth.lerp(u, -Math.PI, Math.PI * 0.5D);
                Vec3 position = ringCenter.add(axisU.scale(Math.cos(angle)))
                        .add(axisV.scale(Math.sin(angle)))
                        .add(WORLD_UP.scale((hash01(seed + 4600L + i * 17L) - 0.5D) * 0.10D));
                float flicker = 0.38F + 0.62F
                        * sineSquared(frame * 3.7F + i * 1.41F);
                float size = 0.022F + i % 4 * 0.005F;
                ThunderboltCallGeometry.billboard(particles, matrix, position, camera,
                        size, size * 1.42F, (float) angle,
                        i % 7 == 0 ? PALE : VIOLET,
                        ringAlpha * flicker * 0.82F);
            }
        }

        Vec3 cageCenter = owner.add(WORLD_UP.scale(1.58D))
                .subtract(basis.forward.scale(0.05D));
        float cageAlpha = NarukamiDivinityTimeline.plateau(
                frame, 13.9F, 14.7F, 25.8F, 29.2F);
        for (int i = 0; i < 38 && cageAlpha > 0.001F; i++) {
            float birth = 14.2F + hash01(seed + 4800L + i * 17L) * 10.2F;
            float age = frame - birth;
            float life = 3.8F + hash01(seed + 4840L + i * 19L) * 4.8F;
            float fade = 1.0F - NarukamiDivinityTimeline.smooth(age, life * 0.48F, life);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            double angle = hash01(seed + 4880L + i * 23L) * Mth.TWO_PI;
            double radius = 2.35D + hash01(seed + 4920L + i * 29L) * 1.35D;
            Vec3 radial = basis.right.scale(Math.cos(angle))
                    .add(basis.forward.scale(Math.sin(angle) * 0.68D));
            Vec3 position = cageCenter.add(radial.scale(radius))
                    .add(WORLD_UP.scale((hash01(seed + 4960L + i * 31L) - 0.5D) * 2.25D))
                    .add(radial.scale(age * (0.018D + i % 4 * 0.006D)))
                    .add(WORLD_UP.scale(age * 0.018D));
            float size = 0.020F + i % 5 * 0.004F;
            ThunderboltCallGeometry.billboard(particles, matrix, position, camera,
                    size, size * (1.25F + i % 4 * 0.22F),
                    age * (0.25F + i % 6 * 0.07F),
                    i % 8 == 0 ? PALE : (i % 3 == 0 ? PINK : VIOLET),
                    cageAlpha * fade * (0.46F
                            + 0.54F * sineSquared(frame * 3.2F + i)));
        }

        float collapseAlpha = NarukamiDivinityTimeline.plateau(
                frame, 19.8F, 20.8F, 29.0F, 34.2F);
        Vec3 ground = owner.add(WORLD_UP.scale(0.055D));
        for (int i = 0; i < 28 && collapseAlpha > 0.001F; i++) {
            float birth = 20.0F + hash01(seed + 5200L + i * 17L) * 4.8F;
            float age = frame - birth;
            float life = 6.5F + hash01(seed + 5240L + i * 19L) * 6.2F;
            float fade = 1.0F - NarukamiDivinityTimeline.smooth(age, life * 0.50F, life);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            double angle = hash01(seed + 5280L + i * 23L) * Mth.TWO_PI;
            double radius = 1.3D + hash01(seed + 5320L + i * 29L) * 2.2D;
            Vec3 radial = basis.right.scale(Math.cos(angle))
                    .add(basis.forward.scale(Math.sin(angle) * 0.74D));
            Vec3 position = ground.add(radial.scale(radius + age * 0.075D))
                    .add(WORLD_UP.scale(0.08D + age * 0.045D - age * age * 0.004D));
            float size = 0.024F + i % 5 * 0.005F;
            ThunderboltCallGeometry.billboard(i % 9 == 0 ? dark : particles,
                    matrix, position, camera, size, size * (1.35F + i % 3 * 0.28F),
                    (float) angle + age * 0.24F,
                    i % 9 == 0 ? BLACK_PURPLE : (i % 5 == 0 ? PALE : VIOLET),
                    collapseAlpha * fade * (0.54F
                            + 0.46F * sineSquared(frame * 2.8F + i * 1.1F)));
        }
    }

    private static void renderResidueParticles(Matrix4f matrix, VertexConsumer dark,
                                               VertexConsumer particles, float frame,
                                               Vec3 owner, Basis basis,
                                               Vec3 camera, long seed) {
        float alpha = NarukamiDivinityTimeline.plateau(frame, 13.6F, 15.0F, 32.0F, 39.0F);
        for (int i = 0; i < 72; i++) {
            float birth = 13.8F + hash01(seed + 3100L + i * 17L) * 12.0F;
            float life = 6.0F + hash01(seed + 3140L + i * 19L) * 11.0F;
            float age = frame - birth;
            float fade = 1.0F - NarukamiDivinityTimeline.smooth(age, life * 0.55F, life);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            double angle = hash01(seed + 3180L + i * 23L) * Mth.TWO_PI;
            double radius = 0.5D + hash01(seed + 3220L + i * 29L) * 3.4D;
            Vec3 origin = owner
                    .add(basis.right.scale(Math.cos(angle) * radius))
                    .add(basis.forward.scale(Math.sin(angle) * radius * 0.70D))
                    .add(WORLD_UP.scale(hash01(seed + 3260L + i * 31L) * 1.8D));
            Vec3 position = origin
                    .add(basis.right.scale((hash01(seed + 3300L + i * 37L) - 0.5D) * age * 0.08D))
                    .add(basis.forward.scale((hash01(seed + 3340L + i * 41L) - 0.5D) * age * 0.07D))
                    .add(WORLD_UP.scale(age * (0.025D + i % 5 * 0.007D) - age * age * 0.0025D));
            float size = 0.025F + i % 6 * 0.004F;
            ThunderboltCallGeometry.billboard(i % 7 == 0 ? dark : particles,
                    matrix, position, camera, size, size * (1.2F + i % 4 * 0.28F),
                    age * (0.22F + i % 5 * 0.07F),
                    i % 7 == 0 ? BLACK_PURPLE : (i % 5 == 0 ? PALE : VIOLET),
                    alpha * fade * (0.58F + 0.42F * sineSquared(frame * 2.4F + i)));
        }
    }

    private static void renderPierce(Matrix4f matrix, VertexConsumer dark,
                                     VertexConsumer energy, VertexConsumer slash,
                                     float frame, Cue cue,
                                     ThunderboltCallGeometry.Curve seedCurve,
                                     ThunderboltCallGeometry.Curve impactCurve,
                                     ThunderboltCallGeometry.Curve handoffCurve,
                                     Vec3 camera, long seed, float width, Cue dominant) {
        float approach = NarukamiDivinityTimeline.smooth(frame, cue.start(), cue.impact());
        float depart = NarukamiDivinityTimeline.smooth(frame, cue.handoff(), cue.release());
        ThunderboltCallGeometry.Curve curve = u ->
                seedCurve.point(u).lerp(impactCurve.point(u), approach)
                        .lerp(handoffCurve.point(u), depart);
        float precursor = NarukamiDivinityTimeline.precursor(cue, frame);
        if (precursor > 0.001F) {
            ThunderboltCallGeometry.ribbon(slash, matrix, seedCurve, 56,
                    NarukamiDivinityTimeline.reveal(cue, frame), 0.0F,
                    width * 0.35F, camera, PURPLE, precursor, 0.20F,
                    seed - 7L, frame);
        }
        renderSingleCue(matrix, dark, energy, slash, frame, cue, curve,
                camera, seed, width, dominant);
        float wake = NarukamiDivinityTimeline.wake(cue, frame);
        if (wake > 0.001F) {
            ThunderboltCallGeometry.ribbon(dark, matrix, handoffCurve, 56,
                    1.0F, NarukamiDivinityTimeline.exit(cue, frame),
                    width * 0.58F, camera, DEEP_PURPLE, wake, 0.42F,
                    seed + 9L, frame + 2.0F);
        }
    }

    private static void renderSingleCue(Matrix4f matrix, VertexConsumer dark,
                                        VertexConsumer energy, VertexConsumer slash,
                                        float frame, Cue cue,
                                        ThunderboltCallGeometry.Curve curve,
                                        Vec3 camera, long seed, float width, Cue dominant) {
        float alpha = NarukamiDivinityTimeline.envelope(cue, frame);
        if (alpha <= 0.001F) {
            return;
        }
        float head = NarukamiDivinityTimeline.reveal(cue, frame);
        float tail = NarukamiDivinityTimeline.exit(cue, frame);
        float erode = NarukamiDivinityTimeline.smooth(frame, cue.release(), cue.end());
        ThunderboltCallGeometry.ribbon(dark, matrix, curve, 64, head, tail,
                width * 1.70F, camera, BLACK_PURPLE, alpha * 0.62F,
                erode * 0.40F, seed, frame);
        ThunderboltCallGeometry.ribbon(slash, matrix, curve, 64, head, tail * 0.95F,
                width, camera, VIOLET, alpha * 0.94F,
                erode * 0.60F, seed + 1L, frame + 1.1F);
        ThunderboltCallGeometry.ribbon(energy, matrix, curve, 64, head, tail * 0.88F,
                width * (dominant == cue ? 0.24F : 0.12F), camera,
                dominant == cue ? WHITE : PINK,
                alpha * (dominant == cue ? 1.0F : 0.55F),
                erode * 0.76F, seed + 2L, frame + 2.2F);
    }

    private static void renderTrack(Matrix4f matrix, VertexConsumer dark,
                                    VertexConsumer slash, float frame, Cue cue,
                                    ThunderboltCallGeometry.Curve curve,
                                    Vec3 camera, long seed, float width) {
        float alpha = NarukamiDivinityTimeline.envelope(cue, frame);
        if (alpha <= 0.001F) {
            return;
        }
        float head = NarukamiDivinityTimeline.reveal(cue, frame);
        float tail = NarukamiDivinityTimeline.exit(cue, frame);
        float erode = 0.25F + NarukamiDivinityTimeline.smooth(frame, cue.release(), cue.end()) * 0.50F;
        ThunderboltCallGeometry.ribbon(dark, matrix, curve, 56, head, tail,
                width * 1.50F, camera, BLACK_PURPLE, alpha * 0.48F,
                erode * 0.52F, seed, frame);
        ThunderboltCallGeometry.ribbon(slash, matrix, curve, 56, head, tail * 0.93F,
                width, camera, PURPLE, alpha * 0.80F,
                erode * 0.72F, seed + 1L, frame + 1.7F);
    }

    private static void renderSlashLayers(Matrix4f matrix, VertexConsumer dark,
                                          VertexConsumer slash, VertexConsumer energy,
                                          ThunderboltCallGeometry.Curve curve,
                                          float head, float tail, float alpha, float erode,
                                          Vec3 camera, long seed, float frame,
                                          float width, boolean whiteCore) {
        ThunderboltCallGeometry.ribbon(dark, matrix, curve, 72, head, tail,
                width * 1.75F, camera, BLACK_PURPLE, alpha * 0.66F,
                erode * 0.38F, seed, frame);
        ThunderboltCallGeometry.ribbon(slash, matrix, curve, 72, head, tail * 0.95F,
                width, camera, VIOLET, alpha * 0.96F,
                erode * 0.60F, seed + 1L, frame + 1.4F);
        ThunderboltCallGeometry.ribbon(energy, matrix, curve, 72, head, tail * 0.86F,
                width * 0.22F, camera, whiteCore ? WHITE : PALE, alpha,
                erode * 0.78F, seed + 2L, frame + 2.8F);
    }

    private static void renderCrossMotionEcho(Matrix4f matrix, VertexConsumer dark,
                                              VertexConsumer slash,
                                              ThunderboltCallGeometry.Curve curve,
                                              float head, float tail, float alpha, float erode,
                                              Vec3 camera, long seed, float frame, float width) {
        float motionAlpha = alpha * (0.18F + 0.12F * head);
        ThunderboltCallGeometry.ribbon(dark, matrix, curve, 64, head, tail,
                width * 1.42F, camera, BLACK_PURPLE, motionAlpha,
                0.28F + erode * 0.48F, seed, frame);
        ThunderboltCallGeometry.ribbon(slash, matrix, curve, 64, head, tail * 0.92F,
                width * 0.68F, camera, PURPLE, motionAlpha * 1.25F,
                0.42F + erode * 0.46F, seed + 1L, frame + 1.3F);
    }

    /**
     * A quick launch followed by a persistent forward coast. The linear coast
     * term deliberately keeps a non-zero velocity through the final fade frame.
     */
    private static float crossForwardTravel(float frame) {
        float progress = Mth.clamp((frame - 4.35F) / (16.20F - 4.35F), 0.0F, 1.0F);
        float inverse = 1.0F - progress;
        float launch = 1.0F - inverse * inverse * inverse;
        return -0.30F + launch * 2.35F + progress * 1.35F;
    }

    private static ThunderboltCallGeometry.Curve crossCurve(Vec3 center, Basis basis,
                                                            boolean reverse) {
        return u -> {
            double x = (u - 0.5D) * 9.8D;
            double y = (u - 0.5D) * 5.6D * (reverse ? -1.0D : 1.0D);
            double z = Math.sin(Math.PI * u) * (reverse ? 0.46D : 0.38D);
            return localPoint(center, basis, x, y, z);
        };
    }

    private static ThunderboltCallGeometry.Curve axis(
            Vec3 center, Basis basis,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double xBow, double zBow) {
        return u -> {
            double bow = Math.sin(Math.PI * u);
            return localPoint(center, basis,
                    Mth.lerp(u, x0, x1) + bow * xBow,
                    Mth.lerp(u, y0, y1),
                    Mth.lerp(u, z0, z1) + bow * zBow);
        };
    }

    private static ThunderboltCallGeometry.Curve orbit(
            Vec3 center, Basis basis,
            double startDegrees, double endDegrees,
            double radiusX, double radiusZ,
            double startY, double endY,
            double heightBow, double radialBulge, double heightTwist) {
        return u -> {
            double angle = Math.toRadians(Mth.lerp(u, startDegrees, endDegrees));
            double radialScale = 1.0D + Math.sin(Math.PI * u) * radialBulge;
            double x = Math.cos(angle) * radiusX * radialScale;
            double z = Math.sin(angle) * radiusZ * radialScale;
            double y = Mth.lerp(u, startY, endY)
                    + Math.sin(Math.PI * u) * heightBow
                    + Math.sin(Math.PI * 2.0D * u) * heightTwist;
            return localPoint(center, basis, x, y, z);
        };
    }

    private static Vec3 localPoint(Vec3 center, Basis basis,
                                   double x, double y, double z) {
        return center.add(basis.right.scale(x))
                .add(WORLD_UP.scale(y))
                .add(basis.forward.scale(z));
    }

    private static Vec3 localVector(Basis basis, double x, double y, double z) {
        return basis.right.scale(x).add(WORLD_UP.scale(y)).add(basis.forward.scale(z));
    }

    private static float groundPulse(float frame) {
        float[] impacts = {7.1F, 9.0F, 16.05F, 18.55F, 19.20F, 20.0F, 22.45F};
        float pulse = 0.0F;
        for (float impact : impacts) {
            pulse = Math.max(pulse, NarukamiDivinityTimeline.gaussian(frame, impact, 0.48F));
        }
        return pulse;
    }

    private static float sineSquared(float value) {
        float sine = Mth.sin(value);
        return sine * sine;
    }

    private static Basis basis(Vec3 forward) {
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < 1.0E-8D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 right = WORLD_UP.cross(horizontal).normalize();
        return new Basis(right, horizontal);
    }

    @Override
    public ResourceLocation getTextureLocation(NarukamiDivinityEntity entity) {
        return EMPTY_TEXTURE;
    }

    private record Basis(Vec3 right, Vec3 forward) {
    }
}

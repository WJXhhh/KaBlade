package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.entity.ThunderboltCallEntity;
import com.wjx.kablade.slasharts.ThunderboltCallTimeline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.wjx.kablade.client.renderer.ThunderboltCallGeometry.hash01;

/** Layered world-space renderer for 唤霆霓 / Thunderbolt Call. */
public final class ThunderboltCallRenderer extends EntityRenderer<ThunderboltCallEntity> {

    private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "textures/entity/empty.png");
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private static final int DARK_PURPLE = 0x28133F;
    private static final int DEEP_PURPLE = 0x4C207F;
    private static final int PURPLE = 0x7134CE;
    private static final int BRIGHT_PURPLE = 0xB468FF;
    private static final int PINK_PURPLE = 0xD68CFF;
    private static final int PALE_PURPLE = 0xF1D6FF;
    private static final int WHITE = 0xFFFFFF;
    private static final int DARK_FRAGMENT = 0x130D21;

    public ThunderboltCallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(ThunderboltCallEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return frustum.isVisible(entity.getBoundingBoxForCulling());
    }

    @Override
    public void render(ThunderboltCallEntity entity, float entityYaw, float partialTick,
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

    /** Material-separated entry point shared by the vanilla and private Oculus paths. */
    static void renderGeometry(ThunderboltCallEntity entity, float partialTick,
                               Matrix4f matrix, Vec3 cameraPosition,
                               VertexConsumer dark, VertexConsumer energy,
                               VertexConsumer slash, VertexConsumer lightning,
                               VertexConsumer particles) {
        float frame = entity.getReferenceFrame(partialTick);
        Vec3 entityPosition = entity.getPosition(partialTick);
        Vec3 camera = cameraPosition.subtract(entityPosition);
        Vec3 owner = entity.getOwnerAnchor(partialTick).subtract(entityPosition);
        Vec3 target = entity.getTargetAnchor(partialTick).subtract(entityPosition);
        Vec3 storedTarget = entity.getStoredTargetAnchor().subtract(entityPosition);
        Vec3 forward = entity.getLaunchDirection();
        Basis basis = basis(forward);

        renderCharge(matrix, dark, energy, lightning, frame, owner, forward, basis, camera,
                entity.getSeed());
        renderDash(matrix, dark, energy, slash, lightning, frame, target, forward, basis,
                camera, entity.getSeed());
        renderFirstImpact(matrix, dark, energy, frame, target, basis, camera, entity.getSeed());
        renderCrossFinisher(matrix, dark, energy, slash, lightning, frame, storedTarget,
                forward, basis, camera, entity.getSeed());
        renderTargetElectricity(matrix, dark, energy, lightning, frame, owner, target,
                forward, basis, camera, entity.getSeed());
        renderResidual(matrix, dark, particles, frame, target, storedTarget, forward, basis,
                camera, entity.getSeed());

        if (KabladeClientConfig.THUNDERBOLT_CALL_DEBUG_ANCHORS.get()) {
            renderDebug(matrix, energy, owner, target, storedTarget, forward, camera);
        }
    }

    private static void renderCharge(Matrix4f matrix, VertexConsumer dark,
                                     VertexConsumer energy, VertexConsumer lightning,
                                     float frame, Vec3 owner, Vec3 forward, Basis basis,
                                     Vec3 camera, long seed) {
        Vec3 center = owner.add(WORLD_UP.scale(1.72D)).add(forward.scale(0.08D));
        float haloAlpha = ThunderboltCallTimeline.plateau(frame, 0.0F, 0.15F, 2.2F, 3.8F);
        if (haloAlpha > 0.001F) {
            float pulse = ThunderboltCallTimeline.gaussian(frame, 0.55F, 0.38F) * 0.32F
                    + ThunderboltCallTimeline.gaussian(frame, 1.55F, 0.45F) * 0.42F
                    + ThunderboltCallTimeline.gaussian(frame, 2.45F, 0.36F) * 0.48F;
            int[] ringColors = {PALE_PURPLE, BRIGHT_PURPLE, PURPLE, DEEP_PURPLE};
            for (int i = 0; i < 4; i++) {
                float radius = (1.08F + i * 0.12F) * (1.0F + pulse * (0.12F + i * 0.025F));
                float spin = frame * (i % 2 == 0 ? 0.024F : -0.026F) + i * 0.42F;
                ThunderboltCallGeometry.ring(i == 3 ? dark : energy, matrix, center,
                        basis.right, basis.up, radius, 0.020F + (i % 2) * 0.010F,
                        48, spin, camera, ringColors[i], haloAlpha * (0.70F - i * 0.08F),
                        seed + 31L * i, 0.0F);
            }
            for (int i = 0; i < 5; i++) {
                ThunderboltCallGeometry.ring(i >= 3 ? dark : energy, matrix, center,
                        basis.right, basis.up, 1.06F + i * 0.065F,
                        0.016F + (i % 3) * 0.006F, 56,
                        i * 0.47F + frame * (i % 2 == 0 ? 0.024F : -0.027F),
                        camera, i == 0 ? PALE_PURPLE : (i % 2 == 0 ? PURPLE : BRIGHT_PURPLE),
                        haloAlpha * (0.72F - i * 0.08F), seed + 211L + i * 17L,
                        0.09F + i * 0.008F);
            }
            for (int i = 0; i < 18; i++) {
                double angle = i * Mth.TWO_PI / 18.0D
                        + (hash01(seed + 400L + i) - 0.5D) * 0.16D;
                Vec3 radial = basis.right.scale(Math.cos(angle)).add(basis.up.scale(Math.sin(angle)));
                Vec3 start = center.add(radial.scale(1.0D));
                float length = 0.36F + hash01(seed + 430L + i * 3L) * 0.65F;
                float flicker = 0.62F + 0.38F * sineSquared(frame * 1.7F + i);
                ThunderboltCallGeometry.beam(energy, matrix, start,
                        start.add(radial.scale(length)), 0.012F + i % 4 * 0.003F,
                        camera, i % 5 == 0 ? PALE_PURPLE : BRIGHT_PURPLE,
                        haloAlpha * flicker * 0.72F);
            }
            ThunderboltCallGeometry.discBillboard(dark, matrix, center, camera,
                    1.62F + pulse, 1.62F + pulse, frame * 0.022F,
                    PURPLE, haloAlpha * 0.24F);
            ThunderboltCallGeometry.discBillboard(energy, matrix, center, camera,
                    0.70F + pulse * 0.42F, 0.70F + pulse * 0.42F,
                    -frame * 0.035F, PALE_PURPLE, haloAlpha * 0.38F);
        }

        float arcsAlpha = ThunderboltCallTimeline.plateau(frame, 0.15F, 0.55F, 3.7F, 5.6F);
        if (arcsAlpha > 0.001F) {
            long dynamicSeed = seed + (long) (frame * 3.0F) * 97L;
            for (int i = 0; i < 9; i++) {
                double angle = i * Mth.TWO_PI / 9.0D + hash01(seed + 520L + i) * 0.35D;
                Vec3 radial = basis.right.scale(Math.cos(angle)).add(basis.up.scale(Math.sin(angle)));
                Vec3 start = center.add(radial.scale(0.38D));
                Vec3 end = center.add(radial.scale(1.35D + hash01(seed + 550L + i) * 0.75D))
                        .add(forward.scale((hash01(seed + 570L + i) - 0.5D) * 0.48D));
                float flicker = 0.42F + 0.58F * sineSquared(frame * 2.8F + i);
                ThunderboltCallGeometry.lightning(dark, lightning, matrix, start, end,
                        9 + i % 4, 0.13F + hash01(seed + i * 13L) * 0.18F,
                        0.024F + i % 3 * 0.005F, camera,
                        PURPLE, i % 3 == 0 ? PALE_PURPLE : BRIGHT_PURPLE,
                        arcsAlpha * flicker, dynamicSeed + i * 101L);
            }
        }
    }

    private static void renderDash(Matrix4f matrix, VertexConsumer dark,
                                   VertexConsumer energy, VertexConsumer slash,
                                   VertexConsumer lightning, float frame, Vec3 target,
                                   Vec3 forward, Basis basis, Vec3 camera, long seed) {
        Vec3 start = WORLD_UP.scale(1.50D);
        Vec3 finish = target.add(forward.scale(2.45D));
        Vec3 controlA = start.lerp(target, 0.34D).add(basis.up.scale(0.18D));
        Vec3 controlB = start.lerp(target, 0.78D).add(basis.right.scale(-0.16D));
        ThunderboltCallGeometry.Curve dashCurve = u -> bezier(start, controlA, controlB, finish, u);

        float ribbonAlpha = ThunderboltCallTimeline.plateau(frame, 0.8F, 1.5F, 7.1F, 10.6F);
        float head = ThunderboltCallTimeline.smooth(frame, 1.0F, 4.35F);
        float tail = 0.93F * ThunderboltCallTimeline.smooth(frame, 6.4F, 10.3F);
        float erode = ThunderboltCallTimeline.smooth(frame, 7.5F, 10.6F);
        ThunderboltCallGeometry.ribbon(dark, matrix, dashCurve, 72, head, tail,
                0.37F, camera, DARK_PURPLE, ribbonAlpha * 0.66F, erode * 0.42F,
                seed + 1001L, frame);
        ThunderboltCallGeometry.ribbon(slash, matrix, dashCurve, 72, head, tail * 0.94F,
                0.22F, camera, BRIGHT_PURPLE, ribbonAlpha * 0.92F, erode * 0.56F,
                seed + 1002L, frame);
        ThunderboltCallGeometry.ribbon(energy, matrix, dashCurve, 72, head, tail * 0.86F,
                0.074F, camera, WHITE, ribbonAlpha, erode * 0.72F,
                seed + 1003L, frame);
        ThunderboltCallGeometry.Curve lowerCurve = u -> dashCurve.point(u)
                .add(basis.up.scale(-0.38D - u * 0.10D))
                .add(forward.scale(0.17D + Math.sin(u * Math.PI * 3.0D) * 0.05D));
        ThunderboltCallGeometry.ribbon(energy, matrix, lowerCurve, 64,
                ThunderboltCallTimeline.smooth(frame, 1.3F, 4.8F),
                0.92F * ThunderboltCallTimeline.smooth(frame, 6.8F, 10.2F),
                0.062F, camera, PALE_PURPLE, ribbonAlpha * 0.78F,
                erode * 0.70F, seed + 1004L, frame + 5.2F);

        float lightningAlpha = ThunderboltCallTimeline.plateau(frame, 1.0F, 1.8F, 7.3F, 10.4F);
        if (lightningAlpha > 0.001F) {
            long dynamicSeed = seed + 1200L + (long) (frame * 3.0F) * 89L;
            for (int i = 0; i < 6; i++) {
                float delay = i * 0.22F;
                float age = frame - 1.0F - delay;
                float appear = ThunderboltCallTimeline.smooth(age, 0.0F, 0.65F);
                float fade = 1.0F - ThunderboltCallTimeline.smooth(frame,
                        7.1F + i * 0.15F, 10.3F);
                float flicker = 0.48F + 0.52F * sineSquared(frame * 3.1F + i * 1.31F);
                Vec3 a = dashCurve.point(0.03F + i * 0.055F)
                        .add(basis.up.scale((i % 2 == 0 ? -1.0D : 1.0D) * (0.22D + i * 0.035D)));
                Vec3 b = dashCurve.point(0.78F + i * 0.035F)
                        .add(basis.up.scale((i % 3 - 1) * 0.34D))
                        .add(forward.scale((i % 2 == 0 ? 1.0D : -1.0D) * 0.20D));
                ThunderboltCallGeometry.lightning(dark, lightning, matrix, a, b,
                        13 + i % 3, 0.22F + i % 3 * 0.04F,
                        0.026F + i % 2 * 0.007F, camera, PURPLE,
                        i % 3 == 0 ? PALE_PURPLE : BRIGHT_PURPLE,
                        lightningAlpha * appear * fade * flicker,
                        dynamicSeed + i * 107L);
            }
        }

        float ghostAlpha = ThunderboltCallTimeline.plateau(frame, 1.0F, 1.8F, 7.0F, 9.5F);
        float current = ThunderboltCallTimeline.smooth(frame, 1.0F, 4.8F);
        for (int i = 0; i < 4; i++) {
            float u = Mth.clamp(current - 0.105F * (i + 1), 0.0F, 1.0F);
            float individual = ghostAlpha * Mth.clamp((current - i * 0.08F) * 2.1F, 0.0F, 1.0F)
                    * (1.0F - ThunderboltCallTimeline.smooth(frame, 6.3F + i * 0.25F, 9.3F));
            Vec3 feet = dashCurve.point(u).add(basis.up.scale(-1.45D))
                    .add(forward.scale(0.10D + i * 0.055D));
            ThunderboltCallGeometry.humanoidAfterimage(i == 0 ? energy : dark, matrix,
                    feet, camera, basis.right, basis.up, 0.92F + i * 0.018F,
                    i == 0 ? PALE_PURPLE : PURPLE,
                    individual * (0.72F - i * 0.10F));
        }

        float needleAlpha = ThunderboltCallTimeline.plateau(frame, 1.0F, 1.8F, 7.6F, 10.3F);
        for (int i = 0; i < 42; i++) {
            float delay = hash01(seed + 1500L + i * 11L) * 2.4F;
            float age = frame - 1.0F - delay;
            float appear = ThunderboltCallTimeline.smooth(age, 0.0F, 0.65F);
            float fade = 1.0F - ThunderboltCallTimeline.smooth(age, 2.0F, 5.6F);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            float u = hash01(seed + 1600L + i * 17L);
            Vec3 p = dashCurve.point(u)
                    .add(basis.right.scale((hash01(seed + i * 23L) - 0.5D) * 2.8D))
                    .add(basis.up.scale((hash01(seed + i * 29L) - 0.5D) * 3.5D))
                    .add(forward.scale((hash01(seed + i * 31L) - 0.5D) * 1.4D));
            float length = (0.75F + hash01(seed + i * 37L) * 2.9F) * appear * fade;
            ThunderboltCallGeometry.beam(i % 8 == 0 ? energy : dark, matrix,
                    p.add(forward.scale(length * 0.25D)), p.subtract(forward.scale(length)),
                    0.007F + i % 3 * 0.002F, camera,
                    i % 8 == 0 ? PALE_PURPLE : (i % 3 == 0 ? PURPLE : DARK_PURPLE),
                    needleAlpha * fade * (0.58F + 0.42F * sineSquared(frame * 1.9F + i)));
        }
    }

    private static void renderFirstImpact(Matrix4f matrix, VertexConsumer dark,
                                          VertexConsumer energy, float frame, Vec3 target,
                                          Basis basis, Vec3 camera, long seed) {
        float alpha = ThunderboltCallTimeline.plateau(frame, 2.8F, 3.4F, 7.2F, 9.5F);
        float pulse = ThunderboltCallTimeline.gaussian(frame, 3.75F, 0.48F) * 0.78F
                + ThunderboltCallTimeline.gaussian(frame, 5.05F, 0.72F)
                + ThunderboltCallTimeline.gaussian(frame, 6.45F, 0.95F) * 0.46F;
        if (alpha > 0.001F) {
            ThunderboltCallGeometry.beam(energy, matrix,
                    target.subtract(basis.right.scale(4.4D)), target.add(basis.right.scale(4.4D)),
                    0.055F + pulse * 0.035F, camera, PALE_PURPLE, alpha * 0.72F);
            ThunderboltCallGeometry.beam(energy, matrix,
                    target.subtract(basis.up.scale(2.9D)), target.add(basis.up.scale(2.9D)),
                    0.045F + pulse * 0.025F, camera, BRIGHT_PURPLE, alpha * 0.52F);
        }

        float radialAlpha = ThunderboltCallTimeline.plateau(frame, 2.9F, 3.5F, 6.8F, 8.8F);
        if (radialAlpha > 0.001F) {
            for (int i = 0; i < 28; i++) {
                float delay = hash01(seed + 1800L + i * 17L) * 0.85F;
                float age = frame - 2.95F - delay;
                float grow = easeOutBack(ThunderboltCallTimeline.smooth(age, 0.0F, 1.35F));
                float fade = 1.0F - ThunderboltCallTimeline.smooth(age, 2.1F, 5.0F);
                if (age <= 0.0F || fade <= 0.001F) {
                    continue;
                }
                double angle = i * Mth.TWO_PI / 28.0D
                        + (hash01(seed + 1850L + i) - 0.5D) * 0.18D;
                Vec3 direction = basis.right.scale(Math.cos(angle)).add(basis.up.scale(Math.sin(angle)));
                float length = (1.25F + (float) Math.pow(hash01(seed + 1900L + i), 0.55D) * 3.2F)
                        * grow;
                ThunderboltCallGeometry.beam(i % 7 == 0 ? energy : dark, matrix,
                        target.add(direction.scale(0.07D * grow)), target.add(direction.scale(length)),
                        0.018F + hash01(seed + i * 41L) * 0.024F, camera,
                        i % 7 == 0 ? WHITE : (i % 3 == 0 ? BRIGHT_PURPLE : PURPLE),
                        radialAlpha * fade * 0.92F);
            }
        }
    }

    private static void renderCrossFinisher(Matrix4f matrix, VertexConsumer dark,
                                            VertexConsumer energy, VertexConsumer slash,
                                            VertexConsumer lightning, float frame,
                                            Vec3 storedTarget, Vec3 forward, Basis basis,
                                            Vec3 camera, long seed) {
        float alpha = ThunderboltCallTimeline.plateau(frame, 10.0F, 11.2F, 20.8F, 24.8F);
        float distance = ThunderboltCallTimeline.crossTravelDistance(frame);
        float scale = ThunderboltCallTimeline.crossScale(frame);
        Vec3 center = storedTarget.add(forward.scale(0.25D + distance));
        ThunderboltCallGeometry.Curve slashA = crossCurve(center, forward, basis, scale, false);
        ThunderboltCallGeometry.Curve slashB = crossCurve(center, forward, basis, scale, true);

        float headA = ThunderboltCallTimeline.smooth(frame, 10.2F, 12.7F);
        float headB = ThunderboltCallTimeline.smooth(frame, 10.7F, 13.25F);
        float tailA = 0.91F * ThunderboltCallTimeline.smooth(frame, 17.2F, 24.4F);
        float tailB = 0.89F * ThunderboltCallTimeline.smooth(frame, 17.7F, 24.8F);
        float erodeA = ThunderboltCallTimeline.smooth(frame, 15.8F, 24.8F);
        float erodeB = ThunderboltCallTimeline.smooth(frame, 16.2F, 24.8F);
        renderSlashLayers(matrix, dark, slash, energy, slashA, headA, tailA, alpha,
                erodeA, camera, seed + 2001L, frame, 0.42F * scale);
        renderSlashLayers(matrix, dark, slash, energy, slashB, headB, tailB, alpha,
                erodeB, camera, seed + 2101L, frame + 7.1F, 0.40F * scale);

        float crossPulse = ThunderboltCallTimeline.gaussian(frame, 11.45F, 0.55F) * 0.72F
                + ThunderboltCallTimeline.gaussian(frame, 12.55F, 0.65F)
                + ThunderboltCallTimeline.gaussian(frame, 14.2F, 1.15F) * 0.46F;
        float centerFade = 1.0F - ThunderboltCallTimeline.smooth(frame, 16.0F, 22.2F);
        ThunderboltCallGeometry.discBillboard(dark, matrix, center, camera,
                (1.0F + crossPulse * 1.7F) * scale,
                (1.0F + crossPulse * 1.7F) * scale, frame * 0.02F,
                BRIGHT_PURPLE, alpha * centerFade * (0.24F + crossPulse * 0.25F));
        ThunderboltCallGeometry.discBillboard(energy, matrix, center, camera,
                (0.55F + crossPulse * 1.35F) * scale,
                (0.55F + crossPulse * 1.35F) * scale, -0.18F + frame * 0.055F,
                WHITE, alpha * centerFade * (0.32F + crossPulse * 0.35F));

        float lightningAlpha = ThunderboltCallTimeline.plateau(frame, 10.2F, 11.4F, 21.8F, 25.6F);
        if (lightningAlpha > 0.001F) {
            float launch = ThunderboltCallTimeline.smooth(frame, 11.05F, 17.15F);
            float lag = (0.08F + launch * 0.32F)
                    * (1.0F - ThunderboltCallTimeline.smooth(frame, 22.0F, 25.4F));
            Vec3 lightningCenter = center.subtract(forward.scale(lag));
            ThunderboltCallGeometry.Curve lightningA = crossCurve(lightningCenter, forward, basis, scale, false);
            ThunderboltCallGeometry.Curve lightningB = crossCurve(lightningCenter, forward, basis, scale, true);
            long dynamicSeed = seed + 2300L + (long) (frame * 3.0F) * 131L;
            for (int i = 0; i < 6; i++) {
                float startU = i < 4 ? (i % 2) * 0.48F : 0.16F + (i - 4) * 0.18F;
                float endU = Math.min(1.0F, startU + (i < 4 ? 0.52F : 0.56F));
                ThunderboltCallGeometry.Curve curve = i % 2 == 0 ? lightningA : lightningB;
                float flicker = 0.35F + 0.65F * sineSquared(frame * 2.75F + i * 1.71F);
                ThunderboltCallGeometry.lightning(dark, lightning, matrix,
                        curve.point(startU), curve.point(endU), 15 + i % 3,
                        0.22F + i % 3 * 0.035F, 0.027F + i % 2 * 0.007F,
                        camera, PURPLE, i % 2 == 0 ? PALE_PURPLE : BRIGHT_PURPLE,
                        lightningAlpha * flicker, dynamicSeed + i * 151L);
            }
            for (int i = 0; i < 6; i++) {
                double angle = i * Mth.TWO_PI / 6.0D + 0.35D;
                Vec3 branch = lightningCenter
                        .add(basis.right.scale(Math.cos(angle) * (1.15D + i * 0.16D) * scale))
                        .add(basis.up.scale(Math.sin(angle) * (1.0D + i * 0.10D) * scale))
                        .add(forward.scale((i % 2 == 0 ? -1.0D : 1.0D) * 0.32D));
                float flicker = 0.34F + 0.66F * sineSquared(frame * 3.05F + i);
                ThunderboltCallGeometry.lightning(dark, lightning, matrix,
                        lightningCenter, branch, 8 + i % 3, 0.18F,
                        0.018F, camera, DEEP_PURPLE, PALE_PURPLE,
                        lightningAlpha * flicker * 0.72F,
                        dynamicSeed + 1000L + i * 173L);
            }
        }

        // A short follow-up crescent that chases the completed X. Keeping it attached to
        // the moving cross center avoids the old, detached half-ring at the stored target.
        float orbitAlpha = ThunderboltCallTimeline.plateau(frame, 13.0F, 13.7F, 17.2F, 19.1F);
        float orbitHead = ThunderboltCallTimeline.smooth(frame, 13.0F, 14.5F);
        float orbitTail = 0.94F * ThunderboltCallTimeline.smooth(frame, 16.3F, 18.9F);
        Vec3 orbitCenter = center.subtract(forward.scale(0.34D));
        ThunderboltCallGeometry.Curve orbit = u -> {
            double angle = Mth.lerp(u, -2.24D, -1.02D);
            double radius = 1.36D + Math.sin(Math.PI * u) * 0.18D;
            return orbitCenter.add(basis.right.scale(Math.cos(angle) * radius))
                    .add(basis.up.scale(Math.sin(angle) * radius * 0.76D + 0.82D))
                    .add(forward.scale(-0.12D + u * 0.34D));
        };
        ThunderboltCallGeometry.ribbon(dark, matrix, orbit, 72, orbitHead, orbitTail,
                0.16F, camera, DARK_PURPLE, orbitAlpha * 0.52F,
                ThunderboltCallTimeline.smooth(frame, 16.8F, 19.0F) * 0.58F,
                seed + 2601L, frame + 11.0F);
        ThunderboltCallGeometry.ribbon(slash, matrix, orbit, 72, orbitHead, orbitTail * 0.94F,
                0.092F, camera, BRIGHT_PURPLE, orbitAlpha * 0.84F,
                ThunderboltCallTimeline.smooth(frame, 16.8F, 19.0F) * 0.72F,
                seed + 2602L, frame + 12.0F);
        ThunderboltCallGeometry.ribbon(energy, matrix, orbit, 72, orbitHead, orbitTail * 0.86F,
                0.026F, camera, WHITE, orbitAlpha * 0.94F,
                ThunderboltCallTimeline.smooth(frame, 16.8F, 19.0F) * 0.88F,
                seed + 2603L, frame + 13.0F);
    }

    private static void renderTargetElectricity(Matrix4f matrix, VertexConsumer dark,
                                                VertexConsumer energy, VertexConsumer lightning,
                                                float frame, Vec3 owner, Vec3 target,
                                                Vec3 forward, Basis basis, Vec3 camera,
                                                long seed) {
        float first = ThunderboltCallTimeline.plateau(frame, 2.8F, 3.5F, 7.8F, 10.0F) * 0.75F;
        float second = ThunderboltCallTimeline.plateau(frame, 10.4F, 11.5F, 25.8F, 29.7F);
        float alpha = Math.max(first, second);
        if (alpha > 0.001F) {
            long dynamicSeed = seed + 2800L + (long) (frame * 4.0F) * 109L;
            for (int i = 0; i < 10; i++) {
                double angle = hash01(seed + 2820L + i * 13L) * Mth.TWO_PI;
                double y0 = (hash01(seed + 2840L + i * 17L) - 0.5D) * 2.2D;
                Vec3 radial = basis.right.scale(Math.cos(angle))
                        .add(forward.scale(Math.sin(angle)));
                Vec3 start = target.add(radial.scale(0.30D)).add(basis.up.scale(y0));
                double angle2 = angle + 0.8D + hash01(seed + 2860L + i * 19L);
                Vec3 radial2 = basis.right.scale(Math.cos(angle2))
                        .add(forward.scale(Math.sin(angle2)));
                Vec3 end = target.add(radial2.scale(0.65D + hash01(seed + i * 23L) * 0.65D))
                        .add(basis.up.scale(y0 + (hash01(seed + i * 29L) - 0.5D) * 1.3D));
                float flicker = 0.28F + 0.72F * sineSquared(frame * 3.4F + i * 1.21F);
                ThunderboltCallGeometry.lightning(dark, lightning, matrix, start, end,
                        7 + i % 4, 0.15F, 0.018F + i % 3 * 0.004F,
                        camera, PURPLE, i % 3 == 0 ? PALE_PURPLE : BRIGHT_PURPLE,
                        alpha * flicker, dynamicSeed + i * 113L);
            }
        }

        float groundAlpha = ThunderboltCallTimeline.plateau(frame, 10.8F, 11.7F, 20.4F, 24.7F);
        if (groundAlpha > 0.001F) {
            Vec3 ground = new Vec3(owner.x, 0.045D, owner.z);
            long dynamicSeed = seed + 3100L + (long) (frame * 3.0F) * 83L;
            for (int i = 0; i < 7; i++) {
                float delay = hash01(seed + 3120L + i * 17L) * 1.6F;
                float age = frame - 10.8F - delay;
                float appear = ThunderboltCallTimeline.smooth(age, 0.0F, 0.75F);
                float fade = 1.0F - ThunderboltCallTimeline.smooth(frame,
                        20.2F + i * 0.32F, 24.6F);
                if (age <= 0.0F || fade <= 0.001F) {
                    continue;
                }
                double angle = Mth.lerp(i / 6.0D, -2.7D, 0.3D)
                        + (hash01(seed + 3140L + i) - 0.5D) * 0.30D;
                double distance = 1.2D + hash01(seed + 3160L + i * 19L) * 3.6D;
                Vec3 end = ground.add(basis.right.scale(Math.cos(angle) * distance))
                        .add(forward.scale(Math.sin(angle) * distance * 0.48D));
                float flicker = 0.32F + 0.68F * sineSquared(frame * 2.9F + i * 1.23F);
                ThunderboltCallGeometry.lightning(dark, lightning, matrix, ground, end,
                        10 + i % 4, 0.16F, 0.018F + i % 2 * 0.005F,
                        camera, DEEP_PURPLE, i % 2 == 0 ? BRIGHT_PURPLE : PALE_PURPLE,
                        groundAlpha * appear * fade * flicker,
                        dynamicSeed + i * 127L);
            }
        }
    }

    private static void renderResidual(Matrix4f matrix, VertexConsumer dark,
                                       VertexConsumer particles, float frame,
                                       Vec3 target, Vec3 storedTarget, Vec3 forward,
                                       Basis basis, Vec3 camera, long seed) {
        float sparkAlpha = ThunderboltCallTimeline.plateau(frame, 2.0F, 3.0F, 27.2F, 32.4F);
        for (int i = 0; i < 92; i++) {
            boolean firstBatch = i < 38;
            float birth = firstBatch
                    ? 2.2F + hash01(seed + 3400L + i * 13L) * 3.2F
                    : 10.6F + hash01(seed + 3400L + i * 13L) * 5.6F;
            float duration = 5.0F + hash01(seed + 3420L + i * 17L) * 11.0F;
            float age = frame - birth;
            float born = ThunderboltCallTimeline.smooth(age, 0.0F, 0.42F);
            float fade = 1.0F - ThunderboltCallTimeline.smooth(age,
                    duration * 0.62F, duration);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            Vec3 source = firstBatch ? target : storedTarget;
            Vec3 origin = source
                    .add(basis.right.scale((hash01(seed + 3440L + i * 19L) - 0.5D)
                            * (firstBatch ? 0.55D : 0.75D)))
                    .add(basis.up.scale((hash01(seed + 3460L + i * 23L) - 0.5D)
                            * (firstBatch ? 0.45D : 0.65D)))
                    .add(forward.scale((hash01(seed + 3480L + i * 29L) - 0.5D)
                            * (firstBatch ? 0.45D : 0.55D)));
            double angle = hash01(seed + 3500L + i * 31L) * Mth.TWO_PI;
            float speed = 0.08F + hash01(seed + 3520L + i * 37L) * 0.30F;
            Vec3 velocity = basis.right.scale(Math.cos(angle) * speed)
                    .add(forward.scale(Math.sin(angle) * speed * 0.70D))
                    .add(basis.up.scale((hash01(seed + 3540L + i * 41L) - 0.06D) * 0.24D));
            Vec3 position = origin.add(velocity.scale(age))
                    .add(basis.up.scale(-age * age * 0.0075D));
            float size = 0.022F + born * (0.026F + i % 6 * 0.002F);
            float flicker = 0.72F + 0.28F * sineSquared(frame * 2.2F + i);
            ThunderboltCallGeometry.billboard(particles, matrix, position, camera,
                    size, size * 1.65F, age * (0.35F + i % 5 * 0.09F),
                    i % 10 == 0 ? WHITE : (i % 4 == 0 ? PINK_PURPLE : BRIGHT_PURPLE),
                    sparkAlpha * fade * flicker);
        }

        float fragmentAlpha = ThunderboltCallTimeline.plateau(frame, 10.6F, 11.5F, 26.0F, 30.8F);
        for (int i = 0; i < 36; i++) {
            float birth = 10.8F + hash01(seed + 3700L + i * 13L) * 4.0F;
            float duration = 8.0F + hash01(seed + 3720L + i * 17L) * 10.0F;
            float age = frame - birth;
            float born = ThunderboltCallTimeline.smooth(age, 0.0F, 0.45F);
            float fade = 1.0F - ThunderboltCallTimeline.smooth(age,
                    duration * 0.58F, duration);
            if (age <= 0.0F || fade <= 0.001F) {
                continue;
            }
            Vec3 origin = storedTarget
                    .add(basis.right.scale((hash01(seed + 3740L + i * 19L) - 0.5D) * 0.95D))
                    .add(basis.up.scale((hash01(seed + 3760L + i * 23L) - 0.5D) * 0.82D))
                    .add(forward.scale((hash01(seed + 3780L + i * 29L) - 0.5D) * 0.72D));
            double angle = hash01(seed + 3800L + i * 31L) * Mth.TWO_PI;
            Vec3 velocity = basis.right.scale(Math.cos(angle) * (0.07D + hash01(seed + i * 37L) * 0.24D))
                    .add(forward.scale(Math.sin(angle) * (0.05D + hash01(seed + i * 41L) * 0.19D)))
                    .add(basis.up.scale((hash01(seed + i * 43L) - 0.18D) * 0.24D));
            Vec3 position = origin.add(velocity.scale(age))
                    .add(basis.up.scale(-age * age * 0.006D));
            float size = 0.035F + born * (0.045F + i % 4 * 0.007F);
            ThunderboltCallGeometry.billboard(dark, matrix, position, camera,
                    i % 3 == 0 ? size * 0.42F : size,
                    i % 3 == 0 ? size * 2.2F : size * 1.25F,
                    age * (0.18F + i % 6 * 0.11F),
                    i % 5 == 0 ? DEEP_PURPLE : DARK_FRAGMENT,
                    fragmentAlpha * fade * 0.82F);
        }
    }

    private static void renderSlashLayers(Matrix4f matrix, VertexConsumer dark,
                                          VertexConsumer slash, VertexConsumer energy,
                                          ThunderboltCallGeometry.Curve curve,
                                          float head, float tail, float alpha, float erode,
                                          Vec3 camera, long seed, float frame, float width) {
        ThunderboltCallGeometry.ribbon(dark, matrix, curve, 84, head, tail,
                width, camera, DARK_PURPLE, alpha * 0.62F, erode * 0.42F,
                seed, frame);
        ThunderboltCallGeometry.ribbon(slash, matrix, curve, 84, head, tail * 0.94F,
                width * 0.60F, camera, BRIGHT_PURPLE, alpha * 0.92F,
                erode * 0.61F, seed + 1L, frame + 1.7F);
        ThunderboltCallGeometry.ribbon(energy, matrix, curve, 84, head, tail * 0.86F,
                width * 0.18F, camera, WHITE, alpha,
                erode * 0.80F, seed + 2L, frame + 3.4F);
    }

    private static ThunderboltCallGeometry.Curve crossCurve(Vec3 center, Vec3 forward,
                                                            Basis basis, float scale,
                                                            boolean reverse) {
        return u -> {
            double horizontal = (u - 0.5D) * 8.4D * scale;
            double vertical = (u - 0.5D) * 4.9D * scale * (reverse ? -1.0D : 1.0D);
            double bow = Math.sin(Math.PI * u) * 0.20D * (reverse ? -1.0D : 1.0D);
            return center.add(basis.right.scale(horizontal))
                    .add(basis.up.scale(vertical))
                    .add(forward.scale(bow));
        };
    }

    private static void renderDebug(Matrix4f matrix, VertexConsumer energy,
                                    Vec3 owner, Vec3 target, Vec3 storedTarget,
                                    Vec3 forward, Vec3 camera) {
        ThunderboltCallGeometry.billboard(energy, matrix, owner.add(WORLD_UP.scale(1.6D)),
                camera, 0.09F, 0.09F, 0.0F, 0x44FF88, 0.95F);
        ThunderboltCallGeometry.billboard(energy, matrix, target,
                camera, 0.11F, 0.11F, 0.0F, 0xFF5566, 0.95F);
        ThunderboltCallGeometry.billboard(energy, matrix, storedTarget,
                camera, 0.08F, 0.08F, 0.0F, 0x55AAFF, 0.95F);
        ThunderboltCallGeometry.beam(energy, matrix, storedTarget,
                storedTarget.add(forward.scale(7.0D)), 0.018F, camera,
                0x55AAFF, 0.92F);
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double inverse = 1.0D - t;
        return p0.scale(inverse * inverse * inverse)
                .add(p1.scale(3.0D * inverse * inverse * t))
                .add(p2.scale(3.0D * inverse * t * t))
                .add(p3.scale(t * t * t));
    }

    private static float easeOutBack(float value) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float t = Mth.clamp(value, 0.0F, 1.0F) - 1.0F;
        return 1.0F + c3 * t * t * t + c1 * t * t;
    }

    private static float sineSquared(float value) {
        float sine = Mth.sin(value);
        return sine * sine;
    }

    private static Basis basis(Vec3 forward) {
        Vec3 right = WORLD_UP.cross(forward);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        return new Basis(right, up);
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderboltCallEntity entity) {
        return EMPTY_TEXTURE;
    }

    private record Basis(Vec3 right, Vec3 up) {
    }
}

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.client.RaizanCleaveAnimation;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.entity.RaizanCleaveEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** OBJ weapon and shader-driven layered presentation for Raizan Cleave. */
public final class RaizanCleaveRenderer extends EntityRenderer<RaizanCleaveEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/mei_c5_weapon/mdl.obj");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/mei_c5_weapon/tex.png");
    private static final ResourceLocation BLOOM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/mei_c5_weapon/tex_bloom.png");
    private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "textures/entity/empty.png");

    private static final int CYAN = 0x5EEBFF;
    private static final int BLUE = 0x4184FF;
    private static final int PURPLE = 0xA95CFF;
    private static final int BLADE_ENERGY_PURPLE = 0x9C2FFF;
    private static final int BLADE_ENERGY_CORE = 0xE2B5FF;
    private static final int LIGHTNING_HALO = 0x4E16B8;
    private static final int LIGHTNING_MIST = 0x7127E8;
    private static final int LIGHTNING_BODY = 0xB94FFF;
    private static final int HEART_HALO = 0x74145F;
    private static final int MAGENTA = 0xF05BFF;
    private static final int RED = 0xFF466E;
    private static final int WHITE = 0xF4FFFF;

    public RaizanCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(RaizanCleaveEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        Vec3 origin = entity.position();
        Vec3 target = entity.getTargetAnchor();
        AABB effectBounds = new AABB(
                Math.min(origin.x, target.x), Math.min(origin.y, target.y), Math.min(origin.z, target.z),
                Math.max(origin.x, target.x), Math.max(origin.y, target.y), Math.max(origin.z, target.z))
                .inflate(12.5D, 8.0D, 12.5D);
        return frustum.isVisible(effectBounds);
    }

    @Override
    public void render(RaizanCleaveEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float frame = entity.getReferenceFrame(partialTick);
        RaizanCleaveAnimation.Animation animation = RaizanCleaveAnimation.INSTANCE.current();
        Vec3 target = localTarget(entity);

        poseStack.pushPose();
        Vec3 ownerOffset = clientOwnerOffset(entity, partialTick);
        poseStack.translate(ownerOffset.x, ownerOffset.y, ownerOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        renderWeaponPart(animation, "sheath", "sheath", frame, poseStack, buffer, packedLight);
        renderWeaponPart(animation, "blade", "blade", frame, poseStack, buffer, packedLight);

        if (!RaizanCleavePostPipeline.enqueue(entity, partialTick)) {
            renderEffects(entity, animation, frame, target, poseStack.last().pose(), buffer);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderWeaponPart(RaizanCleaveAnimation.Animation animation,
                                         String track, String part, float frame,
                                         PoseStack poseStack, MultiBufferSource buffer,
                                         int packedLight) {
        RaizanCleaveAnimation.WeaponPose pose = animation.sample(track, frame);
        RaizanCleaveAnimation.ModelSettings modelSettings = animation.model();
        Quaternionf orientation = new Quaternionf(pose.orientation());

        poseStack.pushPose();
        poseStack.translate(pose.grip().x, pose.grip().y, pose.grip().z);
        poseStack.mulPose(orientation);
        poseStack.translate(modelSettings.pivot().x, modelSettings.pivot().y,
                modelSettings.pivot().z);
        Vector3f axis = modelSettings.axisRotationDegrees();
        poseStack.mulPose(Axis.ZP.rotationDegrees(axis.z));
        poseStack.mulPose(Axis.YP.rotationDegrees(axis.y));
        poseStack.mulPose(Axis.XP.rotationDegrees(axis.x));
        poseStack.scale(modelSettings.scale(), modelSettings.scale(), modelSettings.scale());

        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        BladeRenderState.setCol(0xFFFFFFFF);
        BladeRenderState.renderOverrided(ItemStack.EMPTY, model, part, TEXTURE,
                poseStack, buffer, packedLight);

        float bladeEnergy = "blade".equals(part) ? phaseOneBladeEnergy(frame) : 0.0F;
        if (bladeEnergy > 0.001F) {
            int surfaceAlpha = Mth.clamp((int) (218.0F * bladeEnergy), 0, 255);
            BladeRenderState.setCol(BLADE_ENERGY_PURPLE | (surfaceAlpha << 24));
            BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, part, TEXTURE,
                    poseStack, buffer, BladeRenderState.MAX_LIGHT);

            poseStack.pushPose();
            float shellScale = 1.018F + pulse(frame, 2.8F) * 0.012F;
            poseStack.scale(shellScale, shellScale, shellScale);
            int shellAlpha = Mth.clamp((int) (92.0F * bladeEnergy), 0, 255);
            BladeRenderState.setCol(BLADE_ENERGY_CORE | (shellAlpha << 24));
            BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, part, TEXTURE,
                    poseStack, buffer, BladeRenderState.MAX_LIGHT);
            poseStack.popPose();
        }

        int bloomAlpha = Mth.clamp((int) (216.0F + bladeEnergy * 36.0F), 0, 255);
        int bloomRgb = bladeEnergy > 0.001F ? BLADE_ENERGY_CORE : 0xFFFFFF;
        BladeRenderState.setCol(bloomRgb | (bloomAlpha << 24));
        BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, part, BLOOM_TEXTURE,
                poseStack, buffer, BladeRenderState.MAX_LIGHT);
        BladeRenderState.resetCol();
        poseStack.popPose();
    }

    static void renderEffects(RaizanCleaveEntity entity,
                              RaizanCleaveAnimation.Animation animation,
                              float frame, Vec3 target, Matrix4f matrix,
                              MultiBufferSource buffer) {
        VertexConsumer energy = buffer.getBuffer(KabladeRenderTypes.raizanWeaponEnergy());
        VertexConsumer lightning = buffer.getBuffer(KabladeRenderTypes.raizanLightning());
        VertexConsumer heart = buffer.getBuffer(KabladeRenderTypes.raizanHeartSlash());
        VertexConsumer particle = buffer.getBuffer(KabladeRenderTypes.raizanParticle());
        VertexConsumer composite = buffer.getBuffer(KabladeRenderTypes.raizanComposite());
        renderEffects(entity, animation, frame, target, matrix,
                energy, lightning, heart, particle, composite);
    }

    /**
     * Material-separated entry point used by the private Oculus renderer.  It keeps
     * the geometry generator identical between vanilla and shader-pack paths while
     * allowing Oculus to submit it through raw GL programs instead of ShaderInstance.
     */
    static void renderEffects(RaizanCleaveEntity entity,
                              RaizanCleaveAnimation.Animation animation,
                              float frame, Vec3 target, Matrix4f matrix,
                              VertexConsumer energy, VertexConsumer lightning,
                              VertexConsumer heart, VertexConsumer particle,
                              VertexConsumer composite) {
        renderWeaponEnergy(energy, lightning, matrix, animation, frame, entity.getSeed());
        renderGroundSlashScar(energy, matrix, animation, frame, target, entity.getSeed());
        renderLightning(lightning, matrix, animation, frame, target, entity.getSeed());

        renderHeartSlash(heart, matrix, animation, frame, target, entity.getSeed());

        renderParticles(particle, matrix, animation, frame, target, entity.getSeed());

        renderImpactCores(composite, matrix, frame, target);
    }

    private static void renderWeaponEnergy(VertexConsumer out, VertexConsumer lightning,
                                           Matrix4f matrix,
                                           RaizanCleaveAnimation.Animation animation,
                                           float frame, long seed) {
        RaizanCleaveAnimation.WeaponPose blade = animation.sample("blade", frame);
        RaizanCleaveAnimation.WeaponPose sheath = animation.sample("sheath", frame);
        Vec3 core = blade.grip().lerp(sheath.grip(), 0.5D).add(0.0D, 0.20D, 0.0D);

        float coreAlpha = animation.envelope("weapon_core", frame);
        if (coreAlpha > 0.001F) {
            crossedCore(out, matrix, core, 0.34F + pulse(frame, 3.2F) * 0.08F,
                    PURPLE, coreAlpha * 0.80F);
            ring(out, matrix, core, 0.58F, 0.025F, 24, CYAN, coreAlpha * 0.42F,
                    frame * 0.055F, RingPlane.XZ);
            ring(out, matrix, core, 0.46F, 0.020F, 20, MAGENTA, coreAlpha * 0.38F,
                    -frame * 0.073F, RingPlane.XY);
        }

        float bladeCoat = phaseOneBladeEnergy(frame);
        if (bladeCoat > 0.001F) {
            Vec3 bladeAxis = poseDirection(blade);
            Vec3 coatStart = blade.grip().add(bladeAxis.scale(0.10D));
            Vec3 coatEnd = blade.grip().add(bladeAxis.scale(2.72D));
            beamCross(out, matrix, coatStart, coatEnd, 0.22F,
                    LIGHTNING_HALO, bladeCoat * 0.13F);
            beamCross(out, matrix, coatStart, coatEnd, 0.105F,
                    BLADE_ENERGY_PURPLE, bladeCoat * 0.30F);
            beamCross(out, matrix, coatStart, coatEnd, 0.040F,
                    BLADE_ENERGY_CORE, bladeCoat * 0.54F);
        }

        float petals = animation.envelope("guard_petals", frame);
        if (petals > 0.001F) {
            for (int i = 0; i < 10; i++) {
                double angle = i * Mth.TWO_PI / 10.0D + frame * 0.08D;
                Vec3 p = core.add(Math.cos(angle) * 0.62D,
                        Math.sin(angle * 1.7D + i) * 0.22D,
                        Math.sin(angle) * 0.42D);
                diamond(out, matrix, p, 0.12F, i % 2 == 0 ? MAGENTA : PURPLE,
                        petals * 0.70F, angle);
            }
        }

        float glyph = animation.envelope("activation_glyph", frame);
        if (glyph > 0.001F) {
            Vec3 floor = new Vec3(0.0D, 0.035D, 0.0D);
            ring(out, matrix, floor, 1.28F, 0.025F, 40, CYAN, glyph * 0.46F,
                    frame * 0.018F, RingPlane.XZ);
            ring(out, matrix, floor, 0.82F, 0.018F, 32, PURPLE, glyph * 0.34F,
                    -frame * 0.027F, RingPlane.XZ);
            for (int i = 0; i < 12; i++) {
                double a = i * Mth.TWO_PI / 12.0D + frame * 0.018D;
                beam(out, matrix,
                        floor.add(Math.cos(a) * 1.36D, 0.0D, Math.sin(a) * 1.36D),
                        floor.add(Math.cos(a) * 1.62D, 0.0D, Math.sin(a) * 1.62D),
                        0.025F, CYAN, glyph * 0.38F);
            }
        }

        drawHistoricalTrail(out, matrix, animation, frame, "draw_trail", 5.0F,
                3, PURPLE, 0.16F);
        drawHistoricalTrail(out, matrix, animation, frame, "rear_loop_trail", 5.5F,
                3, MAGENTA, 0.18F);
        drawHistoricalTrail(out, matrix, animation, frame, "chop_trail", 4.4F,
                4, CYAN, 0.22F);

        float embers = animation.envelope("orbit_embers", frame);
        if (embers > 0.001F) {
            Vec3 bladeAxis = poseDirection(blade);
            Vec3 bladeSide = perpendicularTo(bladeAxis);
            Vec3 bladeNormal = bladeAxis.cross(bladeSide).normalize();
            int count = scaledCount(10);
            for (int i = 0; i < count; i++) {
                double phase = random01(seed, i * 13 + 4) * Mth.TWO_PI;
                double a = phase + frame * (0.035D + i % 5 * 0.004D);
                double along = 0.16D + random01(seed, i * 17 + 8) * 2.05D;
                double radius = 0.025D + random01(seed, i * 19 + 2) * 0.065D;
                Vec3 p = blade.grip().add(bladeAxis.scale(along))
                        .add(bladeSide.scale(Math.cos(a) * radius))
                        .add(bladeNormal.scale(Math.sin(a) * radius));
                double length = 0.12D + random01(seed, i * 29 + 7) * 0.12D;
                Vec3 tangent = bladeAxis.scale(0.62D)
                        .add(bladeSide.scale(-Math.sin(a) * 0.72D))
                        .add(bladeNormal.scale(Math.cos(a) * 0.72D)).normalize();
                Vec3 bend = bladeSide.scale(Math.cos(a) * 0.035D)
                        .add(bladeNormal.scale(Math.sin(a) * 0.035D));
                Vec3 middle = p.add(tangent.scale(length * 0.46D)).add(bend);
                Vec3 end = p.add(tangent.scale(length));
                float width = 0.0045F + (i % 3) * 0.001F;
                int accent = i % 3 == 0 ? MAGENTA : PURPLE;
                glowingLightningSegment(lightning, matrix, p, middle, width,
                        accent, embers * 0.32F);
                glowingLightningSegment(lightning, matrix, middle, end, width,
                        accent, embers * 0.26F);
            }
        }

        float returning = animation.envelope("weapon_return", frame);
        if (returning > 0.001F) {
            // Never bridge the blade grip and the sheath grip here.  During the
            // over-shoulder return they can be more than a block apart, and a
            // beam between them reads as a stretched weapon/string.  The return
            // accent belongs to the sheath mouth and remains spatially short.
            Vec3 sheathAxis = poseDirection(sheath);
            Vec3 mouth = sheath.grip().add(sheathAxis.scale(0.08D));
            beamCross(out, matrix,
                    mouth.subtract(sheathAxis.scale(0.14D)),
                    mouth.add(sheathAxis.scale(0.18D)),
                    0.042F, PURPLE, returning * 0.42F);
        }
    }

    private static void drawHistoricalTrail(VertexConsumer out, Matrix4f matrix,
                                            RaizanCleaveAnimation.Animation animation,
                                            float frame, String layer, float history,
                                            int layers, int color, float width) {
        float alpha = animation.envelope(layer, frame);
        if (alpha <= 0.001F) {
            return;
        }
        int samples = scaledCount(18);
        float startFrame = Math.max(0.0F, frame - history);
        RaizanCleaveAnimation.WeaponPose previousPose = animation.sample("blade", startFrame);
        Vec3 previousAxis = poseDirection(previousPose);
        Vec3 previousRoot = previousPose.grip().add(previousAxis.scale(0.16D));
        Vec3 previousEdge = previousPose.grip().add(previousAxis.scale(2.72D));
        Vec3 previousHotRoot = previousPose.grip().add(previousAxis.scale(1.82D));
        for (int i = 1; i <= samples; i++) {
            float u0 = (i - 1) / (float) samples;
            float u1 = i / (float) samples;
            float f = Mth.lerp(u1, startFrame, frame);
            RaizanCleaveAnimation.WeaponPose currentPose = animation.sample("blade", f);
            Vec3 currentAxis = poseDirection(currentPose);
            Vec3 currentRoot = currentPose.grip().add(currentAxis.scale(0.16D));
            Vec3 currentEdge = currentPose.grip().add(currentAxis.scale(2.72D));
            Vec3 currentHotRoot = currentPose.grip().add(currentAxis.scale(1.82D));
            float life = i / (float) samples;
            double speed = currentEdge.distanceTo(previousEdge)
                    + currentPose.grip().distanceTo(previousPose.grip()) * 0.45D;
            float velocity = Mth.clamp((float) (speed * samples / Math.max(history, 0.1F)),
                    0.18F, 1.35F);
            float bodyAlpha = alpha * life * (0.20F + velocity * 0.34F);

            // The trail is the surface swept by the physical blade, not a line through its grip.
            quadUv(out, matrix, previousRoot, previousEdge, currentEdge, currentRoot,
                    color, bodyAlpha, u0, u1, 0.0F, 1.0F);
            quadUv(out, matrix, previousHotRoot, previousEdge, currentEdge, currentHotRoot,
                    i + layers >= samples ? WHITE : MAGENTA,
                    alpha * life * (0.28F + velocity * 0.44F),
                    u0, u1, 0.0F, 1.0F);

            previousPose = currentPose;
            previousRoot = currentRoot;
            previousEdge = currentEdge;
            previousHotRoot = currentHotRoot;
        }
    }

    private static void renderGroundSlashScar(VertexConsumer out, Matrix4f matrix,
                                              RaizanCleaveAnimation.Animation animation,
                                              float frame, Vec3 target, long seed) {
        float layer = animation.envelope("ground_slash_scar", frame);
        if (layer <= 0.001F) {
            return;
        }

        Vec3 flatForward = new Vec3(target.x, 0.0D, target.z);
        if (flatForward.lengthSqr() < 1.0E-5D) {
            flatForward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            flatForward = flatForward.normalize();
        }
        Vec3 side = flatForward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 impact = new Vec3(target.x, target.y - 0.86D, target.z)
                .subtract(flatForward.scale(0.28D));

        // The scar writes forward from the impact instead of popping in at full length.
        float write = smooth(Mth.clamp((frame - 18.62F) / 1.55F, 0.0F, 1.0F));
        float lateFade = 1.0F - smooth(Mth.clamp((frame - 24.8F) / 3.0F, 0.0F, 1.0F));
        float alpha = layer * lateFade;
        double fullLength = 7.6D;
        double visibleLength = fullLength * write;
        int segments = Math.max(1, (int) Math.ceil(14.0D * write));
        Vec3 previous = impact;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            double distance = visibleLength * t;
            double drift = Math.sin(t * Math.PI * 2.15D + seed * 0.00001D)
                    * 0.055D * Math.sin(t * Math.PI);
            Vec3 current = impact.add(flatForward.scale(distance))
                    .add(side.scale(drift))
                    .add(0.0D, 0.010D + t * 0.006D, 0.0D);
            float taper = 1.0F - smooth(t) * 0.72F;
            float leading = i == segments && write < 0.999F ? 1.24F : 1.0F;

            beam(out, matrix, previous, current, 0.72F * taper,
                    LIGHTNING_HALO, alpha * 0.16F);
            beam(out, matrix, previous, current, 0.40F * taper,
                    BLADE_ENERGY_PURPLE, alpha * 0.38F);
            beam(out, matrix, previous, current, 0.20F * taper,
                    MAGENTA, alpha * 0.54F);
            beam(out, matrix, previous, current, 0.072F * taper * leading,
                    WHITE, alpha * 0.94F);
            previous = current;
        }

        // Two restrained edge wisps give the scar volume without turning it into ground lightning.
        for (int i = 0; i < 2; i++) {
            double sign = i == 0 ? -1.0D : 1.0D;
            float wispWrite = smooth(Mth.clamp((frame - 18.95F - i * 0.18F) / 1.8F,
                    0.0F, 1.0F));
            Vec3 start = impact.add(side.scale(sign * 0.23D)).add(flatForward.scale(0.18D));
            Vec3 end = impact.add(side.scale(sign * (0.10D + i * 0.05D)))
                    .add(flatForward.scale(visibleLength * (0.66D + i * 0.10D) * wispWrite));
            beam(out, matrix, start, end, 0.11F,
                    PURPLE, alpha * wispWrite * 0.26F);
            beam(out, matrix, start, end, 0.025F,
                    BLADE_ENERGY_CORE, alpha * wispWrite * 0.56F);
        }

        // Small packets travel along the white core to retain the flowing-light read while fading.
        for (int i = 0; i < 3; i++) {
            float travel = (float) (((frame - 18.7F) * 0.115F + i * 0.34F) % 1.0F);
            if (travel < 0.0F) {
                travel += 1.0F;
            }
            float packetFade = Mth.sin(travel * Mth.PI);
            double packetStart = visibleLength * Math.max(0.0F, travel - 0.055F);
            double packetEnd = visibleLength * Math.min(1.0F, travel + 0.055F);
            beam(out, matrix, impact.add(flatForward.scale(packetStart)).add(0.0D, 0.022D, 0.0D),
                    impact.add(flatForward.scale(packetEnd)).add(0.0D, 0.022D, 0.0D),
                    0.095F, BLADE_ENERGY_CORE, alpha * packetFade * 0.74F);
        }
    }

    private static void renderLightning(VertexConsumer out, Matrix4f matrix,
                                        RaizanCleaveAnimation.Animation animation,
                                        float frame, Vec3 target, long seed) {
        float sky = animation.envelope("sky_lightning", frame);
        if (sky > 0.001F) {
            Vec3 end = animation.sample("blade", frame).grip().add(0.0D, 0.15D, 0.0D);
            jaggedBolt(out, matrix, end.add(0.0D, 7.8D, 0.0D), end,
                    12, 0.090F, PURPLE, sky * 0.90F, seed + 11L);
            jaggedBolt(out, matrix, end.add(-2.2D, 5.4D, 1.1D), end,
                    8, 0.052F, PURPLE, sky * 0.68F, seed + 19L);
        }

        float branches = animation.envelope("lightning_branches", frame);
        if (branches > 0.001F) {
            RaizanCleaveAnimation.WeaponPose bladePose = animation.sample("blade", frame);
            Vec3 bladeAxis = poseDirection(bladePose);
            Vec3 bladeSide = perpendicularTo(bladeAxis);
            Vec3 bladeNormal = bladeAxis.cross(bladeSide).normalize();
            for (int i = 0; i < 4; i++) {
                double a = i * Mth.TWO_PI / 4.0D + random01(seed, i + 31) * 0.35D;
                double along = 0.28D + i * 0.48D;
                Vec3 source = bladePose.grip().add(bladeAxis.scale(along))
                        .add(bladeSide.scale(Math.cos(a) * 0.035D));
                Vec3 end = source.add(bladeAxis.scale(0.16D + random01(seed, i + 45) * 0.20D))
                        .add(bladeSide.scale(Math.cos(a) * (0.10D + i * 0.012D)))
                        .add(bladeNormal.scale(Math.sin(a) * (0.08D + i * 0.010D)));
                jaggedBolt(out, matrix, source, end, 3, 0.016F,
                        i % 2 == 0 ? PURPLE : MAGENTA, branches * 0.36F,
                        seed + i * 53L);
            }
        }

        float column = animation.envelope("impact_column", frame);
        if (column > 0.001F) {
            jaggedBolt(out, matrix, target.add(0.0D, 7.2D, 0.0D),
                    target.add(0.0D, -1.0D, 0.0D), 14, 0.145F,
                    PURPLE, column * 0.96F, seed + 101L);
            beamCross(out, matrix, target.add(0.0D, -0.8D, 0.0D),
                    target.add(0.0D, 5.5D, 0.0D), 0.25F, WHITE, column * 0.46F);
            ring(out, matrix, target.add(0.0D, -0.91D, 0.0D),
                    1.85F + column * 1.10F, 0.24F, 42,
                    LIGHTNING_MIST, column * 0.22F, frame * 0.018F, RingPlane.XZ);
            quadFacingXZ(out, matrix, target.add(0.0D, -0.93D, 0.0D),
                    1.10F + column * 0.62F, LIGHTNING_HALO, column * 0.12F);
        }

        float cage = animation.envelope("ground_cage", frame);
        if (cage > 0.001F) {
            for (int i = 0; i < 9; i++) {
                double a = i * Mth.TWO_PI / 9.0D + random01(seed, 210 + i) * 0.22D;
                Vec3 start = target.add(0.0D, -0.95D, 0.0D);
                Vec3 end = start.add(Math.cos(a) * (2.4D + i % 3 * 0.55D),
                        0.08D, Math.sin(a) * (2.4D + i % 3 * 0.55D));
                jaggedBolt(out, matrix, start, end, 7, 0.052F,
                        i % 3 == 0 ? WHITE : PURPLE, cage * 0.76F, seed + 300L + i);
            }
        }

        float heartEntryElectric = animation.envelope("heart_entry", frame);
        if (heartEntryElectric > 0.001F) {
            RaizanCleaveAnimation.WeaponPose bladePose = animation.sample("blade", frame);
            Vec3 bladeAxis = poseDirection(bladePose);
            Vec3 bladeSide = perpendicularTo(bladeAxis);
            Vec3 bladeNormal = bladeAxis.cross(bladeSide).normalize();
            for (int i = 0; i < 8; i++) {
                double a = i * Mth.TWO_PI / 8.0D + frame * 0.045D;
                double along = 0.12D + i * 0.25D;
                Vec3 start = bladePose.grip().add(bladeAxis.scale(along))
                        .add(bladeSide.scale(Math.cos(a) * 0.16D))
                        .add(bladeNormal.scale(Math.sin(a) * 0.16D));
                Vec3 end = bladePose.grip().add(bladeAxis.scale(along + 0.34D))
                        .add(bladeSide.scale(Math.cos(a + 0.78D) * 0.30D))
                        .add(bladeNormal.scale(Math.sin(a + 0.78D) * 0.30D));
                jaggedBolt(out, matrix, start, end, 3, 0.030F,
                        i % 3 == 0 ? MAGENTA : PURPLE,
                        heartEntryElectric * (0.50F + (i % 2) * 0.08F),
                        seed + 470L + i * 29L);
            }
        }

        float heartStorm = Math.max(animation.envelope("heart_main", frame) * 0.72F,
                animation.envelope("heart_core", frame) * 0.58F);
        if (heartStorm > 0.001F) {
            // The reference keeps the heart-slash lightning as a few accents around the
            // broad slash planes. Stagger the bolts so they do not become a persistent web.
            for (int i = 0; i < 3; i++) {
                float local = heartPulse(frame, 32.0F + i * 5.5F,
                        32.55F + i * 5.5F, 35.4F + i * 5.5F);
                if (local <= 0.001F) {
                    continue;
                }
                boolean fromLeft = (i & 1) == 0;
                Vec3 edge = target.add(fromLeft ? -10.0D : 10.0D,
                        -0.48D + i * 0.58D, (i - 1.0D) * 0.44D);
                Vec3 core = target.add((fromLeft ? -0.28D : 0.28D),
                        -0.08D + i * 0.28D, (1.0D - i) * 0.10D);
                jaggedBolt(out, matrix, edge, core, 7,
                        0.042F + (i % 2) * 0.008F,
                        i == 1 ? MAGENTA : PURPLE,
                        heartStorm * local * 0.58F, seed + 510L + i * 37L);
            }
        }

        float after = animation.envelope("after_lightning", frame);
        if (after > 0.001F) {
            for (int i = 0; i < 3; i++) {
                Vec3 start = target.add((i - 1) * 1.4D, 3.2D + i * 0.55D, (i % 2) * 0.8D);
                Vec3 end = target.add((1 - i) * 1.1D, -0.75D, (1 - i) * 0.65D);
                jaggedBolt(out, matrix, start, end, 8, 0.068F,
                        i == 1 ? MAGENTA : PURPLE, after * 0.72F, seed + 421L + i);
            }
        }
    }

    private static void renderHeartSlash(VertexConsumer out, Matrix4f matrix,
                                         RaizanCleaveAnimation.Animation animation,
                                         float frame, Vec3 target, long seed) {
        float entry = animation.envelope("heart_entry", frame);
        if (entry > 0.001F) {
            // A single cyan-white axis announces the cut. A red echo underneath gives it
            // the chromatic split seen in the reference without stacking parallel threads.
            layeredHeartBeam(out, matrix,
                    new Vec3(target.x - 9.6D, target.y, target.z - 0.02D),
                    new Vec3(target.x + 9.6D, target.y, target.z - 0.02D),
                    0.115F, CYAN, entry * 0.94F, 6.4F);
            layeredHeartBeam(out, matrix,
                    new Vec3(target.x - 9.1D, target.y - 0.13D, target.z + 0.06D),
                    new Vec3(target.x + 9.1D, target.y - 0.13D, target.z + 0.06D),
                    0.060F, RED, entry * 0.54F, 4.8F);
        }

        float sweep = animation.envelope("heart_sweep", frame);
        if (sweep > 0.001F) {
            // The turn is completed in the 31.2-32.0 preparation beat.  Once the
            // actual cut starts, the blade crosses the full heart-slash plane in
            // only 3.8 reference frames and keeps its edge locked to the left.
            float progress = smooth(Mth.clamp((frame - 32.0F) / 3.8F, 0.0F, 1.0F));
            Vec3 center = target.add(0.0D, 0.82D - progress * 0.56D,
                    -0.34D + progress * 0.18D);
            Vec3 direction = new Vec3(1.0D, -0.105D - progress * 0.04D, 0.0D);
            // A dominant red-white surface carries the silhouette from both front and rear.
            layeredSlashPlane(out, matrix, center, direction, 19.2D,
                    0.78F - progress * 0.12F, RED, sweep * 0.96F);
            layeredSlashPlane(out, matrix, center.add(0.0D, -0.20D, 0.10D),
                    new Vec3(1.0D, -0.075D, 0.0D), 18.4D,
                    0.30F, MAGENTA, sweep * 0.46F);
        }

        float cross = animation.envelope("heart_cross", frame);
        if (cross > 0.001F) {
            for (int i = 0; i < 5; i++) {
                float birth = 31.0F + i * 2.55F;
                float local = heartPulse(frame, birth, birth + 0.50F, birth + 4.8F);
                if (local <= 0.001F) {
                    continue;
                }
                double angle = -0.70D + i * 0.35D;
                double length = 6.4D + i % 2 * 1.5D;
                Vec3 direction = new Vec3(Math.cos(angle), Math.sin(angle), 0.0D);
                Vec3 center = target.add((i - 2) * 0.18D, 0.45D + (i % 2) * 0.24D,
                        (i - 2) * 0.07D);
                layeredSlashPlane(out, matrix, center, direction, length * 2.0D,
                        0.34F + (i % 2) * 0.10F, RED,
                        cross * local * (i == 2 ? 0.98F : 0.86F));
            }
        }

        float triangles = animation.envelope("heart_triangles", frame);
        if (triangles > 0.001F) {
            for (int i = 0; i < 4; i++) {
                float birth = 34.0F + i * 2.7F;
                float local = heartPulse(frame, birth, birth + 0.70F, birth + 6.0F);
                if (local <= 0.001F) {
                    continue;
                }
                double size = 1.0D + i * 0.42D;
                double rotation = frame * (i % 2 == 0 ? 0.022D : -0.018D) + i * 0.76D;
                Vec3 center = target.add((i - 1.5D) * 0.38D, 0.65D + i * 0.18D,
                        (i - 1.5D) * 0.28D);
                triangleFrame(out, matrix, center, size, rotation,
                        RED, triangles * local * 0.48F);
            }
        }

        float cuts = animation.envelope("heart_cut_lines", frame);
        if (cuts > 0.001F) {
            int count = scaledCount(12);
            for (int i = 0; i < count; i++) {
                float birth = 44.0F + i * (7.2F / Math.max(1, count - 1));
                float local = heartPulse(frame, birth, birth + 0.24F, birth + 2.10F);
                if (local <= 0.001F) {
                    continue;
                }
                double y = target.y - 1.1D + random01(seed, 600 + i) * 4.4D;
                double z = target.z - 1.9D + random01(seed, 700 + i) * 3.8D;
                double tilt = -0.32D + random01(seed, 800 + i) * 0.64D;
                Vec3 a = new Vec3(target.x - 10.2D, y - tilt * 4.4D, z);
                Vec3 b = new Vec3(target.x + 10.2D, y + tilt * 4.4D, z);
                residualHeartLine(out, matrix, a, b,
                        0.016F + (i % 3) * 0.004F,
                        cuts * local * (0.36F + i % 3 * 0.07F));
            }
        }

        float main = animation.envelope("heart_main", frame);
        if (main > 0.001F) {
            Vec3 a = target.add(-11.2D, 0.0D, 0.0D);
            Vec3 b = target.add(11.2D, 0.0D, 0.0D);
            layeredHeartBeam(out, matrix, a, b, 0.135F,
                    RED, main, 6.8F);
        }

        float core = animation.envelope("heart_core", frame);
        if (core > 0.001F) {
            crossedCore(out, matrix, target, 0.48F + pulse(frame, 2.1F) * 0.12F,
                    PURPLE, core * 0.78F);
            ring(out, matrix, target, 0.72F, 0.035F, 30, MAGENTA,
                    core * 0.50F, frame * 0.042F, RingPlane.XY);
        }
    }

    private static void renderParticles(VertexConsumer out, Matrix4f matrix,
                                        RaizanCleaveAnimation.Animation animation,
                                        float frame, Vec3 target, long seed) {
        float debris = animation.envelope("weapon_debris", frame);
        if (debris > 0.001F) {
            int count = scaledCount(26);
            for (int i = 0; i < count; i++) {
                double a = random01(seed, 910 + i) * Mth.TWO_PI;
                double speed = 0.55D + random01(seed, 940 + i) * 1.9D;
                double life = Mth.clamp((frame - 18.5F) / 9.5F, 0.0F, 1.0F);
                Vec3 p = target.add(Math.cos(a) * speed * (0.35D + life),
                        -0.52D + random01(seed, 970 + i) * 2.2D + life * life * 0.72D,
                        Math.sin(a) * speed * (0.35D + life));
                shard3d(out, matrix, p, 0.065F + (i % 5) * 0.016F,
                        i % 3 == 0 ? WHITE : PURPLE, debris * 0.68F,
                        a + frame * (0.10D + i % 4 * 0.025D), i * 0.47D);
            }
        }

        float shards = animation.envelope("heart_shards", frame);
        if (shards > 0.001F) {
            int count = scaledCount(18);
            for (int i = 0; i < count; i++) {
                double a = i * Mth.TWO_PI / Math.max(1, count) + frame * 0.018D;
                double radius = 0.8D + (i % 6) * 0.42D;
                double life = Mth.clamp((frame - 31.0F) / 20.0F, 0.0F, 1.0F);
                Vec3 p = target.add(Math.cos(a) * radius * (0.72D + life * 0.55D),
                        -0.70D + (i % 5) * 0.52D + life * 0.34D,
                        Math.sin(a) * radius * (0.42D + life * 0.24D));
                shard3d(out, matrix, p, 0.11F + (i % 3) * 0.028F,
                        i % 2 == 0 ? WHITE : RED, shards * 0.74F,
                        a + frame * 0.075D, i * 0.81D - frame * 0.052D);
            }
        }

        float petals = animation.envelope("heart_petals", frame);
        if (petals > 0.001F) {
            int count = scaledCount(34);
            for (int i = 0; i < count; i++) {
                double a = random01(seed, 1100 + i) * Mth.TWO_PI + frame * 0.012D;
                double radius = 0.45D + random01(seed, 1200 + i) * 3.8D;
                Vec3 p = target.add(Math.cos(a) * radius,
                        -0.9D + random01(seed, 1300 + i) * 4.2D,
                        Math.sin(a) * radius * 0.72D);
                diamond(out, matrix, p, 0.045F + (i % 4) * 0.018F,
                        i % 4 == 0 ? WHITE : (i % 2 == 0 ? MAGENTA : RED),
                        petals * 0.55F, a + frame * 0.04D);
            }
        }
    }

    private static void renderImpactCores(VertexConsumer out, Matrix4f matrix,
                                          float frame, Vec3 target) {
        float impact = Math.max(gaussian(frame, 19.0F, 0.82F),
                Math.max(gaussian(frame, 38.0F, 1.15F), gaussian(frame, 47.5F, 1.0F)));
        if (impact <= 0.002F) {
            return;
        }
        quadFacingXY(out, matrix, target, 1.25F + impact * 0.85F,
                frame < 25.0F ? CYAN : MAGENTA, impact * 0.70F);
        quadFacingXZ(out, matrix, target.add(0.0D, -0.72D, 0.0D),
                1.45F + impact * 0.62F, PURPLE, impact * 0.42F);
    }

    private static Vec3 localTarget(RaizanCleaveEntity entity) {
        Vec3 relative = entity.getTargetAnchor().subtract(entity.position());
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        double cos = Mth.cos(yaw);
        double sin = Mth.sin(yaw);
        return new Vec3(cos * relative.x + sin * relative.z,
                relative.y, -sin * relative.x + cos * relative.z);
    }

    private static Vec3 clientOwnerOffset(RaizanCleaveEntity entity, float partialTick) {
        Entity owner = entity.level().getEntity(entity.getOwnerId());
        if (owner == null) {
            return Vec3.ZERO;
        }
        Vec3 renderedOwner = owner.getPosition(partialTick);
        Vec3 renderedAnchor = entity.getPosition(partialTick);
        return renderedOwner.subtract(renderedAnchor);
    }

    private static Vec3 poseDirection(RaizanCleaveAnimation.WeaponPose pose) {
        Vector3f tip = pose.tipDirection();
        return new Vec3(tip.x(), tip.y(), tip.z()).normalize();
    }

    private static Vec3 perpendicularTo(Vec3 axis) {
        Vec3 reference = Math.abs(axis.y) < 0.88D
                ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        return axis.cross(reference).normalize();
    }

    private static void jaggedBolt(VertexConsumer out, Matrix4f matrix, Vec3 start, Vec3 end,
                                   int segments, float width, int color, float alpha, long seed) {
        Vec3 previous = start;
        Vec3 direction = end.subtract(start);
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 second = direction.normalize().cross(side).normalize();
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float envelope = Mth.sin(t * Mth.PI);
            double jitterA = (random01(seed, i * 2) - 0.5D) * 0.58D * envelope;
            double jitterB = (random01(seed, i * 2 + 1) - 0.5D) * 0.58D * envelope;
            Vec3 current = start.lerp(end, t).add(side.scale(jitterA)).add(second.scale(jitterB));
            glowingLightningSegment(out, matrix, previous, current, width, color, alpha);
            previous = current;
        }
    }

    private static void glowingLightningSegment(VertexConsumer out, Matrix4f matrix,
                                                 Vec3 start, Vec3 end, float width,
                                                 int accent, float alpha) {
        // Broad violet haze, saturated electrical body and a continuous white core.
        // Keeping these as distinct meshes also preserves the glow in shader fallback mode.
        beamCross(out, matrix, start, end, width * 7.2F,
                LIGHTNING_HALO, alpha * 0.24F);
        beamCross(out, matrix, start, end, width * 4.1F,
                LIGHTNING_MIST, alpha * 0.30F);
        beamCross(out, matrix, start, end, width * 2.15F,
                accent == WHITE ? PURPLE : LIGHTNING_BODY, alpha * 0.62F);
        beamCross(out, matrix, start, end, width * 0.78F,
                WHITE, alpha);
    }

    private static void layeredHeartBeam(VertexConsumer out, Matrix4f matrix,
                                         Vec3 start, Vec3 end, float coreWidth,
                                         int bodyColor, float alpha, float haloScale) {
        beam(out, matrix, start, end, coreWidth * haloScale,
                HEART_HALO, alpha * 0.14F);
        beam(out, matrix, start, end, coreWidth * 2.65F,
                bodyColor, alpha * 0.44F);
        beam(out, matrix, start, end, coreWidth,
                WHITE, alpha * 0.96F);
    }

    private static void layeredSlashPlane(VertexConsumer out, Matrix4f matrix,
                                          Vec3 center, Vec3 direction, double length,
                                          float halfWidth, int bodyColor, float alpha) {
        Vec3 axis = direction.normalize();
        Vec3 normal = new Vec3(-axis.y, axis.x, 0.0D).normalize();
        Vec3 start = center.subtract(axis.scale(length * 0.5D));
        Vec3 end = center.add(axis.scale(length * 0.5D));

        // A tapered luminous plane reads as a blade-shaped cut instead of another wire.
        slashDiamond(out, matrix, start, center, end, normal, halfWidth * 1.85F,
                HEART_HALO, alpha * 0.16F);
        slashDiamond(out, matrix, start, center, end, normal, halfWidth,
                bodyColor, alpha * 0.58F);
        slashDiamond(out, matrix, start, center, end, normal, halfWidth * 0.34F,
                WHITE, alpha * 0.90F);
        beam(out, matrix, start, end, 0.030F, WHITE, alpha * 0.95F);
    }

    private static void slashDiamond(VertexConsumer out, Matrix4f matrix,
                                     Vec3 start, Vec3 center, Vec3 end, Vec3 normal,
                                     float halfWidth, int color, float alpha) {
        Vec3 spread = normal.scale(halfWidth);
        quad(out, matrix, start, center.add(spread), end, center.subtract(spread),
                color, alpha);
    }

    private static void residualHeartLine(VertexConsumer out, Matrix4f matrix,
                                          Vec3 start, Vec3 end, float width, float alpha) {
        // Residual cuts are red and sparse; omitting the white core prevents each one
        // from competing with the main horizontal slash.
        beam(out, matrix, start, end, width * 3.2F, HEART_HALO, alpha * 0.13F);
        beam(out, matrix, start, end, width, RED, alpha * 0.72F);
    }

    private static void triangleFrame(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                      double size, double rotation, int color, float alpha) {
        Vec3[] points = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            double angle = rotation + i * Mth.TWO_PI / 3.0D - Math.PI / 2.0D;
            points[i] = center.add(Math.cos(angle) * size, Math.sin(angle) * size, 0.0D);
        }
        for (int i = 0; i < 3; i++) {
            beam(out, matrix, points[i], points[(i + 1) % 3], 0.035F, color, alpha);
        }
    }

    private static void crossedCore(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                    float size, int color, float alpha) {
        quadFacingXY(out, matrix, center, size, color, alpha);
        quadFacingXZ(out, matrix, center, size, color, alpha * 0.72F);
        quadFacingYZ(out, matrix, center, size, WHITE, alpha * 0.48F);
    }

    private static void ring(VertexConsumer out, Matrix4f matrix, Vec3 center,
                             float radius, float width, int segments, int color,
                             float alpha, float rotation, RingPlane plane) {
        Vec3 previous = ringPoint(center, radius, rotation, plane);
        for (int i = 1; i <= segments; i++) {
            double angle = rotation + i * Mth.TWO_PI / segments;
            Vec3 current = ringPoint(center, radius, angle, plane);
            beam(out, matrix, previous, current, width, color, alpha);
            previous = current;
        }
    }

    private static Vec3 ringPoint(Vec3 center, float radius, double angle, RingPlane plane) {
        double a = Math.cos(angle) * radius;
        double b = Math.sin(angle) * radius;
        return switch (plane) {
            case XY -> center.add(a, b, 0.0D);
            case XZ -> center.add(a, 0.0D, b);
            case YZ -> center.add(0.0D, a, b);
        };
    }

    private static void diamond(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                float size, int color, float alpha, double rotation) {
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        Vec3 right = new Vec3(cos * size, sin * size, 0.0D);
        Vec3 up = new Vec3(-sin * size * 1.8D, cos * size * 1.8D, 0.0D);
        quad(out, matrix, center.subtract(right), center.add(up), center.add(right),
                center.subtract(up), color, alpha);
    }

    private static void shard3d(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                float size, int color, float alpha,
                                double yaw, double roll) {
        Vec3 axis = new Vec3(Math.cos(yaw) * Math.cos(roll), Math.sin(roll),
                Math.sin(yaw) * Math.cos(roll)).normalize();
        Vec3 side = axis.cross(Math.abs(axis.y) < 0.86D
                ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D)).normalize();
        Vec3 normal = axis.cross(side).normalize();
        Vec3 tip = axis.scale(size * 2.35D);
        Vec3 sideWidth = side.scale(size * 0.62D);
        Vec3 normalWidth = normal.scale(size * 0.62D);
        quad(out, matrix, center.subtract(tip), center.add(sideWidth),
                center.add(tip), center.subtract(sideWidth), color, alpha);
        quad(out, matrix, center.subtract(tip), center.add(normalWidth),
                center.add(tip), center.subtract(normalWidth), color, alpha * 0.82F);
        beam(out, matrix, center.subtract(tip), center.add(tip), size * 0.12F,
                WHITE, alpha * 0.70F);
    }

    private static void beamCross(VertexConsumer out, Matrix4f matrix, Vec3 start, Vec3 end,
                                  float width, int color, float alpha) {
        beam(out, matrix, start, end, width, color, alpha);
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        Vec3 side = direction.normalize().cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 perpendicular = direction.normalize().cross(side.normalize()).normalize().scale(width);
        quad(out, matrix, start.subtract(perpendicular), start.add(perpendicular),
                end.add(perpendicular), end.subtract(perpendicular), color, alpha);
    }

    private static void beam(VertexConsumer out, Matrix4f matrix, Vec3 start, Vec3 end,
                             float width, int color, float alpha) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D || alpha <= 0.001F) {
            return;
        }
        Vec3 reference = Math.abs(direction.normalize().y) < 0.90D
                ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = direction.cross(reference).normalize().scale(width);
        quad(out, matrix, start.subtract(side), start.add(side),
                end.add(side), end.subtract(side), color, alpha);
    }

    private static void quadFacingXY(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                     float size, int color, float alpha) {
        quad(out, matrix, center.add(-size, -size, 0.0D), center.add(-size, size, 0.0D),
                center.add(size, size, 0.0D), center.add(size, -size, 0.0D), color, alpha);
    }

    private static void quadFacingXZ(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                     float size, int color, float alpha) {
        quad(out, matrix, center.add(-size, 0.0D, -size), center.add(-size, 0.0D, size),
                center.add(size, 0.0D, size), center.add(size, 0.0D, -size), color, alpha);
    }

    private static void quadFacingYZ(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                     float size, int color, float alpha) {
        quad(out, matrix, center.add(0.0D, -size, -size), center.add(0.0D, size, -size),
                center.add(0.0D, size, size), center.add(0.0D, -size, size), color, alpha);
    }

    private static void quad(VertexConsumer out, Matrix4f matrix, Vec3 p0, Vec3 p1,
                             Vec3 p2, Vec3 p3, int color, float alpha) {
        quadUv(out, matrix, p0, p1, p2, p3, color, alpha,
                0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static void quadUv(VertexConsumer out, Matrix4f matrix, Vec3 p0, Vec3 p1,
                               Vec3 p2, Vec3 p3, int color, float alpha,
                               float u0, float u1, float v0, float v1) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        vertex(out, matrix, p0, red, green, blue, alpha, u0, v0);
        vertex(out, matrix, p1, red, green, blue, alpha, u0, v1);
        vertex(out, matrix, p2, red, green, blue, alpha, u1, v1);
        vertex(out, matrix, p3, red, green, blue, alpha, u1, v0);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 point,
                               float red, float green, float blue, float alpha,
                               float u, float v) {
        out.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F))
                .uv(u, v).endVertex();
    }

    private static float pulse(float frame, float period) {
        return 0.5F + 0.5F * Mth.sin(frame * Mth.TWO_PI / period);
    }

    private static float phaseOneBladeEnergy(float frame) {
        float appear = smooth(Mth.clamp(frame / 1.8F, 0.0F, 1.0F));
        float disappear = 1.0F - smooth(Mth.clamp((frame - 24.5F) / 3.0F, 0.0F, 1.0F));
        return appear * disappear * (0.86F + pulse(frame, 3.1F) * 0.14F);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float heartPulse(float frame, float birth, float peak, float end) {
        if (frame <= birth || frame >= end) {
            return 0.0F;
        }
        if (frame < peak) {
            return smooth((frame - birth) / Math.max(peak - birth, 1.0E-4F));
        }
        return 1.0F - smooth((frame - peak) / Math.max(end - peak, 1.0E-4F));
    }

    private static float gaussian(float value, float center, float width) {
        float x = (value - center) / Math.max(width, 1.0E-4F);
        return (float) Math.exp(-0.5F * x * x);
    }

    private static double random01(long seed, int salt) {
        long value = seed ^ (salt * 0x9E3779B97F4A7C15L);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static int scaledCount(int high) {
        return switch (KabladeClientConfig.RAIZAN_CLEAVE_QUALITY.get()) {
            case LOW -> Math.max(6, high / 3);
            case MEDIUM -> Math.max(10, high * 2 / 3);
            case HIGH -> high;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(RaizanCleaveEntity entity) {
        return EMPTY_TEXTURE;
    }

    private enum RingPlane {
        XY, XZ, YZ
    }
}

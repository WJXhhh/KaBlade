package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.renderer.RaidenCycloneOculusPipeline;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.network.RaidenCycloneEndPacket;
import com.wjx.kablade.network.RaidenCycloneFxPacket;
import com.wjx.kablade.slasharts.RaidenCycloneTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic, entity-free world renderer for Raiden's Cyclone. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaidenCycloneRenderer {

    private static final Map<Long, ActiveFx> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<Integer, Deque<TipSample>> TIP_HISTORY = new ConcurrentHashMap<>();
    private static final double MAX_RENDER_DISTANCE_SQR = 64.0D * 64.0D;
    private static final float[] FIELD_RADII = {1.05F, 1.48F, 2.00F, 2.42F};
    private static final int CYAN = 0x24DFFF;
    private static final int BLUE = 0x287CFF;
    private static final int WHITE = 0xEFFFFF;
    private static final int VIOLET = 0x596BFF;
    private static final int DARK = 0x101827;

    private RaidenCycloneRenderer() {
    }

    public static void start(RaidenCycloneFxPacket packet) {
        ACTIVE.put(packet.castId(), new ActiveFx(packet));
    }

    public static void stop(long castId, byte reason) {
        ActiveFx fx = ACTIVE.get(castId);
        Minecraft minecraft = Minecraft.getInstance();
        if (fx == null || minecraft.level == null) return;
        if (reason == RaidenCycloneEndPacket.COMPLETE) {
            fx.forcedEndTick = minecraft.level.getGameTime() + 1L;
        } else {
            fx.forcedEndTick = minecraft.level.getGameTime() + 4L;
        }
    }

    public static boolean isActive(int entityId) {
        return ACTIVE.values().stream().anyMatch(fx -> fx.casterId == entityId);
    }

    public static void recordBladeTip(int entityId, Vec3 worldTip) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        double time = minecraft.level.getGameTime() + minecraft.getFrameTime();
        Deque<TipSample> history = TIP_HISTORY.computeIfAbsent(entityId, key -> new ArrayDeque<>());
        TipSample last = history.peekLast();
        if (last != null && Math.abs(last.time - time) < 0.004D) return;
        history.addLast(new TipSample(time, worldTip));
        while (history.size() > 18 || (!history.isEmpty() && time - history.peekFirst().time > 8.0D)) {
            history.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (ACTIVE.isEmpty()) return;
        boolean oculusPath = ShaderCompat.shouldUseOculusPostPath();
        RenderLevelStageEvent.Stage expectedStage = oculusPath
                ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                : RenderLevelStageEvent.Stage.AFTER_PARTICLES;
        if (event.getStage() != expectedStage) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }

        cleanup(level);
        if (ACTIVE.isEmpty()) return;
        if (oculusPath) {
            RaidenCycloneOculusPipeline.render(event,
                    (bright, dark) -> renderAll(event, bright, dark));
            return;
        }
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer bright = buffers.getBuffer(KabladeRenderTypes.raidenCyclone());
        VertexConsumer dark = buffers.getBuffer(KabladeRenderTypes.raidenCycloneDark());
        renderAll(event, bright, dark);
        buffers.endBatch(KabladeRenderTypes.raidenCyclone());
        buffers.endBatch(KabladeRenderTypes.raidenCycloneDark());
    }

    private static void cleanup(ClientLevel level) {
        long now = level.getGameTime();
        Iterator<ActiveFx> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            ActiveFx fx = iterator.next();
            float seconds = (now - fx.serverStartGameTime) / 20.0F;
            if (seconds > RaidenCycloneTimeline.DURATION_SECONDS + 0.35F
                    || (fx.forcedEndTick != Long.MAX_VALUE && now > fx.forcedEndTick)) {
                iterator.remove();
            }
        }
    }

    private static void renderAll(RenderLevelStageEvent event,
                                  VertexConsumer bright, VertexConsumer dark) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        float partial = event.getPartialTick();
        double now = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        for (ActiveFx fx : new ArrayList<>(ACTIVE.values())) {
            Entity caster = level.getEntity(fx.casterId);
            Entity target = fx.targetId < 0 ? null : level.getEntity(fx.targetId);
            Vec3 casterPos = interpolated(caster, partial, fx.origin);
            Vec3 targetPos = interpolated(target, partial, fx.lastTarget);
            fx.lastCaster = casterPos;
            fx.lastTarget = targetPos;
            if (camera.distanceToSqr(targetPos) > MAX_RENDER_DISTANCE_SQR) continue;

            float seconds = (float) ((now - fx.serverStartGameTime) / 20.0D);
            if (seconds < -0.1F || seconds > RaidenCycloneTimeline.DURATION_SECONDS + 0.3F) continue;
            float forcedFade = fx.forcedEndTick == Long.MAX_VALUE
                    ? 1.0F
                    : Mth.clamp((float) (fx.forcedEndTick - now) / 4.0F, 0.0F, 1.0F);
            float endFade = 1.0F - RaidenCycloneTimeline.smooth(
                    (seconds - 4.42F) / (RaidenCycloneTimeline.DURATION_SECONDS - 4.42F));
            float globalAlpha = Math.min(forcedFade, seconds > 4.42F ? endFade : 1.0F);

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(targetPos.x - camera.x, targetPos.y - camera.y, targetPos.z - camera.z);
            Matrix4f matrix = poseStack.last().pose();
            renderSlashes(bright, dark, matrix, fx, seconds, globalAlpha);
            renderGroundField(bright, matrix, fx, seconds, globalAlpha);
            renderPillar(bright, matrix, fx, seconds, globalAlpha);
            renderLightning(bright, matrix, fx, target, seconds, globalAlpha);
            renderImpacts(bright, matrix, fx, casterPos, targetPos, seconds, globalAlpha);
            renderAfterimages(bright, dark, matrix, fx, seconds, globalAlpha);
            renderFragments(bright, dark, matrix, fx, seconds, globalAlpha);
            poseStack.popPose();
        }
    }

    private static void renderSlashes(VertexConsumer bright, VertexConsumer dark, Matrix4f matrix,
                                      ActiveFx fx, float seconds, float globalAlpha) {
        int segments = slashSegments();
        for (RaidenCycloneTimeline.SlashSpec spec : RaidenCycloneTimeline.SLASHES) {
            float age = seconds - spec.start();
            if (age < 0.0F || age > spec.life()) continue;
            float head = RaidenCycloneTimeline.smooth(age / spec.write());
            float wipe = age <= spec.eraseLead() ? 0.0F
                    : RaidenCycloneTimeline.smooth((age - spec.eraseLead())
                    / Math.max(spec.eraseEnd() - spec.eraseLead(), 1.0E-4F));
            float tail = Math.max(head - spec.maxSpan(), wipe * head);
            if (head - tail < 0.002F) continue;
            float fade = 1.0F - RaidenCycloneTimeline.smooth(
                    (age - spec.eraseEnd()) / Math.max(spec.life() - spec.eraseEnd(), 1.0E-4F));
            float alpha = fade * globalAlpha;

            emitSlashLayer(bright, matrix, fx, spec, head, tail, 1.00F,
                    spec.width() * 1.20F, BLUE, alpha * 0.22F, segments, 0.0F, false);
            emitSlashLayer(bright, matrix, fx, spec, head, tail, 0.80F,
                    spec.width() * 0.82F, CYAN, alpha * 0.72F, segments, 0.0F, false);
            emitSlashLayer(bright, matrix, fx, spec, head, tail, 0.36F,
                    spec.width() * 0.30F, WHITE, alpha * 0.98F, segments, 0.015F, false);
            emitSlashLayer(bright, matrix, fx, spec, head, tail, 0.62F,
                    spec.width() * 0.12F, 0x9AFFFF, alpha * 0.82F, segments, spec.width() * 0.42F, false);
            if (spec.dark()) {
                emitSlashLayer(dark, matrix, fx, spec, head, tail, 0.70F,
                        spec.width() * 1.12F, DARK, alpha * 0.72F, segments, -0.08F, true);
            }
        }
    }

    private static void emitSlashLayer(VertexConsumer out, Matrix4f matrix, ActiveFx fx,
                                       RaidenCycloneTimeline.SlashSpec spec,
                                       float head, float tail, float lengthFactor, float width,
                                       int color, float alpha, int segments, float radialOffset,
                                       boolean serrated) {
        float layerTail = Math.max(tail, head - spec.maxSpan() * lengthFactor);
        for (int i = 0; i < segments; i++) {
            float q0 = i / (float) segments;
            float q1 = (i + 1) / (float) segments;
            float u0 = Mth.lerp(q0, layerTail, head);
            float u1 = Mth.lerp(q1, layerTail, head);
            float jag0 = serrated ? (float) Math.sin(u0 * 71.0F) * width * 0.14F : 0.0F;
            float jag1 = serrated ? (float) Math.sin(u1 * 71.0F) * width * 0.14F : 0.0F;
            Vec3 a0 = slashPoint(fx, spec, u0, radialOffset - width * 0.5F - jag0);
            Vec3 b0 = slashPoint(fx, spec, u0, radialOffset + width * 0.5F + jag0);
            Vec3 a1 = slashPoint(fx, spec, u1, radialOffset - width * 0.5F - jag1);
            Vec3 b1 = slashPoint(fx, spec, u1, radialOffset + width * 0.5F + jag1);
            quad(out, matrix, a0, a1, b1, b0, color, alpha, q0, q1);
        }
    }

    private static Vec3 slashPoint(ActiveFx fx, RaidenCycloneTimeline.SlashSpec spec,
                                   float u, float radiusOffset) {
        float theta = spec.angle() + spec.direction() * spec.arc() * u;
        float radius = spec.radius() + radiusOffset;
        double x = Math.cos(theta) * radius;
        double y = spec.y() + Math.sin(u * Math.PI) * spec.lift();
        double z = Math.sin(theta) * radius;

        double cosX = Math.cos(spec.rotateX()), sinX = Math.sin(spec.rotateX());
        double ry = y * cosX - z * sinX;
        double rz = y * sinX + z * cosX;
        double cosZ = Math.cos(spec.rotateZ()), sinZ = Math.sin(spec.rotateZ());
        double rx = x * cosZ - ry * sinZ;
        ry = x * sinZ + ry * cosZ;
        return rotateBasis(fx, rx + spec.centerX(), ry, rz + spec.centerZ());
    }

    private static void renderGroundField(VertexConsumer out, Matrix4f matrix, ActiveFx fx,
                                          float seconds, float globalAlpha) {
        float alpha = RaidenCycloneTimeline.envelope(seconds, 0.38F, 0.62F, 4.54F) * globalAlpha;
        if (alpha <= 0.002F) return;
        float spin = seconds * 0.72F;
        emitRing(out, matrix, fx, 0.72F, 0.42F, 0.025F, BLUE, alpha * 0.12F, 64, -spin);
        for (int i = 0; i < FIELD_RADII.length; i++) {
            emitRing(out, matrix, fx, FIELD_RADII[i], 0.018F + i * 0.006F, 0.035F,
                    i % 2 == 0 ? CYAN : BLUE, alpha * (0.42F - i * 0.055F), 64,
                    spin * (i % 2 == 0 ? 1.0F : -0.8F));
        }
        emitRing(out, matrix, fx, 0.62F, 0.036F, 0.045F, WHITE, alpha * 0.58F, 56, -spin * 1.7F);
        for (int i = 0; i < 24; i++) {
            float angle = Mth.TWO_PI * i / 24.0F + spin * (i % 2 == 0 ? 1.0F : -1.0F);
            float inner = i % 2 == 0 ? 1.05F : 1.48F;
            float outer = i % 2 == 0 ? 2.00F : 2.42F;
            Vec3 p0 = rotateBasis(fx, Math.cos(angle) * inner, 0.032F, Math.sin(angle) * inner);
            Vec3 p1 = rotateBasis(fx, Math.cos(angle) * outer, 0.032F, Math.sin(angle) * outer);
            segment(out, matrix, p0, p1, 0.026F, i % 3 == 0 ? WHITE : BLUE, alpha * 0.28F);
        }
    }

    private static void renderPillar(VertexConsumer out, Matrix4f matrix, ActiveFx fx,
                                     float seconds, float globalAlpha) {
        float alpha = Math.max(
                RaidenCycloneTimeline.envelope(seconds, 2.10F, 2.28F, 3.24F),
                RaidenCycloneTimeline.envelope(seconds, 3.28F, 3.52F, 4.42F)) * globalAlpha;
        if (alpha <= 0.002F) return;
        int segments = pillarSegments();
        for (int sheet = 0; sheet < 5; sheet++) {
            float phase = Mth.TWO_PI * sheet / 5.0F + seconds * (1.8F + sheet * 0.08F);
            for (int i = 0; i < segments; i++) {
                float u0 = i / (float) segments;
                float u1 = (i + 1) / (float) segments;
                Vec3 p0 = helixPoint(fx, u0, phase, sheet);
                Vec3 p1 = helixPoint(fx, u1, phase, sheet);
                segment(out, matrix, p0, p1, 0.10F + u0 * 0.055F,
                        sheet % 2 == 0 ? CYAN : VIOLET, alpha * (0.38F + u0 * 0.32F));
            }
        }
        Vec3 base = rotateBasis(fx, 0.0D, 0.04D, 0.0D);
        Vec3 top = rotateBasis(fx, 0.0D, 4.82D, 0.0D);
        segment(out, matrix, base, top, 0.18F, WHITE, alpha * 0.72F);
        segment(out, matrix, base, top, 0.42F, CYAN, alpha * 0.14F);
        for (int i = 0; i < 3; i++) {
            float pulse = 0.85F + 0.15F * Mth.sin(seconds * 8.0F + i);
            emitRing(out, matrix, fx, (0.72F + i * 0.44F) * pulse,
                    0.035F, 0.04F + i * 0.018F, i == 0 ? WHITE : CYAN,
                    alpha * (0.64F - i * 0.12F), 64, seconds * (i + 1));
        }
    }

    private static Vec3 helixPoint(ActiveFx fx, float u, float phase, int sheet) {
        float angle = phase + u * (Mth.TWO_PI * (2.35F + sheet * 0.07F));
        float radius = (0.32F + 0.72F * (float) Math.sin(u * Math.PI)) * (1.0F + sheet * 0.035F);
        return rotateBasis(fx, Math.cos(angle) * radius, 0.05F + u * 4.8F, Math.sin(angle) * radius);
    }

    private static void renderLightning(VertexConsumer out, Matrix4f matrix, ActiveFx fx,
                                        Entity target, float seconds, float globalAlpha) {
        float alpha = Math.max(
                RaidenCycloneTimeline.envelope(seconds, 2.06F, 2.18F, 2.44F),
                Math.max(RaidenCycloneTimeline.envelope(seconds, 2.88F, 3.02F, 3.25F),
                        RaidenCycloneTimeline.envelope(seconds, 3.30F, 3.52F, 4.42F))) * globalAlpha;
        if (alpha <= 0.002F) return;
        float height = target == null ? 1.9F : target.getBbHeight();
        float width = target == null ? 0.65F : target.getBbWidth();
        long refresh = (long) Math.floor(seconds * 28.0F);
        long baseSeed = fx.seed ^ refresh * 0x9E3779B97F4A7C15L;
        Vec3 chest = rotateBasis(fx, 0.0D, height * 0.56F, 0.0D);
        Vec3 sky = rotateBasis(fx,
                seededSigned(baseSeed, 1) * 0.72D, 7.4D,
                seededSigned(baseSeed, 2) * 0.72D);
        drawBolt(out, matrix, sky, chest, baseSeed, alpha, 0.105F, lightningSegments());

        Vec3[] anchors = {
                rotateBasis(fx, 0.0D, height * 0.92D, 0.0D),
                rotateBasis(fx, 0.0D, height * 0.34D, 0.0D),
                rotateBasis(fx, -width * 0.70D, height * 0.60D, 0.0D),
                rotateBasis(fx, width * 0.70D, height * 0.60D, 0.0D),
                rotateBasis(fx, -width * 0.32D, 0.08D, 0.0D),
                rotateBasis(fx, width * 0.32D, 0.08D, 0.0D)
        };
        for (int i = 0; i < anchors.length; i++) {
            drawBolt(out, matrix, chest, anchors[i], baseSeed + 31L * i,
                    alpha * (0.72F - i * 0.035F), 0.055F, 8);
        }
        for (int i = 0; i < 5; i++) {
            float angle = Mth.TWO_PI * i / 5.0F + seconds * 0.7F;
            Vec3 ground = rotateBasis(fx, Math.cos(angle) * (1.0F + i * 0.22F), 0.04F,
                    Math.sin(angle) * (1.0F + i * 0.22F));
            drawBolt(out, matrix, anchors[4 + i % 2], ground, baseSeed + 131L * i,
                    alpha * 0.55F, 0.042F, 7);
        }
    }

    private static void drawBolt(VertexConsumer out, Matrix4f matrix, Vec3 from, Vec3 to,
                                 long seed, float alpha, float width, int segments) {
        Random random = new Random(seed);
        Vec3 previous = from;
        Vec3 delta = to.subtract(from);
        for (int i = 1; i <= segments; i++) {
            float u = i / (float) segments;
            Vec3 point = i == segments ? to : from.add(delta.scale(u));
            if (i < segments) {
                float spread = (float) Math.sin(u * Math.PI) * 0.28F;
                point = point.add((random.nextDouble() - 0.5D) * spread,
                        (random.nextDouble() - 0.5D) * spread,
                        (random.nextDouble() - 0.5D) * spread);
            }
            segment(out, matrix, previous, point, width * 3.2F, BLUE, alpha * 0.16F);
            segment(out, matrix, previous, point, width * 1.65F, CYAN, alpha * 0.62F);
            segment(out, matrix, previous, point, width * 0.55F, WHITE, alpha);
            previous = point;
        }
    }

    private static void renderImpacts(VertexConsumer out, Matrix4f matrix, ActiveFx fx,
                                      Vec3 casterWorld, Vec3 targetWorld,
                                      float seconds, float globalAlpha) {
        for (int hit = 0; hit < RaidenCycloneTimeline.HIT_SECONDS.length; hit++) {
            float age = seconds - RaidenCycloneTimeline.HIT_SECONDS[hit];
            if (age < -0.02F || age > 0.20F) continue;
            float alpha = (1.0F - RaidenCycloneTimeline.smooth(Math.max(age, 0.0F) / 0.20F)) * globalAlpha;
            Vec3 center = hit < 5
                    ? worldToLocal(fx, casterWorld.add(0.0D, 1.05D, 0.0D), targetWorld)
                    : rotateBasis(fx, 0.0D, 1.05D, 0.0D);
            float radius = 0.48F + Math.max(age, 0.0F) * 5.8F;
            emitLocalRing(out, matrix, center, radius, 0.045F, WHITE, alpha * 0.88F, 36,
                    hit * 0.71F);
            emitLocalRing(out, matrix, center, radius * 1.32F, 0.032F, CYAN, alpha * 0.56F, 36,
                    -hit * 0.43F);
            for (int ray = 0; ray < 12; ray++) {
                float angle = Mth.TWO_PI * ray / 12.0F + hit * 0.37F;
                float length = radius * (1.4F + (ray % 3) * 0.34F);
                Vec3 end = center.add(Math.cos(angle) * length,
                        Math.sin(angle * 2.0F) * radius * 0.34F,
                        Math.sin(angle) * length);
                segment(out, matrix, center, end, 0.028F + (ray % 2) * 0.012F,
                        ray % 4 == 0 ? WHITE : CYAN, alpha * 0.72F);
            }
        }
    }

    private static void renderAfterimages(VertexConsumer bright, VertexConsumer dark, Matrix4f matrix,
                                          ActiveFx fx, float seconds, float globalAlpha) {
        float alpha = Math.max(
                RaidenCycloneTimeline.envelope(seconds, 0.05F, 0.45F, 1.85F),
                RaidenCycloneTimeline.envelope(seconds, 2.65F, 3.28F, 4.30F)) * globalAlpha;
        if (alpha <= 0.002F) return;
        for (int ghost = 0; ghost < 5; ghost++) {
            float sample = Math.max(0.0F, seconds - (ghost + 1) * 0.052F);
            RaidenCycloneTimeline.LocalPose pose = RaidenCycloneTimeline.samplePlayer(sample);
            Vec3 feet = rotateBasis(fx, pose.x(), 0.02D, pose.z());
            float a = alpha * (0.48F - ghost * 0.072F);
            Vec3 pelvis = feet.add(0.0D, 0.88D, 0.0D);
            Vec3 chest = feet.add(0.0D, 1.45D, 0.0D);
            Vec3 head = feet.add(0.0D, 1.93D, 0.0D);
            segment(dark, matrix, feet, chest, 0.22F, DARK, a * 0.50F);
            segment(bright, matrix, feet, chest, 0.065F, CYAN, a * 0.42F);
            segment(bright, matrix, chest, head, 0.12F, 0x79EFFF, a * 0.34F);
            double yaw = pose.yaw() + fx.basisRotation;
            Vec3 hand = chest.add(Math.cos(yaw) * 0.38D, -0.10D, Math.sin(yaw) * 0.38D);
            Vec3 tip = hand.add(Math.cos(yaw - 0.52D) * 2.55D,
                    0.24D + Math.sin(sample * 7.0F) * 0.35D,
                    Math.sin(yaw - 0.52D) * 2.55D);
            segment(bright, matrix, hand, tip, 0.075F, WHITE, a * 0.76F);
            segment(bright, matrix, hand, tip, 0.19F, CYAN, a * 0.22F);
        }

        Deque<TipSample> history = TIP_HISTORY.get(fx.casterId);
        Vec3 previousTip = null;
        if (history != null && history.size() >= 2) {
            for (TipSample tipSample : history) {
                Vec3 tip = worldToLocal(fx, tipSample.position, fx.lastTarget);
                if (previousTip != null) {
                    segment(bright, matrix, previousTip, tip, 0.045F, WHITE, alpha * 0.76F);
                    segment(bright, matrix, previousTip, tip, 0.12F, CYAN, alpha * 0.20F);
                }
                previousTip = tip;
            }
        } else {
            int samples = 10;
            for (int i = samples - 1; i >= 0; i--) {
                float sample = Math.max(0.0F, seconds - i * 0.026F);
                RaidenCycloneTimeline.LocalPose pose = RaidenCycloneTimeline.samplePlayer(sample);
                double yaw = pose.yaw();
                Vec3 tip = rotateBasis(fx,
                        pose.x() + Math.cos(yaw) * 2.45D,
                        1.25D + Math.sin(sample * 7.0F) * 0.34D,
                        pose.z() + Math.sin(yaw) * 2.45D);
                if (previousTip != null) segment(bright, matrix, previousTip, tip, 0.045F, WHITE, alpha * 0.76F);
                previousTip = tip;
            }
        }
    }

    private static void renderFragments(VertexConsumer bright, VertexConsumer dark, Matrix4f matrix,
                                        ActiveFx fx, float seconds, float globalAlpha) {
        for (int i = 0; i < fragmentCount(42); i++) {
            int hit = 5 + i % 6;
            float age = seconds - RaidenCycloneTimeline.HIT_SECONDS[hit];
            if (age < 0.0F || age > 0.92F) continue;
            Random random = new Random(fx.seed + i * 0x632BE5ABL);
            float angle = random.nextFloat() * Mth.TWO_PI;
            float speed = 0.8F + random.nextFloat() * 2.6F;
            Vec3 p = rotateBasis(fx,
                    Math.cos(angle) * speed * age,
                    0.85F + random.nextFloat() * 0.75F + age * (1.8F + random.nextFloat()) - age * age * 2.2F,
                    Math.sin(angle) * speed * age);
            Vec3 q = p.add(Math.cos(angle) * 0.16D, 0.08D, Math.sin(angle) * 0.16D);
            segment(bright, matrix, p, q, 0.025F, i % 5 == 0 ? WHITE : CYAN,
                    globalAlpha * (1.0F - age / 0.92F));
        }
        for (int i = 0; i < fragmentCount(32); i++) {
            int hit = 5 + i % 6;
            float age = seconds - RaidenCycloneTimeline.HIT_SECONDS[hit];
            if (age < 0.0F || age > 0.78F) continue;
            Random random = new Random(fx.seed ^ (i * 0x9E3779B9L));
            float angle = random.nextFloat() * Mth.TWO_PI;
            float speed = 0.55F + random.nextFloat() * 2.0F;
            Vec3 p = rotateBasis(fx, Math.cos(angle) * speed * age,
                    0.48F + random.nextFloat() * 1.25F + age * 0.72F,
                    Math.sin(angle) * speed * age);
            Vec3 q = p.add(Math.cos(angle + 0.4F) * 0.22D, -0.08D, Math.sin(angle + 0.4F) * 0.22D);
            segment(dark, matrix, p, q, 0.055F, DARK, globalAlpha * 0.72F * (1.0F - age / 0.78F));
        }

        float residue = RaidenCycloneTimeline.envelope(seconds, 3.70F, 4.40F, 5.04F) * globalAlpha;
        if (residue <= 0.002F) return;
        int motes = fragmentCount(90);
        for (int i = 0; i < motes; i++) {
            float u = i / (float) Math.max(motes - 1, 1);
            float angle = i * 2.399F + seconds * (0.15F + (i % 5) * 0.012F);
            float radius = 0.55F + u * 2.55F;
            Vec3 p = rotateBasis(fx, Math.cos(angle) * radius,
                    0.25F + u * 1.55F + Math.sin(seconds * 1.4F + i) * 0.18F,
                    Math.sin(angle) * radius);
            Vec3 q = p.add(0.0D, 0.035D + (i % 3) * 0.012D, 0.0D);
            segment(bright, matrix, p, q, 0.012F, i % 7 == 0 ? WHITE : CYAN, residue * 0.54F);
        }
    }

    private static void emitRing(VertexConsumer out, Matrix4f matrix, ActiveFx fx,
                                 float radius, float width, float y, int color, float alpha,
                                 int segments, float phase) {
        for (int i = 0; i < segments; i++) {
            float a0 = phase + Mth.TWO_PI * i / segments;
            float a1 = phase + Mth.TWO_PI * (i + 1) / segments;
            Vec3 p0 = rotateBasis(fx, Math.cos(a0) * (radius - width), y, Math.sin(a0) * (radius - width));
            Vec3 p1 = rotateBasis(fx, Math.cos(a1) * (radius - width), y, Math.sin(a1) * (radius - width));
            Vec3 p2 = rotateBasis(fx, Math.cos(a1) * (radius + width), y, Math.sin(a1) * (radius + width));
            Vec3 p3 = rotateBasis(fx, Math.cos(a0) * (radius + width), y, Math.sin(a0) * (radius + width));
            quad(out, matrix, p0, p1, p2, p3, color, alpha, i / (float) segments, (i + 1) / (float) segments);
        }
    }

    private static void emitLocalRing(VertexConsumer out, Matrix4f matrix, Vec3 center,
                                      float radius, float width, int color, float alpha,
                                      int segments, float phase) {
        for (int i = 0; i < segments; i++) {
            float a0 = phase + Mth.TWO_PI * i / segments;
            float a1 = phase + Mth.TWO_PI * (i + 1) / segments;
            Vec3 p0 = center.add(Math.cos(a0) * (radius - width), 0.0D, Math.sin(a0) * (radius - width));
            Vec3 p1 = center.add(Math.cos(a1) * (radius - width), 0.0D, Math.sin(a1) * (radius - width));
            Vec3 p2 = center.add(Math.cos(a1) * (radius + width), 0.0D, Math.sin(a1) * (radius + width));
            Vec3 p3 = center.add(Math.cos(a0) * (radius + width), 0.0D, Math.sin(a0) * (radius + width));
            quad(out, matrix, p0, p1, p2, p3, color, alpha, i / (float) segments, (i + 1) / (float) segments);
        }
    }

    private static void segment(VertexConsumer out, Matrix4f matrix, Vec3 from, Vec3 to,
                                float width, int color, float alpha) {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0E-7D || alpha <= 0.001F) return;
        Vec3 side = delta.cross(new Vec3(0.37D, 0.83D, 0.41D));
        if (side.lengthSqr() < 1.0E-7D) side = delta.cross(new Vec3(0.0D, 1.0D, 0.0D));
        side = side.normalize().scale(width);
        quad(out, matrix, from.subtract(side), to.subtract(side), to.add(side), from.add(side),
                color, alpha, 0.0F, 1.0F);
        Vec3 back = delta.cross(side).normalize().scale(width * 0.65F);
        quad(out, matrix, from.subtract(back), to.subtract(back), to.add(back), from.add(back),
                color, alpha * 0.72F, 0.0F, 1.0F);
    }

    private static void quad(VertexConsumer out, Matrix4f matrix,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             int color, float alpha, float u0, float u1) {
        vertex(out, matrix, p0, color, alpha, u0, 0.0F);
        vertex(out, matrix, p1, color, alpha, u1, 0.0F);
        vertex(out, matrix, p2, color, alpha, u1, 1.0F);
        vertex(out, matrix, p3, color, alpha, u0, 1.0F);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p,
                               int color, float alpha, float u, float v) {
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .color(r, g, b, Mth.clamp(alpha, 0.0F, 1.0F)).uv(u, v).endVertex();
    }

    private static Vec3 rotateBasis(ActiveFx fx, double x, double y, double z) {
        double cos = Math.cos(fx.basisRotation);
        double sin = Math.sin(fx.basisRotation);
        return new Vec3(x * cos - z * sin, y, x * sin + z * cos);
    }

    private static Vec3 worldToLocal(ActiveFx fx, Vec3 world, Vec3 targetWorld) {
        return world.subtract(targetWorld);
    }

    private static Vec3 interpolated(Entity entity, float partial, Vec3 fallback) {
        return entity == null ? fallback : new Vec3(
                Mth.lerp(partial, entity.xOld, entity.getX()),
                Mth.lerp(partial, entity.yOld, entity.getY()),
                Mth.lerp(partial, entity.zOld, entity.getZ()));
    }

    private static double seededSigned(long seed, int salt) {
        long x = seed + salt * 0x9E3779B97F4A7C15L;
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return ((x >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private static int slashSegments() {
        return switch (KabladeClientConfig.RAIDEN_CYCLONE_QUALITY.get()) {
            case LOW -> 20;
            case MEDIUM -> 32;
            case HIGH -> 48;
        };
    }

    private static int lightningSegments() {
        return switch (KabladeClientConfig.RAIDEN_CYCLONE_QUALITY.get()) {
            case LOW -> 7;
            case MEDIUM -> 10;
            case HIGH -> 14;
        };
    }

    private static int pillarSegments() {
        return switch (KabladeClientConfig.RAIDEN_CYCLONE_QUALITY.get()) {
            case LOW -> 36;
            case MEDIUM -> 56;
            case HIGH -> 80;
        };
    }

    private static int fragmentCount(int high) {
        return switch (KabladeClientConfig.RAIDEN_CYCLONE_QUALITY.get()) {
            case LOW -> Math.max(8, high / 3);
            case MEDIUM -> Math.max(12, high * 2 / 3);
            case HIGH -> high;
        };
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!KabladeClientConfig.RAIDEN_CYCLONE_CAMERA_SHAKE.get()) return;
        Feedback feedback = strongestFeedback(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback == null || feedback.impact < 0.002F) return;
        float accessibility = Minecraft.getInstance().options.damageTiltStrength().get().floatValue();
        float strength = feedback.impact * feedback.distanceFade * accessibility;
        event.setYaw(event.getYaw() + Mth.sin(feedback.time * 47.31F + feedback.seedPhase) * 0.88F * strength);
        event.setPitch(event.getPitch() + Mth.sin(feedback.time * 71.93F + feedback.seedPhase * 1.7F) * 0.68F * strength);
        event.setRoll(event.getRoll() + Mth.sin(feedback.time * 39.17F + feedback.seedPhase * 0.7F) * 0.46F * strength);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        Feedback feedback = strongestFeedback(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback == null) return;
        event.setFOV(event.getFOV() - feedback.impact * feedback.distanceFade * 1.85F);
    }

    @SubscribeEvent
    public static void guiFlash(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameRenderer == null) return;
        Feedback feedback = strongestFeedback(minecraft.gameRenderer.getMainCamera().getPosition(), event.getPartialTick());
        if (feedback == null) return;
        float scale = minecraft.options.screenEffectScale().get().floatValue();
        if (KabladeClientConfig.RAIDEN_CYCLONE_REDUCED_FLASH.get()) scale *= 0.28F;
        int alpha = (int) (Mth.clamp(feedback.impact * feedback.distanceFade * scale * 0.22F,
                0.0F, 0.22F) * 255.0F);
        if (alpha <= 0) return;
        event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), alpha << 24 | 0xEFFFFF);
    }

    private static Feedback strongestFeedback(Vec3 camera, float partial) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        float now = minecraft.level.getGameTime() + partial;
        Feedback best = null;
        float bestScore = 0.0F;
        for (ActiveFx fx : ACTIVE.values()) {
            float seconds = (now - fx.serverStartGameTime) / 20.0F;
            float impact = 0.0F;
            for (int i = 0; i < RaidenCycloneTimeline.HIT_SECONDS.length; i++) {
                float width = i < 5 ? 0.028F : (i == 5 || i == 10 ? 0.055F : 0.042F);
                impact = Math.max(impact, RaidenCycloneTimeline.gaussian(
                        seconds, RaidenCycloneTimeline.HIT_SECONDS[i], width));
            }
            double distance = camera.distanceTo(fx.lastTarget);
            float fade = 1.0F - RaidenCycloneTimeline.smooth((float) ((distance - 5.0D) / 43.0D));
            float score = impact * fade;
            if (score > bestScore) {
                bestScore = score;
                best = new Feedback(seconds, impact, fade,
                        (fx.seed & 0xFFFFL) / 65535.0F * Mth.TWO_PI);
            }
        }
        return best;
    }

    private static final class ActiveFx {
        private final long castId;
        private final int casterId;
        private final int targetId;
        private final long serverStartGameTime;
        private final long seed;
        private final Vec3 origin;
        private final float basisRotation;
        private Vec3 lastCaster;
        private Vec3 lastTarget;
        private long forcedEndTick = Long.MAX_VALUE;

        private ActiveFx(RaidenCycloneFxPacket packet) {
            this.castId = packet.castId();
            this.casterId = packet.casterId();
            this.targetId = packet.targetId();
            this.serverStartGameTime = packet.serverStartGameTime();
            this.seed = packet.seed();
            this.origin = new Vec3(packet.originX(), packet.originY(), packet.originZ());
            this.lastCaster = origin;
            this.lastTarget = new Vec3(packet.targetX(), packet.targetY(), packet.targetZ());
            this.basisRotation = packet.basisRotation();
        }
    }

    private record Feedback(float time, float impact, float distanceFade, float seedPhase) {
    }

    private record TipSample(double time, Vec3 position) {
    }
}

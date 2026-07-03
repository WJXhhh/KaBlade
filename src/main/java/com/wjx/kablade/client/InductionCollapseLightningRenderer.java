package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.shader.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InductionCollapseLightningRenderer {

    private static final Map<Integer, ActiveEffect> ACTIVE = new ConcurrentHashMap<>();
    private static final int DRAGON_SEGMENTS = 15;
    private static final int AE2_SEGMENTS = 5;
    private static final int CAGE_RIBS = 6;
    private static final double REFRESH_TICKS = 1.5D;
    private static final float PURPLE_R = 0.70F;
    private static final float PURPLE_G = 0.45F;
    private static final float PURPLE_B = 0.89F;
    private static final float CORE_R = 0.88F;
    private static final float CORE_G = 0.97F;
    private static final float CORE_B = 1.0F;
    private static final float BLUE_R = 0.05F;
    private static final float BLUE_G = 0.55F;
    private static final float BLUE_B = 1.0F;

    private InductionCollapseLightningRenderer() {
    }

    public static void start(int entityId, int duration) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        ACTIVE.compute(entityId, (id, previous) -> {
            if (previous == null) {
                return new ActiveEffect(now, now + duration, mc.level.random.nextLong());
            }
            previous.endTick = Math.max(previous.endTick, now + duration);
            return previous;
        });
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }

        long tick = level.getGameTime();
        float partial = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(KabladeRenderTypes.inductionCollapse());

        Iterator<Map.Entry<Integer, ActiveEffect>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ActiveEffect> entry = iterator.next();
            ActiveEffect effect = entry.getValue();
            if (tick > effect.endTick) {
                iterator.remove();
                continue;
            }

            Entity target = level.getEntity(entry.getKey());
            if (target == null || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            double x = Mth.lerp(partial, target.xOld, target.getX());
            double y = Mth.lerp(partial, target.yOld, target.getY());
            double z = Mth.lerp(partial, target.zOld, target.getZ());
            double age = tick + partial - effect.startTick;
            float alpha = fade(age, effect.endTick - (tick + partial));
            if (alpha <= 0.01F) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
            renderEffect(vc, poseStack.last().pose(), target.getBbWidth(), target.getBbHeight(),
                    age, alpha * shaderBrightness(), effect.seed);
            poseStack.popPose();
        }

        buffers.endBatch(KabladeRenderTypes.inductionCollapse());
    }

    private static float fade(double age, double remaining) {
        return Mth.clamp((float) age / 2.0F, 0.0F, 1.0F)
                * Mth.clamp((float) remaining / 6.0F, 0.0F, 1.0F);
    }

    private static void renderEffect(VertexConsumer vc, Matrix4f mat, float width, float height,
                                     double age, float alpha, long seed) {
        long refresh = (long) Math.floor(age / REFRESH_TICKS);
        Random random = new Random(seed ^ refresh * 0x9E3779B97F4A7C15L);
        double radius = Math.max(0.82D, width * 1.62D);
        Vec3 core = new Vec3(0.0D, height * 0.52D, 0.0D);
        float pulse = 0.72F + 0.28F * Mth.sin((float) age * 2.7F);
        float flash = refresh % 3 == 0 ? 1.0F : 0.35F;

        renderCoreCage(vc, mat, random, radius, width, height, age, alpha * pulse, flash);
        renderImpactStar(vc, mat, core, radius, height, alpha * flash);

        int dragonBolts = refresh % 2 == 0 ? 7 : 5;
        for (int i = 0; i < dragonBolts; i++) {
            Vec3 from = shellPoint(random, radius, height);
            Vec3 to = corePoint(random, width, height);
            drawDragonBolt(vc, mat, from, to, random, alpha * (0.82F + random.nextFloat() * 0.28F));
        }

        int sideFlashes = refresh % 3 == 0 ? 10 : 7;
        for (int i = 0; i < sideFlashes; i++) {
            drawAe2SideFlash(vc, mat, random, radius, height, alpha);
        }

        if (refresh % 2 == 0 || flash > 0.9F) {
            Vec3 top = core.add(offset(random, radius * 0.18D, 0.0D, radius * 0.18D))
                    .add(0.0D, height * 0.62D, 0.0D);
            Vec3 bottom = core.add(offset(random, radius * 0.14D, 0.0D, radius * 0.14D))
                    .add(0.0D, -height * 0.54D, 0.0D);
            drawDragonBolt(vc, mat, top, bottom, random, alpha);
        }
    }

    private static float shaderBrightness() {
        return ShaderCompat.shouldUseOculusPostPath() ? 1.85F : 1.28F;
    }

    private static void renderCoreCage(VertexConsumer vc, Matrix4f mat, Random random, double radius,
                                       float width, float height, double age, float alpha, float flash) {
        Vec3 core = new Vec3(0.0D, height * 0.52D, 0.0D);
        float spin = (float) age * 0.18F;
        for (int i = 0; i < CAGE_RIBS; i++) {
            double phase = Math.PI * 2.0D * i / CAGE_RIBS + spin;
            Vec3 upper = new Vec3(Math.cos(phase) * radius * 0.78D,
                    height * (0.78D + random.nextDouble() * 0.10D),
                    Math.sin(phase) * radius * 0.78D);
            Vec3 lower = new Vec3(Math.cos(phase + 0.52D) * radius * 0.66D,
                    height * (0.12D + random.nextDouble() * 0.10D),
                    Math.sin(phase + 0.52D) * radius * 0.66D);
            drawDragonBolt(vc, mat, upper, corePoint(random, width, height), random, alpha * 0.42F);
            drawDragonBolt(vc, mat, lower, corePoint(random, width, height), random, alpha * 0.36F);
            if (i % 2 == 0) {
                drawBranchBolt(vc, mat, upper, lower, random, alpha * 0.38F);
            }
        }

        double ringY = height * 0.52D;
        int segments = 36;
        Vec3 previousOuter = null;
        Vec3 firstOuter = null;
        for (int i = 0; i <= segments; i++) {
            double phase = Math.PI * 2.0D * i / segments - spin * 0.9D;
            double wobble = 1.0D + 0.055D * Math.sin(age * 1.4D + i * 0.83D);
            Vec3 point = new Vec3(Math.cos(phase) * radius * 0.74D * wobble,
                    ringY + Math.sin(phase * 2.0D + age * 0.7D) * height * 0.035D,
                    Math.sin(phase) * radius * 0.74D * wobble);
            if (firstOuter == null) {
                firstOuter = point;
            }
            if (previousOuter != null) {
                boltSegment(vc, mat, previousOuter, point, 0.030F,
                        BLUE_R, BLUE_G, BLUE_B, alpha * 0.30F);
                if (i % 4 == 0) {
                    boltSegment(vc, mat, point, core, 0.042F,
                            CORE_R, CORE_G, CORE_B, alpha * 0.28F * flash);
                }
            }
            previousOuter = point;
        }
        if (previousOuter != null && firstOuter != null) {
            boltSegment(vc, mat, previousOuter, firstOuter, 0.030F,
                    BLUE_R, BLUE_G, BLUE_B, alpha * 0.30F);
        }
    }

    private static void renderImpactStar(VertexConsumer vc, Matrix4f mat, Vec3 core,
                                         double radius, float height, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        float wide = 0.32F + alpha * 0.08F;
        float tall = height * 0.62F;
        float horizontal = (float) radius * 1.18F;
        ribbon(vc, mat, core.add(-horizontal, 0.0D, 0.0D), core.add(horizontal, 0.0D, 0.0D),
                wide, CORE_R, CORE_G, CORE_B, alpha * 0.18F);
        ribbon(vc, mat, core.add(0.0D, -tall, 0.0D), core.add(0.0D, tall, 0.0D),
                wide * 0.55F, CORE_R, CORE_G, CORE_B, alpha * 0.22F);
        ribbon(vc, mat, core.add(0.0D, 0.0D, -horizontal), core.add(0.0D, 0.0D, horizontal),
                wide, PURPLE_R, PURPLE_G, PURPLE_B, alpha * 0.16F);
    }

    private static Vec3 shellPoint(Random random, double radius, float height) {
        double phase = random.nextDouble() * Math.PI * 2.0D;
        double y = 0.12D + height * (0.12D + random.nextDouble() * 0.76D);
        return new Vec3(Math.cos(phase) * radius, y, Math.sin(phase) * radius);
    }

    private static Vec3 corePoint(Random random, float width, float height) {
        return new Vec3(
                signed(random, width * 0.32D),
                height * (0.18D + random.nextDouble() * 0.64D),
                signed(random, width * 0.32D));
    }

    private static Vec3 offset(Random random, double x, double y, double z) {
        return new Vec3(signed(random, x), signed(random, y), signed(random, z));
    }

    private static double signed(Random random, double scale) {
        return (random.nextDouble() - 0.5D) * 2.0D * scale;
    }

    private static void drawDragonBolt(VertexConsumer vc, Matrix4f mat, Vec3 from, Vec3 to,
                                       Random random, float alpha) {
        Vec3 diff = to.subtract(from);
        double distance = Math.max(0.001D, diff.length());
        Vec3 perpendicular = Vec3.ZERO;
        Vec3 previous = from;

        for (int segment = 1; segment <= DRAGON_SEGMENTS; segment++) {
            double progress = segment / (double) DRAGON_SEGMENTS;
            Vec3 point = segment == DRAGON_SEGMENTS ? to : from.add(diff.scale(progress));
            if (segment < DRAGON_SEGMENTS) {
                double spread = Math.sin(Math.PI * progress) * distance * 0.25D;
                Vec3 randomSide = randomOrthogonal(random, diff);
                perpendicular = perpendicular.scale(0.8D)
                        .add(randomSide.scale(spread * gaussianish(random) * 0.2D));
                point = point.add(perpendicular);
            }

            float size = (float) (0.055D + (1.0D - progress) * 0.035D);
            boltSegment(vc, mat, previous, point, size * 5.8F, BLUE_R, BLUE_G, BLUE_B, alpha * 0.13F);
            boltSegment(vc, mat, previous, point, size * 3.2F, PURPLE_R, PURPLE_G, PURPLE_B, alpha * 0.55F);
            boltSegment(vc, mat, previous, point, size * 1.15F, CORE_R, CORE_G, CORE_B, alpha);
            boltSegment(vc, mat, previous.lerp(point, 0.12D), point.lerp(previous, 0.12D),
                    size * 0.38F, 1.0F, 1.0F, 1.0F, alpha);

            if (segment < DRAGON_SEGMENTS - 2 && random.nextFloat() < 0.25F * (1.0F - (float) progress)) {
                Vec3 branchEnd = point.add(randomOrthogonal(random, diff).scale(distance * 0.22D * (1.0D - progress)));
                drawBranchBolt(vc, mat, point, branchEnd, random, alpha * 0.72F);
            }
            previous = point;
        }
    }

    private static void drawBranchBolt(VertexConsumer vc, Matrix4f mat, Vec3 from, Vec3 to,
                                       Random random, float alpha) {
        Vec3 previous = from;
        for (int segment = 1; segment <= 4; segment++) {
            double progress = segment / 4.0D;
            Vec3 point = from.lerp(to, progress);
            if (segment < 4) {
                point = point.add(offset(random, 0.08D, 0.08D, 0.08D));
            }
            boltSegment(vc, mat, previous, point, 0.075F, BLUE_R, BLUE_G, BLUE_B, alpha * 0.20F);
            boltSegment(vc, mat, previous, point, 0.030F, CORE_R, CORE_G, CORE_B, alpha);
            previous = point;
        }
    }

    private static void drawAe2SideFlash(VertexConsumer vc, Matrix4f mat, Random random,
                                         double radius, float height, float alpha) {
        Vec3 from = shellPoint(random, radius * 0.92D, height);
        Vec3 to = from.add(offset(random, 0.38D, 0.38D, 0.38D));
        Vec3 step = to.subtract(from).scale(1.0D / (AE2_SEGMENTS - 1));
        double len = step.length();
        Vec3 previous = from;
        for (int segment = 1; segment < AE2_SEGMENTS; segment++) {
            Vec3 point = from.add(step.scale(segment))
                    .add(offset(random, len * 0.6D, len * 0.6D, len * 0.6D));
            boltSegment(vc, mat, previous, point, 0.065F, PURPLE_R, PURPLE_G, PURPLE_B, alpha * 0.20F);
            boltSegment(vc, mat, previous, point, 0.026F, CORE_R, CORE_G, CORE_B, alpha * 0.90F);
            previous = point;
        }
    }

    private static Vec3 randomOrthogonal(Random random, Vec3 vec) {
        Vec3 randomVec = new Vec3(
                random.nextDouble() - 0.5D,
                random.nextDouble() - 0.5D,
                random.nextDouble() - 0.5D);
        Vec3 orthogonal = vec.cross(randomVec);
        return orthogonal.lengthSqr() < 1.0e-6D ? new Vec3(0.0D, 1.0D, 0.0D) : orthogonal.normalize();
    }

    private static double gaussianish(Random random) {
        return random.nextDouble() + random.nextDouble() + random.nextDouble() - 1.5D;
    }

    private static void boltSegment(VertexConsumer vc, Matrix4f mat, Vec3 from, Vec3 to, float size,
                                    float r, float g, float b, float alpha) {
        Vec3 diff = to.subtract(from);
        if (diff.lengthSqr() < 1.0e-6D) {
            return;
        }
        Vec3 side = diff.cross(new Vec3(0.45D, 0.82D, 0.31D));
        if (side.lengthSqr() < 1.0e-6D) {
            side = diff.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        side = side.normalize().scale(size);
        Vec3 back = diff.cross(side).normalize().scale(size * 0.65F);

        quad(vc, mat, from.subtract(side), to.subtract(side), to.add(side), from.add(side), r, g, b, alpha);
        quad(vc, mat, from.subtract(back), to.subtract(back), to.add(back), from.add(back), r, g, b, alpha * 0.7F);
    }

    private static void ribbon(VertexConsumer vc, Matrix4f mat, Vec3 from, Vec3 to, float width,
                               float r, float g, float b, float alpha) {
        Vec3 diff = to.subtract(from);
        if (diff.lengthSqr() < 1.0e-6D) {
            return;
        }
        Vec3 side = diff.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0e-6D) {
            side = diff.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(width);
        quad(vc, mat, from.subtract(side), to.subtract(side), to.add(side), from.add(side), r, g, b, alpha);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float r, float g, float b, float alpha) {
        vertex(vc, mat, p0, r, g, b, alpha);
        vertex(vc, mat, p1, r, g, b, alpha);
        vertex(vc, mat, p2, r, g, b, alpha);
        vertex(vc, mat, p3, r, g, b, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, Vec3 p, float r, float g, float b, float alpha) {
        vc.vertex(mat, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, alpha).endVertex();
    }

    private static final class ActiveEffect {
        private final long startTick;
        private long endTick;
        private final long seed;

        private ActiveEffect(long startTick, long endTick, long seed) {
            this.startTick = startTick;
            this.endTick = endTick;
            this.seed = seed;
        }
    }
}

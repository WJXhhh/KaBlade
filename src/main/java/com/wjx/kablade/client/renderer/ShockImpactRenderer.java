package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.ShockImpactEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Honkai-style blade trail for Shock Impact.
 * <p>
 * The blade light is authored as a 3D Bezier stroke instead of a rotated dome:
 * low-left-front start, lifted front body, right-back shoulder-height finish.
 * The ribbon cross-section twists along the sweep so the band keeps real depth.
 */
public final class ShockImpactRenderer extends EntityRenderer<ShockImpactEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");
    private static final int TRAIL_SEGMENTS = 56;
    private static final int AFTERIMAGE_COUNT = 4;
    private static final int SPEED_LINE_COUNT = 26;
    private static final int FRAGMENT_COUNT = 22;
    private static final int SHARD_COUNT = 22;
    private static final float MAIN_THICKNESS = 0.46F;

    // --- Swing geometry -----------------------------------------------------
    // Local axes after yaw rotation: +Z is forward, +Y is up, and -X is the
    // player's right side. The centreline uses an explicit horizontal arc so
    // the sweep keeps a visible centre angle instead of flattening into a beam.
    private static final float ARC_RADIUS = 3.05F;
    private static final float ARC_CENTER_Z = -0.05F;
    private static final float ARC_START = 54.0F * Mth.DEG_TO_RAD;
    private static final float ARC_END = -140.0F * Mth.DEG_TO_RAD;
    private static final float ARC_START_Y = -0.95F;
    private static final float ARC_END_Y = 1.24F;
    private static final float ARC_LIFT = 0.48F;
    private static final float RIBBON_HALFWIDTH = 0.66F;
    // Helicoid twist: the cross-section frame rotates about the path by +/- this
    // many radians across the sweep, so the ribbon turns over and shows both faces.
    private static final float TWIST_AMP = 0.70F;

    public ShockImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ShockImpactEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float life = Math.max(1.0F, entity.getLifetime());
        float progress = Mth.clamp(age / life, 0.0F, 1.0F);
        float open = smootherStep(Mth.clamp(age / 2.6F, 0.0F, 1.0F));
        float fade = 1.0F - smootherStep(Mth.clamp((progress - 0.50F) / 0.50F, 0.0F, 1.0F));
        float alpha = open * fade;
        if (alpha <= 0.004F) {
            return;
        }

        float sweep = smootherStep(Mth.clamp((age - 0.35F) / 6.4F, 0.0F, 1.0F));
        float spark = Mth.sin(Mth.clamp((age - 1.0F) / 9.0F, 0.0F, 1.0F) * Mth.PI);
        float scale = entity.getScale() * (0.94F + sweep * 0.14F);

        poseStack.pushPose();
        // The dispatcher enters with the pose at the entity's own interpolated
        // position, which only updates once per tick and lags the smoothly
        // interpolated player -> jitter. Cancel it out and re-anchor onto the
        // owner's partialTick position so the trail tracks the player per frame.
        Entity owner = entity.getOwner();
        if (owner != null) {
            double entX = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double entY = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double entZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            double ownX = Mth.lerp(partialTick, owner.xOld, owner.getX());
            double ownY = Mth.lerp(partialTick, owner.yOld, owner.getY());
            double ownZ = Mth.lerp(partialTick, owner.zOld, owner.getZ());
            float yawRad = entity.getYRot() * Mth.DEG_TO_RAD;
            double forward = entity.getForwardOffset();
            double fx = -Mth.sin(yawRad);
            double fz = Mth.cos(yawRad);
            poseStack.translate(
                    ownX + fx * forward - entX,
                    ownY + entity.getUpOffset() - entY,
                    ownZ + fz * forward - entZ);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(0.0F, -0.18F + spark * 0.08F, 0.18F);
        poseStack.scale(scale, scale, scale);
        Matrix4f mat = poseStack.last().pose();

        for (int i = AFTERIMAGE_COUNT - 1; i >= 0; i--) {
            float imageAlpha = alpha * (0.075F + i * 0.026F) * (1.0F - progress * 0.50F);
            bladeTrail(vc(buffer), mat, sweep, 0.88F + i * 0.08F, imageAlpha, 1.0F,
                    MAIN_THICKNESS * 0.50F, -0.06F - i * 0.045F, -0.035F - i * 0.024F);
        }

        VertexConsumer vc = vc(buffer);
        bladeTrail(vc, mat, sweep, 0.98F, alpha * 0.18F, 1.0F, MAIN_THICKNESS * 0.58F, -0.12F, -0.035F);
        bladeTrail(vc, mat, sweep, 0.74F, alpha * 0.72F, 1.0F, MAIN_THICKNESS, 0.0F, 0.0F);
        bladeTrail(vc, mat, sweep, 0.30F, alpha, 2.2F, MAIN_THICKNESS * 0.54F, -0.045F, 0.0F);
        bladeTrail(vc, mat, sweep, 0.17F, alpha * 0.60F, 2.2F, MAIN_THICKNESS * 0.32F, 0.075F, 0.0F);
        bladeTrail(vc, mat, sweep, 0.076F, alpha, 3.4F, MAIN_THICKNESS * 0.18F, -0.085F, 0.0F);
        bladeTrail(vc, mat, sweep, 0.042F, alpha * 0.78F, 3.4F, MAIN_THICKNESS * 0.14F, 0.105F, 0.0F);

        edgeFlare(vc, mat, sweep, alpha * (0.72F + spark * 0.35F));
        speedLines(vc, mat, age, sweep, alpha, spark);
        fragments(vc, mat, age, sweep, alpha, spark);
        energyShards(vc, mat, age, sweep, alpha, spark);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static VertexConsumer vc(MultiBufferSource buffer) {
        return buffer.getBuffer(KabladeRenderTypes.shockImpact());
    }

    private static float visibleEnd(float sweep) {
        return Mth.clamp(0.10F + sweep * 0.98F, 0.0F, 1.0F);
    }

    /** Centreline point (the bright line of the crescent) at sweep time t. */
    private static Vector3f center(float t) {
        float angle = ARC_START + (ARC_END - ARC_START) * t;
        float y = Mth.lerp(t, ARC_START_Y, ARC_END_Y) + Mth.sin(t * Mth.PI) * ARC_LIFT;
        return new Vector3f(
                Mth.sin(angle) * ARC_RADIUS,
                y,
                Mth.cos(angle) * ARC_RADIUS + ARC_CENTER_Z);
    }

    /** Twisted cross-section frame at t: outW = across-the-ribbon (width), outB = thickness. */
    private static void frame(float t, Vector3f outW, Vector3f outB) {
        Vector3f tan = sweepTangent(t);
        float angle = ARC_START + (ARC_END - ARC_START) * t;
        Vector3f w = new Vector3f(Mth.sin(angle), 0.0F, Mth.cos(angle)).normalize();
        Vector3f b = new Vector3f(tan).cross(w).normalize();
        float phi = TWIST_AMP * Mth.sin((t - 0.5F) * Mth.PI);
        float cp = Mth.cos(phi);
        float sp = Mth.sin(phi);
        outW.set(w.x * cp - b.x * sp,
                w.y * cp - b.y * sp,
                w.z * cp - b.z * sp);
        outB.set(w.x * sp + b.x * cp,
                w.y * sp + b.y * cp,
                w.z * sp + b.z * cp);
    }

    private static Vector3f sweepTangent(float t) {
        Vector3f a = center(Mth.clamp(t - 0.012F, 0.0F, 1.0F));
        Vector3f b = center(Mth.clamp(t + 0.012F, 0.0F, 1.0F));
        return b.sub(a).normalize();
    }

    /**
     * One layer of the crescent. {@code uBase} selects the shader channel
     * (1.0 soft body / 2.2 core / 3.4 razor); the band is twisted in 3D.
     * {@code enLayer} nudges the layer along the plane normal to avoid z-fighting;
     * {@code echoBack}/{@code echoDrop} offset after-images backward/downward.
     */
    private static void bladeTrail(VertexConsumer vc, Matrix4f mat, float sweep, float widthScale,
                                   float alpha, float uBase, float thickness, float enLayer, float echoDrop) {
        float visibleEnd = visibleEnd(sweep);
        int visibleSegments = Math.max(2, Mth.ceil(TRAIL_SEGMENTS * visibleEnd));
        float half = thickness * 0.5F;
        Vector3f w0 = new Vector3f();
        Vector3f b0 = new Vector3f();
        Vector3f w1 = new Vector3f();
        Vector3f b1 = new Vector3f();
        for (int i = 0; i < visibleSegments; i++) {
            float t0 = i / (float) TRAIL_SEGMENTS;
            float t1 = (i + 1) / (float) TRAIL_SEGMENTS;
            if (t0 > visibleEnd) {
                break;
            }
            t1 = Math.min(t1, visibleEnd);

            Vector3f c0 = center(t0);
            Vector3f c1 = center(t1);
            frame(t0, w0, b0);
            frame(t1, w1, b1);
            float hw0 = RIBBON_HALFWIDTH * widthScale * trailWidth(t0);
            float hw1 = RIBBON_HALFWIDTH * widthScale * trailWidth(t1);
            float a0 = alpha * trailAlpha(t0, visibleEnd);
            float a1 = alpha * trailAlpha(t1, visibleEnd);
            float u0 = uBase + t0;
            float u1 = uBase + t1;

            // Layer/echo displacement of the whole slice (along thickness + down).
            float lx0 = c0.x + b0.x * enLayer;
            float ly0 = c0.y + b0.y * enLayer + echoDrop;
            float lz0 = c0.z + b0.z * enLayer;
            float lx1 = c1.x + b1.x * enLayer;
            float ly1 = c1.y + b1.y * enLayer + echoDrop;
            float lz1 = c1.z + b1.z * enLayer;

            // top = +width edge (v=0), bot = -width edge (v=1); front/back along thickness.
            float topFx0 = lx0 + w0.x * hw0 + b0.x * half, topFy0 = ly0 + w0.y * hw0 + b0.y * half, topFz0 = lz0 + w0.z * hw0 + b0.z * half;
            float topBx0 = lx0 + w0.x * hw0 - b0.x * half, topBy0 = ly0 + w0.y * hw0 - b0.y * half, topBz0 = lz0 + w0.z * hw0 - b0.z * half;
            float botFx0 = lx0 - w0.x * hw0 + b0.x * half, botFy0 = ly0 - w0.y * hw0 + b0.y * half, botFz0 = lz0 - w0.z * hw0 + b0.z * half;
            float botBx0 = lx0 - w0.x * hw0 - b0.x * half, botBy0 = ly0 - w0.y * hw0 - b0.y * half, botBz0 = lz0 - w0.z * hw0 - b0.z * half;
            float topFx1 = lx1 + w1.x * hw1 + b1.x * half, topFy1 = ly1 + w1.y * hw1 + b1.y * half, topFz1 = lz1 + w1.z * hw1 + b1.z * half;
            float topBx1 = lx1 + w1.x * hw1 - b1.x * half, topBy1 = ly1 + w1.y * hw1 - b1.y * half, topBz1 = lz1 + w1.z * hw1 - b1.z * half;
            float botFx1 = lx1 - w1.x * hw1 + b1.x * half, botFy1 = ly1 - w1.y * hw1 + b1.y * half, botFz1 = lz1 - w1.z * hw1 + b1.z * half;
            float botBx1 = lx1 - w1.x * hw1 - b1.x * half, botBy1 = ly1 - w1.y * hw1 - b1.y * half, botBz1 = lz1 - w1.z * hw1 - b1.z * half;

            // front
            quad(vc, mat,
                    topFx0, topFy0, topFz0, u0, 0.0F, a0,
                    topFx1, topFy1, topFz1, u1, 0.0F, a1,
                    botFx1, botFy1, botFz1, u1, 1.0F, a1,
                    botFx0, botFy0, botFz0, u0, 1.0F, a0);
            // back
            quad(vc, mat,
                    topBx0, topBy0, topBz0, u0, 0.0F, a0 * 0.82F,
                    botBx0, botBy0, botBz0, u0, 1.0F, a0 * 0.82F,
                    botBx1, botBy1, botBz1, u1, 1.0F, a1 * 0.82F,
                    topBx1, topBy1, topBz1, u1, 0.0F, a1 * 0.82F);
            // top rim
            quad(vc, mat,
                    topFx0, topFy0, topFz0, u0, 0.0F, a0 * 0.38F,
                    topFx1, topFy1, topFz1, u1, 0.0F, a1 * 0.38F,
                    topBx1, topBy1, topBz1, u1, 0.55F, a1 * 0.30F,
                    topBx0, topBy0, topBz0, u0, 0.55F, a0 * 0.30F);
            // bottom rim
            quad(vc, mat,
                    botFx0, botFy0, botFz0, u0, 1.0F, a0 * 0.34F,
                    botBx0, botBy0, botBz0, u0, 0.45F, a0 * 0.28F,
                    botBx1, botBy1, botBz1, u1, 0.45F, a1 * 0.28F,
                    botFx1, botFy1, botFz1, u1, 1.0F, a1 * 0.34F);
        }
    }

    private static float trailWidth(float t) {
        float body = Mth.sin(t * Mth.PI);
        float tailLift = 1.0F - smootherStep(Mth.clamp((t - 0.82F) / 0.18F, 0.0F, 1.0F));
        return (0.18F + (float) Math.pow(body, 0.58F) * 0.68F) * (0.72F + tailLift * 0.28F);
    }

    private static float trailAlpha(float t, float visibleEnd) {
        float head = smootherStep(Mth.clamp(t / Math.max(0.001F, visibleEnd), 0.0F, 1.0F));
        float tail = smootherStep(Mth.clamp(t / 0.22F, 0.0F, 1.0F));
        float endFade = 1.0F - smootherStep(Mth.clamp((t - 0.88F) / 0.12F, 0.0F, 1.0F));
        return (0.35F + head * 0.65F) * tail * endFade;
    }

    private static void edgeFlare(VertexConsumer vc, Matrix4f mat, float sweep, float alpha) {
        float t = Mth.clamp(visibleEnd(sweep) - 0.02F, 0.0F, 1.0F);
        Vector3f w = new Vector3f();
        Vector3f b = new Vector3f();
        frame(t, w, b);
        Vector3f c = center(t);
        float hw = RIBBON_HALFWIDTH * trailWidth(t);
        // tip = the outer (+width) edge of the leading slice; flare along the sweep.
        float tipx = c.x + w.x * hw, tipy = c.y + w.y * hw, tipz = c.z + w.z * hw;
        Vector3f tan = sweepTangent(t);
        float length = 0.64F + sweep * 0.46F;
        float width = 0.16F;
        float wx = b.x * width, wy = b.y * width, wz = b.z * width;
        float bx = tipx - tan.x * length * 0.34F, by = tipy - tan.y * length * 0.34F, bz = tipz - tan.z * length * 0.34F;
        float hx = tipx + tan.x * length, hy = tipy + tan.y * length, hz = tipz + tan.z * length;
        // Channel 2 (razor): U 3.4 -> 4.4 so shader u runs 0..1 along the flare.
        quad(vc, mat,
                bx + wx, by + wy, bz + wz, 3.4F, 0.0F, alpha * 0.62F,
                hx + wx * 0.25F, hy + wy * 0.25F, hz + wz * 0.25F, 4.4F, 0.0F, alpha,
                hx - wx * 0.25F, hy - wy * 0.25F, hz - wz * 0.25F, 4.4F, 1.0F, alpha,
                bx - wx, by - wy, bz - wz, 3.4F, 1.0F, alpha * 0.62F);
    }

    private static void speedLines(VertexConsumer vc, Matrix4f mat, float age, float sweep,
                                   float alpha, float spark) {
        float visibleEnd = visibleEnd(sweep);
        Vector3f w = new Vector3f();
        Vector3f b = new Vector3f();
        for (int i = 0; i < SPEED_LINE_COUNT; i++) {
            float t = Mth.clamp(0.18F + deterministic(i, 2.4F) * 0.78F, 0.0F, visibleEnd);
            frame(t, w, b);
            Vector3f c = center(t);
            float side = (deterministic(i, 3.8F) - 0.34F) * RIBBON_HALFWIDTH * 0.72F;
            float px = c.x + w.x * side, py = c.y + w.y * side, pz = c.z + w.z * side;
            Vector3f tan = sweepTangent(t);
            float drift = Mth.frac(age * 0.095F + deterministic(i, 7.1F));
            float length = 0.72F + deterministic(i, 8.9F) * 1.10F + spark * 0.58F;
            float width = 0.010F + deterministic(i, 10.6F) * 0.018F;
            px += tan.x * drift * 0.46F;
            py += tan.y * drift * 0.46F;
            pz += tan.z * drift * 0.46F;
            float wx = b.x * width, wy = b.y * width, wz = b.z * width;
            float a = alpha * (0.26F + spark * 0.22F);
            // Channel 3 (speed lines): U 6.0 -> 7.0.
            quad(vc, mat,
                    px - tan.x * length + wx, py - tan.y * length + wy, pz - tan.z * length + wz, 6.0F, 0.0F, alpha * 0.20F,
                    px + tan.x * length * 0.24F + wx, py + tan.y * length * 0.24F + wy, pz + tan.z * length * 0.24F + wz, 7.0F, 0.0F, a,
                    px + tan.x * length * 0.24F - wx, py + tan.y * length * 0.24F - wy, pz + tan.z * length * 0.24F - wz, 7.0F, 1.0F, a,
                    px - tan.x * length - wx, py - tan.y * length - wy, pz - tan.z * length - wz, 6.0F, 1.0F, alpha * 0.20F);
        }
    }

    private static void fragments(VertexConsumer vc, Matrix4f mat, float age, float sweep,
                                  float alpha, float spark) {
        float visibleEnd = visibleEnd(sweep);
        Vector3f w = new Vector3f();
        Vector3f b = new Vector3f();
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            float t = Mth.clamp(0.36F + deterministic(i, 12.3F) * 0.58F, 0.0F, visibleEnd);
            float life = Mth.frac(age * 0.055F + deterministic(i, 14.7F));
            frame(t, w, b);
            Vector3f c = center(t);
            float side = RIBBON_HALFWIDTH * (0.36F + deterministic(i, 16.2F) * 0.38F)
                    + life * (0.12F + spark * 0.16F);
            float depth = (deterministic(i, 17.4F) - 0.5F) * 0.30F - life * 0.08F;
            float px = c.x + w.x * side + b.x * depth;
            float py = c.y + w.y * side + b.y * depth;
            float pz = c.z + w.z * side + b.z * depth;
            float size = 0.034F + deterministic(i, 18.5F) * 0.088F;
            float a = alpha * (1.0F - smootherStep(life)) * (0.30F + deterministic(i, 19.7F) * 0.42F);
            square(vc, mat, px, py, pz, size, age * 0.08F + i * 0.77F, a);
        }
    }

    private static void energyShards(VertexConsumer vc, Matrix4f mat, float age, float sweep,
                                     float alpha, float spark) {
        float visibleEnd = visibleEnd(sweep);
        Vector3f w = new Vector3f();
        Vector3f b = new Vector3f();
        for (int i = 0; i < SHARD_COUNT; i++) {
            float t = Mth.clamp(0.48F + deterministic(i, 21.4F) * 0.48F, 0.0F, visibleEnd);
            float life = Mth.frac(age * 0.082F + deterministic(i, 22.8F));
            frame(t, w, b);
            Vector3f c = center(t);
            Vector3f tan = sweepTangent(t);
            float side = RIBBON_HALFWIDTH * (0.72F + deterministic(i, 24.1F) * 0.42F)
                    + life * (0.12F + spark * 0.14F);
            float depth = (deterministic(i, 25.7F) - 0.5F) * 0.34F;
            float lead = life * (0.84F + deterministic(i, 26.9F) * 0.86F);
            float px = c.x + w.x * side + b.x * depth + tan.x * lead;
            float py = c.y + w.y * side + b.y * depth + tan.y * lead
                    + 0.06F + deterministic(i, 27.5F) * 0.18F;
            float pz = c.z + w.z * side + b.z * depth + tan.z * lead;
            float size = 0.044F + deterministic(i, 28.6F) * 0.078F;
            float a = alpha * (1.0F - smootherStep(life)) * (0.30F + deterministic(i, 29.8F) * 0.42F);
            diamond(vc, mat, px, py, pz, size, age * 0.18F + i * 0.91F, a);
        }
    }

    private static void square(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float size, float rotation,
                               float alpha) {
        float c = Mth.cos(rotation);
        float s = Mth.sin(rotation);
        float hx = c * size;
        float hy = s * size;
        float px = -s * size;
        float py = c * size;
        // Channel 4 (fragments): U 8.0 -> 9.0.
        quad(vc, mat,
                x - hx - px, y - hy - py, z, 8.0F, 0.0F, alpha,
                x + hx - px, y + hy - py, z, 9.0F, 0.0F, alpha,
                x + hx + px, y + hy + py, z, 9.0F, 1.0F, alpha,
                x - hx + px, y - hy + py, z, 8.0F, 1.0F, alpha);
    }

    private static void diamond(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float size, float rotation,
                                float alpha) {
        float c = Mth.cos(rotation);
        float s = Mth.sin(rotation);
        float hx = c * size;
        float hy = s * size;
        float px = -s * size;
        float py = c * size;
        // Channel 5 (Honkai-like diamond shards): U 10.0 -> 11.0.
        quad(vc, mat,
                x - hx - px, y - hy - py, z, 10.0F, 0.0F, alpha,
                x + hx - px, y + hy - py, z, 11.0F, 0.0F, alpha,
                x + hx + px, y + hy + py, z, 11.0F, 1.0F, alpha,
                x - hx + px, y - hy + py, z, 10.0F, 1.0F, alpha);
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float deterministic(int index, float salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float u0, float v0, float a0,
                             float x1, float y1, float z1, float u1, float v1, float a1,
                             float x2, float y2, float z2, float u2, float v2, float a2,
                             float x3, float y3, float z3, float u3, float v3, float a3) {
        vertex(vc, mat, x0, y0, z0, u0, v0, a0);
        vertex(vc, mat, x1, y1, z1, u1, v1, a1);
        vertex(vc, mat, x2, y2, z2, u2, v2, a2);
        vertex(vc, mat, x3, y3, z3, u3, v3, a3);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z, float u, float v, float alpha) {
        vc.vertex(mat, x, y, z).color(0.34F, 0.92F, 1.0F, alpha).uv(u, v).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ShockImpactEntity entity) {
        return EMPTY_TEXTURE;
    }
}

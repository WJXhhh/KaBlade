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
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Honkai-style blade trail for Shock Impact.
 */
public final class ShockImpactRenderer extends EntityRenderer<ShockImpactEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");
    private static final int TRAIL_SEGMENTS = 44;
    private static final int AFTERIMAGE_COUNT = 3;
    private static final int SPEED_LINE_COUNT = 14;
    private static final int FRAGMENT_COUNT = 26;
    private static final float MAIN_THICKNESS = 0.62F;

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
        float open = smootherStep(Mth.clamp(age / 5.0F, 0.0F, 1.0F));
        float fade = 1.0F - smootherStep(Mth.clamp((progress - 0.58F) / 0.42F, 0.0F, 1.0F));
        float alpha = open * fade;
        if (alpha <= 0.004F) {
            return;
        }

        float sweep = smootherStep(Mth.clamp((age - 1.0F) / 12.0F, 0.0F, 1.0F));
        float spark = Mth.sin(Mth.clamp((age - 3.0F) / 16.0F, 0.0F, 1.0F) * Mth.PI);
        float scale = entity.getScale() * (0.88F + sweep * 0.16F);
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.shockImpact());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(0.0F, -0.18F + spark * 0.08F, 0.18F);
        poseStack.scale(scale, scale, scale);
        Matrix4f mat = poseStack.last().pose();

        for (int i = AFTERIMAGE_COUNT - 1; i >= 0; i--) {
            float offset = 0.16F + i * 0.18F;
            float imageAlpha = alpha * (0.16F + i * 0.055F) * (1.0F - progress * 0.34F);
            bladeTrail(vc, mat, sweep, 1.15F + i * 0.18F,
                    imageAlpha, 1.0F, -0.05F - i * 0.012F,
                    -offset, -offset * 0.30F, MAIN_THICKNESS * 0.70F,
                    0.38F + i * 0.26F);
        }

        bladeTrail(vc, mat, sweep, 1.0F, alpha * 0.70F, 1.0F, 0.000F, 0.0F, 0.0F,
                MAIN_THICKNESS, 0.0F);
        bladeTrail(vc, mat, sweep, 0.48F, alpha * 0.92F, 2.2F, 0.018F, 0.0F, 0.0F,
                MAIN_THICKNESS * 0.62F, -0.18F);
        bladeTrail(vc, mat, sweep, 0.30F, alpha * 0.45F, 2.2F, 0.012F, 0.0F, 0.0F,
                MAIN_THICKNESS * 0.48F, 0.42F);
        bladeTrail(vc, mat, sweep, 0.115F, alpha, 3.4F, 0.040F, 0.0F, 0.0F,
                MAIN_THICKNESS * 0.28F, -0.32F);
        bladeTrail(vc, mat, sweep, 0.080F, alpha * 0.55F, 3.4F, 0.030F, 0.0F, 0.0F,
                MAIN_THICKNESS * 0.22F, 0.54F);

        edgeFlare(vc, mat, sweep, alpha * (0.72F + spark * 0.35F));
        speedLines(vc, mat, age, alpha, spark);
        fragments(vc, mat, age, alpha, spark);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void bladeTrail(VertexConsumer vc, Matrix4f mat, float sweep, float widthScale,
                                   float alpha, float uBase, float z, float xOffset, float yOffset,
                                   float thickness, float depthOffset) {
        float visibleEnd = Mth.clamp(0.18F + sweep * 0.88F, 0.0F, 1.0F);
        int visibleSegments = Math.max(2, Mth.ceil(TRAIL_SEGMENTS * visibleEnd));
        float zFront = z + thickness * 0.5F;
        float zBack = z - thickness * 0.5F;
        for (int i = 0; i < visibleSegments; i++) {
            float t0 = i / (float) TRAIL_SEGMENTS;
            float t1 = (i + 1) / (float) TRAIL_SEGMENTS;
            if (t0 > visibleEnd) {
                break;
            }
            t1 = Math.min(t1, visibleEnd);

            Vector3f p0 = curve(t0, xOffset, yOffset, depthOffset);
            Vector3f p1 = curve(t1, xOffset, yOffset, depthOffset);
            Vector2f n0 = normal(t0);
            Vector2f n1 = normal(t1);
            float w0 = trailWidth(t0) * widthScale;
            float w1 = trailWidth(t1) * widthScale;
            float localAlpha0 = alpha * trailAlpha(t0, visibleEnd);
            float localAlpha1 = alpha * trailAlpha(t1, visibleEnd);

            float tx0 = p0.x + n0.x * w0;
            float ty0 = p0.y + n0.y * w0;
            float tx1 = p1.x + n1.x * w1;
            float ty1 = p1.y + n1.y * w1;
            float bx0 = p0.x - n0.x * w0;
            float by0 = p0.y - n0.y * w0;
            float bx1 = p1.x - n1.x * w1;
            float by1 = p1.y - n1.y * w1;
            float front0 = p0.z + zFront;
            float front1 = p1.z + zFront;
            float back0 = p0.z + zBack;
            float back1 = p1.z + zBack;

            quad(vc, mat,
                    tx0, ty0, front0, uBase + t0, 0.0F, localAlpha0,
                    tx1, ty1, front1, uBase + t1, 0.0F, localAlpha1,
                    bx1, by1, front1, uBase + t1, 1.0F, localAlpha1,
                    bx0, by0, front0, uBase + t0, 1.0F, localAlpha0);
            quad(vc, mat,
                    tx0, ty0, back0, uBase + t0, 0.0F, localAlpha0 * 0.82F,
                    bx0, by0, back0, uBase + t0, 1.0F, localAlpha0 * 0.82F,
                    bx1, by1, back1, uBase + t1, 1.0F, localAlpha1 * 0.82F,
                    tx1, ty1, back1, uBase + t1, 0.0F, localAlpha1 * 0.82F);
            quad(vc, mat,
                    tx0, ty0, front0, uBase + t0, 0.0F, localAlpha0 * 0.38F,
                    tx1, ty1, front1, uBase + t1, 0.0F, localAlpha1 * 0.38F,
                    tx1, ty1, back1, uBase + t1, 0.55F, localAlpha1 * 0.30F,
                    tx0, ty0, back0, uBase + t0, 0.55F, localAlpha0 * 0.30F);
            quad(vc, mat,
                    bx0, by0, front0, uBase + t0, 1.0F, localAlpha0 * 0.34F,
                    bx0, by0, back0, uBase + t0, 0.45F, localAlpha0 * 0.28F,
                    bx1, by1, back1, uBase + t1, 0.45F, localAlpha1 * 0.28F,
                    bx1, by1, front1, uBase + t1, 1.0F, localAlpha1 * 0.34F);
        }
    }

    private static Vector3f curve(float t, float xOffset, float yOffset, float zOffset) {
        float inv = 1.0F - t;
        float x = -(inv * inv * -2.45F + 2.0F * inv * t * 0.40F + t * t * 5.25F);
        float y = inv * inv * -1.08F + 2.0F * inv * t * 2.02F + t * t * 2.62F;
        float sag = Mth.sin(t * Mth.PI) * 0.34F;
        float depth = (1.0F - t) * 3.35F - t * 1.35F;
        return new Vector3f(x + xOffset, y + sag + yOffset, depth + zOffset);
    }

    private static Vector2f tangent(float t) {
        Vector3f a = curve(Mth.clamp(t - 0.012F, 0.0F, 1.0F), 0.0F, 0.0F, 0.0F);
        Vector3f b = curve(Mth.clamp(t + 0.012F, 0.0F, 1.0F), 0.0F, 0.0F, 0.0F);
        return new Vector2f(b.x - a.x, b.y - a.y).normalize();
    }

    private static Vector2f normal(float t) {
        Vector2f tan = tangent(t);
        return new Vector2f(-tan.y, tan.x);
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
        Vector3f tip = curve(Mth.clamp(0.76F + sweep * 0.23F, 0.0F, 1.0F), 0.0F, 0.0F, 0.0F);
        Vector2f tan = tangent(0.94F);
        Vector2f n = normal(0.94F);
        float length = 1.55F + sweep * 0.75F;
        float width = 0.11F + sweep * 0.08F;
        quad(vc, mat,
                tip.x - tan.x * length * 0.34F + n.x * width, tip.y - tan.y * length * 0.34F + n.y * width,
                tip.z + 0.065F, 4.6F, 0.0F, alpha * 0.62F,
                tip.x + tan.x * length + n.x * width * 0.25F, tip.y + tan.y * length + n.y * width * 0.25F,
                tip.z + 0.065F, 5.6F, 0.0F, alpha,
                tip.x + tan.x * length - n.x * width * 0.25F, tip.y + tan.y * length - n.y * width * 0.25F,
                tip.z + 0.065F, 5.6F, 1.0F, alpha,
                tip.x - tan.x * length * 0.34F - n.x * width, tip.y - tan.y * length * 0.34F - n.y * width,
                tip.z + 0.065F, 4.6F, 1.0F, alpha * 0.62F);
    }

    private static void speedLines(VertexConsumer vc, Matrix4f mat, float age, float alpha, float spark) {
        for (int i = 0; i < SPEED_LINE_COUNT; i++) {
            float t = 0.40F + deterministic(i, 2.4F) * 0.52F;
            Vector3f p = curve(t, -0.18F - deterministic(i, 3.8F) * 0.28F,
                    -0.10F + (deterministic(i, 5.2F) - 0.5F) * 0.30F,
                    (deterministic(i, 5.9F) - 0.5F) * 0.34F);
            Vector2f tan = tangent(t);
            Vector2f n = normal(t);
            float drift = Mth.frac(age * 0.045F + deterministic(i, 7.1F));
            float length = 0.44F + deterministic(i, 8.9F) * 0.72F + spark * 0.34F;
            float width = 0.014F + deterministic(i, 10.6F) * 0.024F;
            p.add(tan.x * drift * 0.22F, tan.y * drift * 0.22F, -drift * 0.10F);
            quad(vc, mat,
                    p.x - tan.x * length + n.x * width, p.y - tan.y * length + n.y * width,
                    p.z + 0.030F, 6.0F, 0.0F, alpha * 0.14F,
                    p.x + tan.x * length * 0.24F + n.x * width, p.y + tan.y * length * 0.24F + n.y * width,
                    p.z + 0.030F, 7.0F, 0.0F, alpha * (0.18F + spark * 0.16F),
                    p.x + tan.x * length * 0.24F - n.x * width, p.y + tan.y * length * 0.24F - n.y * width,
                    p.z + 0.030F, 7.0F, 1.0F, alpha * (0.18F + spark * 0.16F),
                    p.x - tan.x * length - n.x * width, p.y - tan.y * length - n.y * width,
                    p.z + 0.030F, 6.0F, 1.0F, alpha * 0.14F);
        }
    }

    private static void fragments(VertexConsumer vc, Matrix4f mat, float age, float alpha, float spark) {
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            float t = 0.58F + deterministic(i, 12.3F) * 0.36F;
            float life = Mth.frac(age * 0.030F + deterministic(i, 14.7F));
            Vector3f p = curve(t, 0.0F, 0.0F, 0.0F);
            Vector2f n = normal(t);
            Vector2f tan = tangent(t);
            float side = 0.28F + deterministic(i, 16.2F) * 0.58F + life * (0.16F + spark * 0.22F);
            p.add(n.x * side + tan.x * life * 0.26F,
                    n.y * side + tan.y * life * 0.26F,
                    (deterministic(i, 17.4F) - 0.5F) * 0.42F - life * 0.10F);
            float size = 0.038F + deterministic(i, 18.5F) * 0.066F;
            float a = alpha * (1.0F - smootherStep(life)) * (0.22F + deterministic(i, 19.7F) * 0.34F);
            square(vc, mat, p.x, p.y, p.z + 0.075F, size, age * 0.08F + i * 0.77F, a);
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
        quad(vc, mat,
                x - hx - px, y - hy - py, z, 8.0F, 0.0F, alpha,
                x + hx - px, y + hy - py, z, 9.0F, 0.0F, alpha,
                x + hx + px, y + hy + py, z, 9.0F, 1.0F, alpha,
                x - hx + px, y - hy + py, z, 8.0F, 1.0F, alpha);
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

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.ZaizanEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/** Red-white blade burst inspired by Honkai-style finisher VFX. */
public final class ZaizanRenderer extends EntityRenderer<ZaizanEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");
    private static final int GROUND_SEGMENTS = 96;
    private static final int SHARD_COUNT = 30;
    private static final int STREAK_COUNT = 18;
    private static final int CHARGE_DOT_COUNT = 10;

    public ZaizanRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ZaizanEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float life = Math.max(1.0F, entity.getLifetime());
        float progress = Mth.clamp(age / life, 0.0F, 1.0F);
        float intro = smootherStep(Mth.clamp(age / 3.0F, 0.0F, 1.0F));
        float outro = 1.0F - smootherStep(Mth.clamp((progress - 0.78F) / 0.22F, 0.0F, 1.0F));
        float alpha = intro * outro;
        if (alpha <= 0.004F) {
            return;
        }

        float slashT = Mth.clamp((age - 5.0F) / 9.0F, 0.0F, 1.0F);
        float burst = smootherStep(slashT) * (1.0F - smootherStep(Mth.clamp((slashT - 0.74F) / 0.26F, 0.0F, 1.0F)));
        float flash = Mth.sin(Mth.clamp((age - 6.0F) / 7.0F, 0.0F, 1.0F) * Mth.PI);
        float charge = alpha * (1.0F - smootherStep(Mth.clamp((age - 8.0F) / 5.0F, 0.0F, 1.0F)));
        float after = alpha * smootherStep(Mth.clamp((age - 11.0F) / 5.0F, 0.0F, 1.0F))
                * (1.0F - smootherStep(Mth.clamp((progress - 0.78F) / 0.18F, 0.0F, 1.0F)));
        float scale = entity.getScale() * (1.34F + burst * 0.36F);

        poseStack.pushPose();
        Entity owner = entity.getOwner();
        if (owner != null) {
            double entX = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double entY = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double entZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            double ownX = Mth.lerp(partialTick, owner.xOld, owner.getX());
            double ownY = Mth.lerp(partialTick, owner.yOld, owner.getY());
            double ownZ = Mth.lerp(partialTick, owner.zOld, owner.getZ());
            float yawRad = entity.getYRot() * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yawRad);
            double fz = Mth.cos(yawRad);
            poseStack.translate(
                    ownX + fx * entity.getForwardOffset() - entX,
                    ownY + entity.getUpOffset() - entY,
                    ownZ + fz * entity.getForwardOffset() - entZ);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(0.0F, 0.0F, 0.55F);
        poseStack.scale(scale, scale, scale);
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.zaizan());

        preFlash(vc, mat, age, charge);
        impactEmbers(vc, mat, age, charge + after * 0.48F, flash);
        if (burst > 0.001F) {
            screenWash(vc, mat, burst, flash, alpha);
            mainBladeFlash(vc, mat, age, slashT, burst, flash, alpha);
            redFeatherSlashes(vc, mat, age, slashT, burst, alpha);
        }
        sparksAndShards(vc, mat, age, progress, alpha, burst + after * 0.55F, flash);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void preFlash(VertexConsumer vc, Matrix4f mat, float age, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        float pulse = 0.64F + 0.36F * Mth.sin(age * 0.72F);
        for (int i = 0; i < CHARGE_DOT_COUNT; i++) {
            float t = i / (float) (CHARGE_DOT_COUNT - 1);
            float side = deterministic(i, 4.8F) * 2.0F - 1.0F;
            float x = -0.44F + t * 0.96F + side * 0.06F;
            float y = -0.18F + deterministic(i, 8.3F) * 0.72F;
            float z = 0.78F + deterministic(i, 9.6F) * 0.42F;
            float size = 0.030F + deterministic(i, 11.1F) * 0.044F;
            diamond(vc, mat, x, y, z, size, age * 0.14F + i * 0.37F,
                    1.0F, 0.04F, 0.035F, alpha * pulse * (0.32F + deterministic(i, 12.7F) * 0.42F));
        }
        for (int i = 0; i < 7; i++) {
            float y = -0.28F + deterministic(i, 17.2F) * 1.04F;
            float z = 0.70F + deterministic(i, 19.7F) * 0.68F;
            float x = -1.04F + deterministic(i, 21.5F) * 2.10F;
            float len = 0.34F + deterministic(i, 23.1F) * 0.72F;
            slashRibbon(vc, mat, x - 0.18F, y - 0.014F, z,
                    x + len, y + 0.045F, z + 0.035F,
                    0.018F + deterministic(i, 24.9F) * 0.015F,
                    1.0F, 0.09F, 0.04F, alpha * (0.26F + deterministic(i, 26.2F) * 0.24F), 0.0F);
        }
    }

    private static void impactEmbers(VertexConsumer vc, Matrix4f mat, float age, float alpha, float flash) {
        if (alpha <= 0.003F) {
            return;
        }
        float pulse = 0.72F + flash * 0.52F;
        for (int i = 0; i < 9; i++) {
            float angle = i * Mth.TWO_PI / 9.0F + age * 0.035F;
            float radius = 0.36F + deterministic(i, 61.3F) * 0.58F + flash * 0.16F;
            float x = Mth.cos(angle) * radius;
            float y = -0.12F + Mth.sin(angle) * radius * 0.54F + deterministic(i, 62.9F) * 0.28F;
            float z = 0.88F + deterministic(i, 64.4F) * 0.54F;
            float size = 0.035F + deterministic(i, 65.8F) * 0.075F + flash * 0.025F;
            diamond(vc, mat, x, y, z, size, -age * 0.16F + i * 0.72F,
                    1.0F, 0.05F, 0.04F, alpha * pulse * (0.28F + deterministic(i, 66.7F) * 0.44F));
        }
    }

    private static void screenWash(VertexConsumer vc, Matrix4f mat, float burst, float flash, float alpha) {
        float a = alpha * burst * (0.42F + flash * 0.42F);
        smokeRibbon(vc, mat, -12.0F, 2.72F, 1.72F, 12.5F, 3.00F, 1.42F,
                2.65F, 1.0F, 0.10F, 0.08F, a, 8.0F);
        smokeRibbon(vc, mat, -11.4F, 1.88F, 1.32F, 11.8F, 2.36F, 1.04F,
                1.75F, 1.0F, 0.05F, 0.045F, a * 0.76F, 8.0F);
        smokeRibbon(vc, mat, -10.2F, 0.74F, 0.92F, 10.8F, 1.40F, 0.76F,
                1.10F, 1.0F, 0.035F, 0.035F, a * 0.48F, 8.0F);
    }

    private static void mainBladeFlash(VertexConsumer vc, Matrix4f mat, float age, float slashT,
                                       float burst, float flash, float alpha) {
        float travel = smootherStep(slashT);
        float xShift = -1.70F + travel * 2.95F;
        float yLift = 0.12F + flash * 0.30F;
        float z = 1.06F + flash * 0.10F;
        float a = alpha * burst;

        slashRibbon(vc, mat, -12.6F + xShift, -0.38F + yLift, z,
                13.2F + xShift, 1.26F + yLift, z + 0.02F,
                2.28F + flash * 0.70F, 1.0F, 0.035F, 0.028F, a * (0.72F + flash * 0.44F), 2.0F);
        slashRibbon(vc, mat, -11.9F + xShift, -0.23F + yLift, z + 0.03F,
                12.8F + xShift, 1.12F + yLift, z + 0.05F,
                1.24F + flash * 0.36F, 1.0F, 0.26F, 0.16F, a * (0.92F + flash * 0.54F), 2.0F);
        slashRibbon(vc, mat, -11.0F + xShift, -0.10F + yLift, z + 0.06F,
                12.0F + xShift, 0.99F + yLift, z + 0.08F,
                0.52F + flash * 0.20F, 1.0F, 0.86F, 0.72F, a * (1.18F + flash * 0.86F), 2.0F);
        slashRibbon(vc, mat, -10.4F + xShift, 0.03F + yLift, z + 0.10F,
                11.3F + xShift, 0.86F + yLift, z + 0.12F,
                0.18F + flash * 0.08F, 1.0F, 0.98F, 0.92F, a * (1.36F + flash * 1.08F), 2.0F);

        for (int i = 0; i < STREAK_COUNT; i++) {
            float lane = deterministic(i, 30.1F) * 2.0F - 1.0F;
            float x0 = -7.6F + deterministic(i, 31.4F) * 14.0F + travel * 2.2F;
            float len = 0.8F + deterministic(i, 33.2F) * 2.8F + flash * 1.0F;
            float y = -0.74F + deterministic(i, 34.8F) * 2.18F + lane * 0.12F;
            float zz = 0.72F + deterministic(i, 36.1F) * 1.00F;
            slashRibbon(vc, mat, x0, y, zz, x0 + len, y + 0.09F + lane * 0.04F, zz + 0.03F,
                    0.022F + deterministic(i, 38.9F) * 0.024F,
                    1.0F, 0.10F, 0.045F, a * (0.24F + deterministic(i, 40.5F) * 0.30F), 0.0F);
        }
    }

    private static void redFeatherSlashes(VertexConsumer vc, Matrix4f mat, float age, float slashT,
                                          float burst, float alpha) {
        float travel = smootherStep(slashT);
        float a = alpha * burst;
        for (int i = 0; i < 7; i++) {
            float f = i / 6.0F;
            float y = -0.86F + f * 0.98F;
            float z = 0.78F + f * 0.18F;
            float x0 = -8.8F + f * 1.1F + travel * 2.0F;
            float x1 = 8.0F + f * 2.2F + travel * 2.5F;
            float lift = Mth.sin(f * Mth.PI) * 0.84F;
            slashRibbon(vc, mat, x0, y - lift * 0.14F, z,
                    x1, y + 0.42F + lift, z + 0.10F,
                    0.22F + f * 0.18F, 1.0F, 0.025F, 0.020F,
                    a * (0.32F + (1.0F - f) * 0.22F), 2.0F);
        }
    }

    private static void groundClaws(VertexConsumer vc, Matrix4f mat, float age, float burst, float alpha) {
        float presence = alpha * Mth.clamp((age - 4.0F) / 7.0F, 0.0F, 1.0F)
                * (1.0F - smootherStep(Mth.clamp((age - 24.0F) / 8.0F, 0.0F, 1.0F)));
        if (presence <= 0.002F) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            float radius = 1.55F + i * 0.55F + burst * 0.62F;
            float start = -168.0F + i * 10.0F - age * 2.2F;
            float span = 110.0F + i * 14.0F;
            float width = 0.070F + i * 0.020F + burst * 0.075F;
            groundArc(vc, mat, -1.06F + i * 0.006F, radius, width, start, span,
                    1.0F, 0.025F, 0.02F, presence * (0.34F + burst * 0.36F), 4.0F);
        }
        for (int i = 0; i < 5; i++) {
            float x0 = -4.2F + i * 1.05F;
            float x1 = 2.8F + i * 1.50F;
            float z0 = -1.9F + i * 0.44F;
            float z1 = 1.45F + i * 0.34F;
            groundSlash(vc, mat, x0, z0, x1, z1, 0.09F + i * 0.018F,
                    1.0F, 0.04F, 0.03F, presence * (0.24F + burst * 0.32F));
        }
    }

    private static void sparksAndShards(VertexConsumer vc, Matrix4f mat, float age, float progress,
                                        float alpha, float burst, float flash) {
        float presence = alpha * Mth.clamp((age - 7.0F) / 5.0F, 0.0F, 1.0F)
                * (1.0F - smootherStep(Mth.clamp((progress - 0.72F) / 0.20F, 0.0F, 1.0F)));
        if (presence <= 0.001F) {
            return;
        }
        for (int i = 0; i < SHARD_COUNT; i++) {
            float side = deterministic(i, 47.1F) * 2.0F - 1.0F;
            float drift = Mth.frac(age * (0.026F + deterministic(i, 48.6F) * 0.032F)
                    + deterministic(i, 49.8F));
            float x = -3.2F + deterministic(i, 51.3F) * 7.2F + drift * (1.1F + burst * 1.5F);
            float y = -0.40F + deterministic(i, 52.9F) * 2.05F + drift * 0.40F;
            float z = 0.50F + deterministic(i, 54.4F) * 1.55F + side * 0.28F;
            float size = 0.034F + deterministic(i, 55.6F) * 0.088F + flash * 0.022F;
            float a = presence * (1.0F - smootherStep(drift))
                    * (0.22F + deterministic(i, 57.2F) * 0.40F + burst * 0.28F);
            diamond(vc, mat, x, y, z, size, age * (0.12F + deterministic(i, 58.9F) * 0.20F) + i,
                    1.0F, 0.04F, 0.04F, a);
            if ((i & 7) == 0) {
                slashRibbon(vc, mat, x - size * 4.0F, y - size * 0.12F, z,
                        x + size * 8.0F, y + size * 0.32F, z,
                        size * 0.40F, 1.0F, 0.10F, 0.05F, a * 0.42F, 0.0F);
            }
        }
    }

    private static void groundArc(VertexConsumer vc, Matrix4f mat, float y, float radius, float width,
                                  float startDeg, float spanDeg,
                                  float r, float g, float b, float alpha, float uBase) {
        int segments = Math.max(1, Mth.ceil(GROUND_SEGMENTS * spanDeg / 360.0F));
        for (int i = 0; i < segments; i++) {
            float f0 = i / (float) segments;
            float f1 = (i + 1) / (float) segments;
            float taper0 = smoothBladeTaper(f0);
            float taper1 = smoothBladeTaper(f1);
            float a0 = (float) Math.toRadians(startDeg + spanDeg * f0);
            float a1 = (float) Math.toRadians(startDeg + spanDeg * f1);
            float inner0 = radius - width * taper0;
            float outer0 = radius + width * (1.8F + taper0);
            float inner1 = radius - width * taper1;
            float outer1 = radius + width * (1.8F + taper1);
            quad(vc, mat,
                    Mth.cos(a0) * outer0, y, Mth.sin(a0) * outer0, uBase + f0, 0.0F,
                    Mth.cos(a1) * outer1, y, Mth.sin(a1) * outer1, uBase + f1, 0.0F,
                    Mth.cos(a1) * inner1, y, Mth.sin(a1) * inner1, uBase + f1, 1.0F,
                    Mth.cos(a0) * inner0, y, Mth.sin(a0) * inner0, uBase + f0, 1.0F,
                    r, g, b, alpha * Math.max(taper0, taper1));
        }
    }

    private static void groundSlash(VertexConsumer vc, Matrix4f mat, float x0, float z0, float x1, float z1,
                                    float width, float r, float g, float b, float alpha) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dz * dz);
        if (len <= 0.0001F) {
            return;
        }
        float px = -dz / len * width;
        float pz = dx / len * width;
        quad(vc, mat,
                x0 + px * 2.2F, -1.045F, z0 + pz * 2.2F, 4.0F, 0.0F,
                x1 + px * 0.35F, -1.045F, z1 + pz * 0.35F, 5.0F, 0.0F,
                x1 - px * 0.22F, -1.045F, z1 - pz * 0.22F, 5.0F, 1.0F,
                x0 - px * 1.2F, -1.045F, z0 - pz * 1.2F, 4.0F, 1.0F,
                r, g, b, alpha);
    }

    private static void slashRibbon(VertexConsumer vc, Matrix4f mat,
                                    float x0, float y0, float z0, float x1, float y1, float z1,
                                    float width, float r, float g, float b, float alpha, float uBase) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001F) {
            return;
        }
        float px = -dy / len * width * 0.5F;
        float py = dx / len * width * 0.5F;
        quad(vc, mat,
                x0 + px, y0 + py, z0, uBase, 0.0F,
                x1 + px, y1 + py, z1, uBase + 1.0F, 0.0F,
                x1 - px, y1 - py, z1, uBase + 1.0F, 1.0F,
                x0 - px, y0 - py, z0, uBase, 1.0F,
                r, g, b, alpha);
    }

    private static void smokeRibbon(VertexConsumer vc, Matrix4f mat,
                                    float x0, float y0, float z0, float x1, float y1, float z1,
                                    float width, float r, float g, float b, float alpha, float uBase) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001F) {
            return;
        }
        float px = -dy / len * width * 0.5F;
        float py = dx / len * width * 0.5F;
        float waist = 0.54F;
        float midX = (x0 + x1) * 0.5F;
        float midY = (y0 + y1) * 0.5F + width * 0.08F;
        float midZ = (z0 + z1) * 0.5F;
        quad(vc, mat,
                x0 + px * 0.18F, y0 + py * 0.18F, z0, uBase, 0.0F,
                midX + px, midY + py, midZ, uBase + 0.52F, 0.0F,
                midX - px * waist, midY - py * waist, midZ, uBase + 0.52F, 1.0F,
                x0 - px * 0.12F, y0 - py * 0.12F, z0, uBase, 1.0F,
                r, g, b, alpha * 0.78F);
        quad(vc, mat,
                midX + px, midY + py, midZ, uBase + 0.52F, 0.0F,
                x1 + px * 0.16F, y1 + py * 0.16F, z1, uBase + 1.0F, 0.0F,
                x1 - px * 0.10F, y1 - py * 0.10F, z1, uBase + 1.0F, 1.0F,
                midX - px * waist, midY - py * waist, midZ, uBase + 0.52F, 1.0F,
                r, g, b, alpha);
    }

    private static void diamond(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                                float size, float rotation,
                                float r, float g, float b, float alpha) {
        float c = Mth.cos(rotation);
        float s = Mth.sin(rotation);
        float ax = c * size * 1.35F;
        float ay = s * size * 1.35F;
        float bx = -s * size;
        float by = c * size;
        quad(vc, mat,
                x - ax, y - ay, z, 6.0F, 0.5F,
                x + bx, y + by, z, 6.5F, 0.0F,
                x + ax, y + ay, z, 7.0F, 0.5F,
                x - bx, y - by, z, 6.5F, 1.0F,
                r, g, b, alpha);
    }

    private static float smoothBladeTaper(float t) {
        return smootherStep(Mth.clamp(t / 0.18F, 0.0F, 1.0F))
                * (1.0F - smootherStep(Mth.clamp((t - 0.76F) / 0.24F, 0.0F, 1.0F)));
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float deterministic(int index, float salt) {
        return Mth.frac(Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float r, float g, float b, float alpha) {
        vertex(vc, mat, x0, y0, z0, u0, v0, r, g, b, alpha);
        vertex(vc, mat, x1, y1, z1, u1, v1, r, g, b, alpha);
        vertex(vc, mat, x2, y2, z2, u2, v2, r, g, b, alpha);
        vertex(vc, mat, x3, y3, z3, u3, v3, r, g, b, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float y, float z, float u, float v,
                               float r, float g, float b, float alpha) {
        vc.vertex(mat, x, y, z).color(r, g, b, alpha).uv(u, v).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ZaizanEntity entity) {
        return EMPTY_TEXTURE;
    }
}

package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.ValkyrieImpactEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 5-Layer cinematic multi-row effect renderer for Valkyrie Impact (女武神冲击):
 * 1. Ground Impact Burst: Instant expanding white-gold flash sphere, ground shockwave ring & 24 multi-tiered radial energy spikes
 * 2. 5-Lane Spreading Ground Fissure: Multi-row fan-shaped propagating rock slabs & vertical rising jagged golden energy blade crests
 * 3. Earthen Debris Particles & Sparks: 240 ballistic gravity particles bursting across the entire wide fan area
 * 4. Ground Dust Puffs: Soft rolling dust and shock clouds
 * 5. Multi-Lane Ground Scorch Decal: Persistent glowing fracture scorch marks along all 5 fissure trails
 */
public class ValkyrieImpactRenderer extends EntityRenderer<ValkyrieImpactEntity> {

    public static final ResourceLocation DEEPSLATE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate.png");

    private static final int DEBRIS_COUNT = 240;

    private record Lane(float angleDeg, float offsetX, float length, float heightScale, float widthScale, float delayOffset, int segments) {}

    private static final Lane[] LANES = new Lane[] {
            new Lane(0.0F, 0.0F, 7.25F, 1.20F, 1.10F, 0.00F, 16),      // Center Main Lane
            new Lane(-14.0F, -0.45F, 6.40F, 0.95F, 0.95F, 0.025F, 14), // Inner Left Lane
            new Lane(14.0F, 0.45F, 6.40F, 0.95F, 0.95F, 0.025F, 14),   // Inner Right Lane
            new Lane(-28.0F, -0.80F, 5.20F, 0.75F, 0.85F, 0.050F, 12), // Outer Left Lane
            new Lane(28.0F, 0.80F, 5.20F, 0.75F, 0.85F, 0.050F, 12)    // Outer Right Lane
    };

    public ValkyrieImpactRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(ValkyrieImpactEntity entity) {
        return DEEPSLATE_TEXTURE;
    }

    @Override
    public void render(ValkyrieImpactEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (ValkyrieImpactOculusPipeline.enqueue(entity, partialTicks)) {
            return;
        }

        float timeSeconds = (entity.getLifetime() + partialTicks) / 30.0F;
        if (timeSeconds < 0.0F || timeSeconds > 2.24F) {
            return;
        }

        poseStack.pushPose();
        // Rotate pose to match caster horizontal orientation (Z+ is forward fissure direction)
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYawRot()));

        renderLayers(poseStack, buffer, KabladeRenderTypes.nuclearShockDome(), packedLight, timeSeconds);

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    public static void renderLayers(PoseStack poseStack, MultiBufferSource buffer, RenderType additiveType,
                                    int packedLight, float timeSeconds) {
        if (timeSeconds < 0.56F) {
            return;
        }
        // getBuffer may reuse the same builder and change its vertex format. Finish
        // all solid vertices before acquiring the additive consumer; never retain both.
        renderMultiLaneFissure(poseStack, buffer.getBuffer(RenderType.entityCutout(DEEPSLATE_TEXTURE)),
                packedLight, timeSeconds, true);
        renderAdditiveLayers(poseStack, buffer.getBuffer(additiveType), packedLight, timeSeconds);
    }

    /** For independent mesh collectors only; shared buffer sources must use the overload above. */
    public static void renderLayers(PoseStack poseStack, VertexConsumer additiveConsumer, VertexConsumer solidConsumer,
                                    int packedLight, float timeSeconds) {
        if (timeSeconds >= 0.56F) {
            renderMultiLaneFissure(poseStack, solidConsumer, packedLight, timeSeconds, true);
        }
        renderAdditiveLayers(poseStack, additiveConsumer, packedLight, timeSeconds);
    }

    private static void renderAdditiveLayers(PoseStack poseStack, VertexConsumer additiveConsumer,
                                             int packedLight, float timeSeconds) {
        // 1. Ground Impact Burst (Frame 17 ~ 30, 0.56s ~ 1.00s)
        if (timeSeconds >= 0.56F && timeSeconds < 1.00F) {
            renderImpactBurst(poseStack, additiveConsumer, timeSeconds - 0.56F);
        }

        // 2. Multi-Lane Ground Fissure Slabs & Vertical Jagged Energy Blades (0.56s ~ 2.24s)
        if (timeSeconds >= 0.56F) {
            renderMultiLaneFissure(poseStack, additiveConsumer, packedLight, timeSeconds, false);
        }

        // 3. Earthen Debris Particles & Sparks across the wide fan (0.58s ~ 2.20s)
        if (timeSeconds >= 0.58F && timeSeconds < 2.20F) {
            renderDebrisParticles(poseStack, additiveConsumer, timeSeconds - 0.58F);
        }

        // 4. Multi-Lane Ground Scorch Trails (0.56s ~ 2.24s)
        if (timeSeconds >= 0.56F) {
            renderMultiLaneScorchDecals(poseStack, additiveConsumer, timeSeconds - 0.56F);
        }
    }

    /* -------------------------------------------------------------
       1. Ground Impact Burst (Flash Sphere, Wide Shock Ring & 24 Spikes)
       ------------------------------------------------------------- */
    private static void renderImpactBurst(PoseStack ps, VertexConsumer consumer, float t) {
        Matrix4f mat = ps.last().pose();
        float burstAge = t / 0.42F;
        float alpha = Math.max(0.0F, 1.0F - burstAge);

        // A. Flash Sphere (Radius 0.3 -> 3.2m)
        float flashRadius = 0.3F + burstAge * 2.9F;
        int rings = 8;
        int segments = 16;
        for (int i = 0; i < rings; i++) {
            float phi1 = ((float) i / rings) * (Mth.PI / 2.0F);
            float phi2 = ((float) (i + 1) / rings) * (Mth.PI / 2.0F);

            for (int j = 0; j < segments; j++) {
                float theta1 = ((float) j / segments) * Mth.TWO_PI;
                float theta2 = ((float) (j + 1) / segments) * Mth.TWO_PI;

                putDomeVertex(consumer, mat, flashRadius, phi1, theta1, alpha * 0.9F, 1.0F, 0.98F, 0.85F);
                putDomeVertex(consumer, mat, flashRadius, phi1, theta2, alpha * 0.9F, 1.0F, 0.98F, 0.85F);
                putDomeVertex(consumer, mat, flashRadius, phi2, theta2, alpha * 0.9F, 1.0F, 0.85F, 0.40F);
                putDomeVertex(consumer, mat, flashRadius, phi2, theta1, alpha * 0.9F, 1.0F, 0.85F, 0.40F);
            }
        }

        // B. Expanding Ground Shockwave Disc (Radius 0.4 -> 5.5m)
        float ringInner = 0.15F + burstAge * 3.2F;
        float ringOuter = 0.45F + burstAge * 5.5F;
        int ringSegs = 32;
        for (int j = 0; j < ringSegs; j++) {
            float th1 = ((float) j / ringSegs) * Mth.TWO_PI;
            float th2 = ((float) (j + 1) / ringSegs) * Mth.TWO_PI;

            float cos1 = Mth.cos(th1);
            float sin1 = Mth.sin(th1);
            float cos2 = Mth.cos(th2);
            float sin2 = Mth.sin(th2);

            consumer.vertex(mat, cos1 * ringInner, 0.02F, sin1 * ringInner).color(1.0F, 0.95F, 0.6F, alpha * 0.85F).uv(0, 0).endVertex();
            consumer.vertex(mat, cos2 * ringInner, 0.02F, sin2 * ringInner).color(1.0F, 0.95F, 0.6F, alpha * 0.85F).uv(1, 0).endVertex();
            consumer.vertex(mat, cos2 * ringOuter, 0.02F, sin2 * ringOuter).color(0.95F, 0.60F, 0.1F, 0.0F).uv(1, 1).endVertex();
            consumer.vertex(mat, cos1 * ringOuter, 0.02F, sin1 * ringOuter).color(0.95F, 0.60F, 0.1F, 0.0F).uv(0, 1).endVertex();
        }

        // C. 24 Radial Piercing Golden Energy Spikes (Inner & Outer Staggered)
        int spikeCount = 24;
        for (int s = 0; s < spikeCount; s++) {
            float angle = ((float) s / spikeCount) * Mth.TWO_PI;
            float cosA = Mth.cos(angle);
            float sinA = Mth.sin(angle);
            boolean isLong = (s % 2 == 0);
            float spikeLen = isLong ? (0.5F + burstAge * 2.8F) : (0.8F + burstAge * 4.2F);
            float width = isLong ? 0.09F : 0.06F;

            float cosPerp = -sinA * width;
            float sinPerp = cosA * width;

            float tipX = cosA * spikeLen;
            float tipY = (isLong ? 0.12F : 0.08F) + burstAge * 0.45F;
            float tipZ = sinA * spikeLen;

            consumer.vertex(mat, -cosPerp, 0.05F, -sinPerp).color(1.0F, 0.8F, 0.2F, alpha * 0.85F).uv(0, 0).endVertex();
            consumer.vertex(mat, cosPerp, 0.05F, sinPerp).color(1.0F, 0.8F, 0.2F, alpha * 0.85F).uv(1, 0).endVertex();
            consumer.vertex(mat, tipX, tipY, tipZ).color(1.0F, 0.98F, 0.9F, 0.0F).uv(0.5F, 1).endVertex();
            consumer.vertex(mat, tipX, tipY, tipZ).color(1.0F, 0.98F, 0.9F, 0.0F).uv(0.5F, 1).endVertex();
        }
    }

    /* -------------------------------------------------------------
       2. 5-Lane Spreading Ground Fissure (Rock Slabs + Jagged Energy Blades)
       ------------------------------------------------------------- */
    private static void renderMultiLaneFissure(PoseStack ps, VertexConsumer consumer, int light, float sec, boolean solidPass) {
        Matrix4f mat = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        for (Lane lane : LANES) {
            float angleRad = lane.angleDeg * Mth.DEG_TO_RAD;
            float cosA = Mth.cos(angleRad);
            float sinA = Mth.sin(angleRad);

            // Forward vector along lane & Perpendicular vector across lane
            float fX = sinA;
            float fZ = cosA;
            float pX = cosA;
            float pZ = -sinA;

            for (int i = 0; i < lane.segments; i++) {
                float progress = (float) i / (float) (lane.segments - 1);
                float dist = progress * lane.length;
                float posX = lane.offsetX + fX * dist;
                float posZ = fZ * dist;

                float delay = 0.56F + lane.delayOffset + progress * 0.36F;
                if (sec < delay) {
                    continue;
                }

                float segAge = sec - delay;
                float eruptP;
                float alpha;

                float DURATION_ERUPT = 0.12F;
                float DURATION_HOLD = 0.55F;
                float DURATION_FADE = 0.70F;

                if (segAge < DURATION_ERUPT) {
                    eruptP = segAge / DURATION_ERUPT;
                    eruptP = 1.0F - (1.0F - eruptP) * (1.0F - eruptP);
                    alpha = eruptP * 0.95F;
                } else if (segAge < DURATION_ERUPT + DURATION_HOLD) {
                    eruptP = 1.0F;
                    float pulse = 0.85F + Mth.sin((segAge + lane.delayOffset) * 18.0F) * 0.15F;
                    alpha = pulse;
                } else if (segAge < DURATION_ERUPT + DURATION_HOLD + DURATION_FADE) {
                    float fadeP = (segAge - (DURATION_ERUPT + DURATION_HOLD)) / DURATION_FADE;
                    eruptP = 1.0F - fadeP * 0.5F;
                    alpha = Math.max(0.0F, (1.0F - fadeP) * 0.85F);
                } else {
                    continue;
                }

                if (solidPass) {
                    // A. Left and Right Tilted Rock Slabs along the lane heading
                    float slabOffset = 0.20F * lane.widthScale;
                    float slabW = 0.22F * lane.widthScale;
                    float slabL = 0.32F * lane.widthScale;
                    float slabH = 0.16F * lane.heightScale * eruptP;

                    renderOrientedRockSlab(consumer, mat, normal, light,
                            posX - pX * slabOffset, posZ - pZ * slabOffset,
                            slabW, slabL, slabH, 0.38F, fX, fZ, pX, pZ);
                    renderOrientedRockSlab(consumer, mat, normal, light,
                            posX + pX * slabOffset, posZ + pZ * slabOffset,
                            slabW, slabL, slabH, -0.38F, fX, fZ, pX, pZ);
                    continue;
                }

                // B. Vertical Jagged Golden Energy Blade Crest
                VertexConsumer additiveConsumer = consumer;
                float peakH = (0.55F + Mth.sin(progress * Mth.PI) * 0.65F) * lane.heightScale * eruptP;
                float halfW = 0.14F * lane.widthScale;
                float halfL = 0.18F * lane.widthScale;

                float nX = posX - fX * halfL;
                float nZ = posZ - fZ * halfL;
                float sX = posX + fX * halfL;
                float sZ = posZ + fZ * halfL;
                float wX = posX - pX * halfW;
                float wZ = posZ - pZ * halfW;
                float eX = posX + pX * halfW;
                float eZ = posZ + pZ * halfW;

                float topX = posX + fX * 0.04F;
                float topZ = posZ + fZ * 0.04F;

                // 4-sided pyramid / jagged diamond blade
                // North face
                additiveConsumer.vertex(mat, wX, 0.02F, wZ).color(1.0F, 0.65F, 0.1F, alpha * 0.85F).uv(0, 0).endVertex();
                additiveConsumer.vertex(mat, eX, 0.02F, eZ).color(1.0F, 0.65F, 0.1F, alpha * 0.85F).uv(1, 0).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();

                // South face
                additiveConsumer.vertex(mat, eX, 0.02F, eZ).color(1.0F, 0.65F, 0.1F, alpha * 0.85F).uv(1, 0).endVertex();
                additiveConsumer.vertex(mat, wX, 0.02F, wZ).color(1.0F, 0.65F, 0.1F, alpha * 0.85F).uv(0, 0).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();

                // East face
                additiveConsumer.vertex(mat, eX, 0.02F, eZ).color(1.0F, 0.55F, 0.05F, alpha * 0.85F).uv(0, 0).endVertex();
                additiveConsumer.vertex(mat, sX, 0.02F, sZ).color(1.0F, 0.55F, 0.05F, alpha * 0.85F).uv(1, 0).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();

                // West face
                additiveConsumer.vertex(mat, wX, 0.02F, wZ).color(1.0F, 0.55F, 0.05F, alpha * 0.85F).uv(1, 0).endVertex();
                additiveConsumer.vertex(mat, nX, 0.02F, nZ).color(1.0F, 0.55F, 0.05F, alpha * 0.85F).uv(0, 0).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();
                additiveConsumer.vertex(mat, topX, peakH, topZ).color(1.0F, 0.98F, 0.85F, alpha).uv(0.5F, 1).endVertex();
            }
        }
    }

    private static void putRockVertex(VertexConsumer vc, Matrix4f mat, Matrix3f normal,
                                      float x, float y, float z, float u, float v,
                                      float r, float g, float b, float a, int light,
                                      float nx, float ny, float nz) {
        vc.vertex(mat, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }

    private static void renderOrientedRockSlab(VertexConsumer vc, Matrix4f mat, Matrix3f normal, int light,
                                               float cX, float cZ, float w, float l, float h, float tilt,
                                               float fX, float fZ, float pX, float pZ) {
        if (h <= 0.005F) return;
        float hw = w * 0.5F;
        float hl = l * 0.5F;
        float cosT = Mth.cos(tilt);
        float sinT = Mth.sin(tilt);

        // Displaced left/right bounds along perpendicular axis
        float x1 = cX - pX * hw * cosT;
        float z1 = cZ - pZ * hw * cosT;
        float y1 = sinT * hw;

        float x2 = cX + pX * hw * cosT;
        float z2 = cZ + pZ * hw * cosT;
        float y2 = -sinT * hw;

        float frontX1 = x1 - fX * hl; float frontZ1 = z1 - fZ * hl;
        float frontX2 = x2 - fX * hl; float frontZ2 = z2 - fZ * hl;
        float backX1  = x1 + fX * hl; float backZ1  = z1 + fZ * hl;
        float backX2  = x2 + fX * hl; float backZ2  = z2 + fZ * hl;

        // Top face
        putRockVertex(vc, mat, normal, frontX1, y1 + h, frontZ1, 0, 0, 0.55F, 0.58F, 0.62F, 1.0F, light, 0, 1, 0);
        putRockVertex(vc, mat, normal, frontX2, y2 + h, frontZ2, 1, 0, 0.55F, 0.58F, 0.62F, 1.0F, light, 0, 1, 0);
        putRockVertex(vc, mat, normal, backX2,  y2 + h, backZ2,  1, 1, 0.55F, 0.58F, 0.62F, 1.0F, light, 0, 1, 0);
        putRockVertex(vc, mat, normal, backX1,  y1 + h, backZ1,  0, 1, 0.55F, 0.58F, 0.62F, 1.0F, light, 0, 1, 0);

        // Left face
        putRockVertex(vc, mat, normal, backX1,  0.0F,   backZ1,  0, 1, 0.35F, 0.38F, 0.42F, 1.0F, light, -pX, sinT, -pZ);
        putRockVertex(vc, mat, normal, frontX1, 0.0F,   frontZ1, 1, 1, 0.35F, 0.38F, 0.42F, 1.0F, light, -pX, sinT, -pZ);
        putRockVertex(vc, mat, normal, frontX1, y1 + h, frontZ1, 1, 0, 0.35F, 0.38F, 0.42F, 1.0F, light, -pX, sinT, -pZ);
        putRockVertex(vc, mat, normal, backX1,  y1 + h, backZ1,  0, 0, 0.35F, 0.38F, 0.42F, 1.0F, light, -pX, sinT, -pZ);

        // Right face
        putRockVertex(vc, mat, normal, frontX2, y2 + h, frontZ2, 0, 0, 0.35F, 0.38F, 0.42F, 1.0F, light, pX, -sinT, pZ);
        putRockVertex(vc, mat, normal, frontX2, 0.0F,   frontZ2, 0, 1, 0.35F, 0.38F, 0.42F, 1.0F, light, pX, -sinT, pZ);
        putRockVertex(vc, mat, normal, backX2,  0.0F,   backZ2,  1, 1, 0.35F, 0.38F, 0.42F, 1.0F, light, pX, -sinT, pZ);
        putRockVertex(vc, mat, normal, backX2,  y2 + h, backZ2,  1, 0, 0.35F, 0.38F, 0.42F, 1.0F, light, pX, -sinT, pZ);

        // Front face
        putRockVertex(vc, mat, normal, frontX1, 0.0F,   frontZ1, 0, 1, 0.45F, 0.48F, 0.52F, 1.0F, light, -fX, 0, -fZ);
        putRockVertex(vc, mat, normal, frontX2, 0.0F,   frontZ2, 1, 1, 0.45F, 0.48F, 0.52F, 1.0F, light, -fX, 0, -fZ);
        putRockVertex(vc, mat, normal, frontX2, y2 + h, frontZ2, 1, 0, 0.45F, 0.48F, 0.52F, 1.0F, light, -fX, 0, -fZ);
        putRockVertex(vc, mat, normal, frontX1, y1 + h, frontZ1, 0, 0, 0.45F, 0.48F, 0.52F, 1.0F, light, -fX, 0, -fZ);

        // Back face
        putRockVertex(vc, mat, normal, backX2,  0.0F,   backZ2,  0, 1, 0.45F, 0.48F, 0.52F, 1.0F, light, fX, 0, fZ);
        putRockVertex(vc, mat, normal, backX1,  0.0F,   backZ1,  1, 1, 0.45F, 0.48F, 0.52F, 1.0F, light, fX, 0, fZ);
        putRockVertex(vc, mat, normal, backX1,  y1 + h, backZ1,  1, 0, 0.45F, 0.48F, 0.52F, 1.0F, light, fX, 0, fZ);
        putRockVertex(vc, mat, normal, backX2,  y2 + h, backZ2,  0, 0, 0.45F, 0.48F, 0.52F, 1.0F, light, fX, 0, fZ);
    }

    /* -------------------------------------------------------------
       3. Earthen Debris Particles & Golden Sparks across Fan Area
       ------------------------------------------------------------- */
    private static void renderDebrisParticles(PoseStack ps, VertexConsumer consumer, float pAge) {
        Matrix4f mat = ps.last().pose();
        float gravity = -7.5F;
        float alpha = Math.max(0.0F, 1.0F - pAge / 1.38F);

        for (int i = 0; i < DEBRIS_COUNT; i++) {
            // Deterministic pseudo-random seed per particle
            float seed = i * 13.371F;
            float dist0 = (Mth.sin(seed * 2.1F) * 0.5F + 0.5F) * 7.25F;
            float fanAngle = (Mth.sin(seed * 5.7F)) * 0.55F; // +/- 31 degrees
            float x0 = Mth.sin(fanAngle) * dist0 + (Mth.cos(seed * 3.3F) * 0.4F);
            float z0 = Mth.cos(fanAngle) * dist0;
            float y0 = 0.05F;

            float vx = Mth.cos(seed * 4.3F) * 2.4F;
            float vy = 2.2F + (Mth.sin(seed * 8.9F) * 0.5F + 0.5F) * 4.2F;
            float vz = (Mth.sin(seed * 3.1F) * 0.5F + 0.5F) * 2.2F;

            float x = x0 + vx * pAge;
            float y = Math.max(0.02F, y0 + vy * pAge + 0.5F * gravity * pAge * pAge);
            float z = z0 + vz * pAge * 0.6F;

            float size = (i % 3 == 0) ? 0.065F : 0.040F;
            boolean isGlow = (i % 2 == 0);

            float r = isGlow ? 1.0F : 0.85F;
            float g = isGlow ? 0.88F : 0.55F;
            float b = isGlow ? 0.35F : 0.15F;

            consumer.vertex(mat, x - size, y - size, z).color(r, g, b, alpha * 0.9F).uv(0, 0).endVertex();
            consumer.vertex(mat, x + size, y - size, z).color(r, g, b, alpha * 0.9F).uv(1, 0).endVertex();
            consumer.vertex(mat, x + size, y + size, z).color(r, g, b, alpha * 0.9F).uv(1, 1).endVertex();
            consumer.vertex(mat, x - size, y + size, z).color(r, g, b, alpha * 0.9F).uv(0, 1).endVertex();
        }
    }

    /* -------------------------------------------------------------
       4. Multi-Lane Ground Scorch Decal Trails
       ------------------------------------------------------------- */
    private static void renderMultiLaneScorchDecals(PoseStack ps, VertexConsumer consumer, float scorchAge) {
        Matrix4f mat = ps.last().pose();
        float alpha = scorchAge < 0.35F
                ? (scorchAge / 0.35F) * 0.75F
                : Math.max(0.0F, (1.0F - (scorchAge - 0.35F) / 1.33F) * 0.75F);

        if (alpha <= 0.005F) return;

        for (Lane lane : LANES) {
            float angleRad = lane.angleDeg * Mth.DEG_TO_RAD;
            float fX = Mth.sin(angleRad);
            float fZ = Mth.cos(angleRad);
            float pX = Mth.cos(angleRad);
            float pZ = -Mth.sin(angleRad);

            float halfW = 0.28F * lane.widthScale;
            float startX = lane.offsetX;
            float startZ = 0.0F;
            float endX = lane.offsetX + fX * lane.length;
            float endZ = fZ * lane.length;

            float x1 = startX - pX * halfW; float z1 = startZ - pZ * halfW;
            float x2 = startX + pX * halfW; float z2 = startZ + pZ * halfW;
            float x3 = endX + pX * halfW;   float z3 = endZ + pZ * halfW;
            float x4 = endX - pX * halfW;   float z4 = endZ - pZ * halfW;

            consumer.vertex(mat, x1, 0.006F, z1).color(0.95F, 0.60F, 0.15F, alpha * 0.7F).uv(0, 0).endVertex();
            consumer.vertex(mat, x2, 0.006F, z2).color(0.95F, 0.60F, 0.15F, alpha * 0.7F).uv(1, 0).endVertex();
            consumer.vertex(mat, x3, 0.006F, z3).color(0.95F, 0.60F, 0.15F, alpha * 0.7F).uv(1, 1).endVertex();
            consumer.vertex(mat, x4, 0.006F, z4).color(0.95F, 0.60F, 0.15F, alpha * 0.7F).uv(0, 1).endVertex();
        }
    }

    private static void putDomeVertex(VertexConsumer consumer, Matrix4f mat, float r, float phi, float theta,
                                      float alpha, float rCol, float gCol, float bCol) {
        float x = r * Mth.cos(phi) * Mth.cos(theta);
        float y = r * Mth.sin(phi) + 0.05F;
        float z = r * Mth.cos(phi) * Mth.sin(theta);
        consumer.vertex(mat, x, y, z)
                .color(rCol, gCol, bCol, alpha)
                .uv(theta / Mth.TWO_PI, phi / (Mth.PI / 2.0F))
                .endVertex();
    }
}

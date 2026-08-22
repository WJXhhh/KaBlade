package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.NuclearShockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 5-layer effect renderer for Nuclear Shock (核能震动).
 * Renders an intense, high-impact nuclear detonation sequence with:
 * 1. Ground-zero plasma flash fireball
 * 2. Sky-piercing vertical energy pillar
 * 3. Dual-layer expanding hemispherical shock dome
 * 4. Triple concentric ground blast shock rings
 * 5. Radial energetic plasma spikes
 */
public class NuclearShockRenderer extends EntityRenderer<NuclearShockEntity> {

    public NuclearShockRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(NuclearShockEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (NuclearShockOculusPipeline.enqueue(entity, partialTicks)) {
            return;
        }

        float timeSeconds = (entity.getLifetime() + partialTicks) / 30.0F;
        if (timeSeconds < 0.88F || timeSeconds > 2.24F) {
            return;
        }

        poseStack.pushPose();
        VertexConsumer consumer = buffer.getBuffer(KabladeRenderTypes.nuclearShockDome());
        renderLayers(poseStack, consumer, timeSeconds);
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    public static void renderLayers(PoseStack poseStack, VertexConsumer consumer, float timeSeconds) {
        if (timeSeconds < 0.88F || timeSeconds > 2.24F) {
            return;
        }

        float burstTime = timeSeconds - 0.88F;

        // 1. 地面零点核爆聚能火球闪光 (Ground-Zero Burst Flash)
        if (burstTime < 0.38F) {
            renderFlashCore(poseStack, consumer, burstTime);
        }

        // 2. 垂直贯通喷涌光柱 (Sky-Piercing Energy Pillar)
        if (burstTime < 0.55F) {
            renderPillar(poseStack, consumer, burstTime);
        }

        // 3. 双层红黑-赤金半球能量穹顶 (Dual-Layer Shock Dome - 0 -> 4.8m)
        float domeProgress = Mth.clamp(burstTime / 0.52F, 0.0F, 1.0F);
        float currentRadius = (1.0F - (float) Math.pow(1.0F - domeProgress, 3)) * 4.8F;
        float alpha = burstTime < 0.45F ? 1.0F : Mth.clamp(1.0F - (burstTime - 0.45F) / 0.91F, 0.0F, 1.0F);

        // 主冲击半球穹顶
        renderHemisphereDome(poseStack, consumer, currentRadius, alpha, 1.0F, 0.35F, 0.08F);
        // 外层微透冲击波膜
        if (currentRadius > 0.5F) {
            renderHemisphereDome(poseStack, consumer, currentRadius * 1.06F, alpha * 0.5F, 1.0F, 0.55F, 0.15F);
        }

        // 4. 地面三重旋转冲击震波带 (Triple Concentric Ground Shock Ribbons)
        if (burstTime < 1.15F) {
            renderGroundRibbons(poseStack, consumer, currentRadius, burstTime, alpha);
        }

        // 5. 向上扇形放射高能尖刺 (Radial Plasma Spikes)
        if (burstTime < 0.70F) {
            renderRadialSpikes(poseStack, consumer, burstTime, alpha);
        }
    }

    private static void renderFlashCore(PoseStack ps, VertexConsumer consumer, float t) {
        Matrix4f mat = ps.last().pose();
        float flashProgress = t / 0.38F;
        float flashRadius = (1.0F - (float) Math.pow(1.0F - flashProgress, 2)) * 2.2F;
        float flashAlpha = 1.0F - flashProgress;

        int rings = 8;
        int segments = 16;
        for (int i = 0; i < rings; i++) {
            float phi1 = ((float) i / rings) * (Mth.PI / 2.0F);
            float phi2 = ((float) (i + 1) / rings) * (Mth.PI / 2.0F);

            for (int j = 0; j < segments; j++) {
                float theta1 = ((float) j / segments) * Mth.TWO_PI;
                float theta2 = ((float) (j + 1) / segments) * Mth.TWO_PI;

                putFlashVertex(consumer, mat, flashRadius, phi1, theta1, flashAlpha);
                putFlashVertex(consumer, mat, flashRadius, phi1, theta2, flashAlpha);
                putFlashVertex(consumer, mat, flashRadius, phi2, theta2, flashAlpha);
                putFlashVertex(consumer, mat, flashRadius, phi2, theta1, flashAlpha);
            }
        }
    }

    private static void putFlashVertex(VertexConsumer consumer, Matrix4f mat, float r, float phi, float theta, float alpha) {
        float x = r * Mth.cos(phi) * Mth.cos(theta);
        float y = r * Mth.sin(phi) + 0.15F;
        float z = r * Mth.cos(phi) * Mth.sin(theta);
        consumer.vertex(mat, x, y, z)
                .color(1.0F, 0.95F, 0.75F, alpha * 0.95F)
                .uv(theta / Mth.TWO_PI, phi / (Mth.PI / 2.0F))
                .endVertex();
    }

    private static void renderHemisphereDome(PoseStack ps, VertexConsumer consumer, float radius, float alpha,
                                             float rCol, float gCol, float bCol) {
        Matrix4f mat = ps.last().pose();
        int rings = 16;
        int segments = 32;

        for (int i = 0; i < rings; i++) {
            float phi1 = ((float) i / rings) * (Mth.PI / 2.0F);
            float phi2 = ((float) (i + 1) / rings) * (Mth.PI / 2.0F);

            for (int j = 0; j < segments; j++) {
                float theta1 = ((float) j / segments) * Mth.TWO_PI;
                float theta2 = ((float) (j + 1) / segments) * Mth.TWO_PI;

                putDomeVertex(consumer, mat, radius, phi1, theta1, alpha, rCol, gCol, bCol);
                putDomeVertex(consumer, mat, radius, phi1, theta2, alpha, rCol, gCol, bCol);
                putDomeVertex(consumer, mat, radius, phi2, theta2, alpha, rCol, gCol, bCol);
                putDomeVertex(consumer, mat, radius, phi2, theta1, alpha, rCol, gCol, bCol);
            }
        }
    }

    private static void putDomeVertex(VertexConsumer consumer, Matrix4f mat, float r, float phi, float theta,
                                      float alpha, float rCol, float gCol, float bCol) {
        float x = r * Mth.cos(phi) * Mth.cos(theta);
        float y = r * Mth.sin(phi);
        float z = r * Mth.cos(phi) * Mth.sin(theta);
        consumer.vertex(mat, x, y, z)
                .color(rCol, gCol, bCol, alpha * 0.92F)
                .uv(theta / Mth.TWO_PI, phi / (Mth.PI / 2.0F))
                .endVertex();
    }

    private static void renderPillar(PoseStack ps, VertexConsumer consumer, float t) {
        Matrix4f mat = ps.last().pose();
        float height = 13.5F;
        float progress = t / 0.55F;
        float width = 0.85F * (1.0F - (float) Math.pow(progress, 2));
        float alpha = 1.0F - progress;

        // 4-way crossed central energy blades
        for (int k = 0; k < 4; k++) {
            float rot = k * (Mth.PI / 4.0F) + t * 4.0F;
            float cos = Mth.cos(rot) * width;
            float sin = Mth.sin(rot) * width;

            consumer.vertex(mat, -cos, 0.05F, -sin).color(1.0F, 0.95F, 0.6F, alpha * 0.95F).uv(0, 0).endVertex();
            consumer.vertex(mat, cos, 0.05F, sin).color(1.0F, 0.95F, 0.6F, alpha * 0.95F).uv(1, 0).endVertex();
            consumer.vertex(mat, cos * 0.6F, height, sin * 0.6F).color(1.0F, 0.35F, 0.05F, 0.0F).uv(1, 1).endVertex();
            consumer.vertex(mat, -cos * 0.6F, height, -sin * 0.6F).color(1.0F, 0.35F, 0.05F, 0.0F).uv(0, 1).endVertex();
        }

        // Surrounding cylindrical plasma sheath
        int cylinderSegments = 16;
        float cylRadius = width * 1.35F;
        for (int j = 0; j < cylinderSegments; j++) {
            float th1 = ((float) j / cylinderSegments) * Mth.TWO_PI + t * 6.0F;
            float th2 = ((float) (j + 1) / cylinderSegments) * Mth.TWO_PI + t * 6.0F;

            float x1 = Mth.cos(th1) * cylRadius;
            float z1 = Mth.sin(th1) * cylRadius;
            float x2 = Mth.cos(th2) * cylRadius;
            float z2 = Mth.sin(th2) * cylRadius;

            consumer.vertex(mat, x1, 0.05F, z1).color(1.0F, 0.65F, 0.15F, alpha * 0.75F).uv(0, 0).endVertex();
            consumer.vertex(mat, x2, 0.05F, z2).color(1.0F, 0.65F, 0.15F, alpha * 0.75F).uv(1, 0).endVertex();
            consumer.vertex(mat, x2 * 0.7F, height * 0.85F, z2 * 0.7F).color(1.0F, 0.20F, 0.02F, 0.0F).uv(1, 1).endVertex();
            consumer.vertex(mat, x1 * 0.7F, height * 0.85F, z1 * 0.7F).color(1.0F, 0.20F, 0.02F, 0.0F).uv(0, 1).endVertex();
        }
    }

    private static void renderGroundRibbons(PoseStack ps, VertexConsumer consumer, float r, float t, float alpha) {
        Matrix4f mat = ps.last().pose();
        float rotAngle = t * 7.5F;
        int segs = 36;

        // 环 1: 主扩散冲击波带
        float rMain = r * 1.12F;
        float wMain = 0.55F;
        for (int j = 0; j < segs; j++) {
            float th1 = ((float) j / segs) * Mth.TWO_PI + rotAngle;
            float th2 = ((float) (j + 1) / segs) * Mth.TWO_PI + rotAngle;

            consumer.vertex(mat, (rMain - wMain) * Mth.cos(th1), 0.03F, (rMain - wMain) * Mth.sin(th1))
                    .color(1.0F, 0.65F, 0.15F, alpha * 0.9F).uv(0, 0).endVertex();
            consumer.vertex(mat, (rMain + wMain) * Mth.cos(th1), 0.03F, (rMain + wMain) * Mth.sin(th1))
                    .color(1.0F, 0.15F, 0.02F, 0.0F).uv(1, 0).endVertex();
            consumer.vertex(mat, (rMain + wMain) * Mth.cos(th2), 0.03F, (rMain + wMain) * Mth.sin(th2))
                    .color(1.0F, 0.15F, 0.02F, 0.0F).uv(1, 1).endVertex();
            consumer.vertex(mat, (rMain - wMain) * Mth.cos(th2), 0.03F, (rMain - wMain) * Mth.sin(th2))
                    .color(1.0F, 0.65F, 0.15F, alpha * 0.9F).uv(0, 1).endVertex();
        }

        // 环 2: 内层高密度等离子光盘 (Inner Plasma Disk)
        float rInner = r * 0.65F;
        float wInner = 0.40F;
        for (int j = 0; j < segs; j++) {
            float th1 = ((float) j / segs) * Mth.TWO_PI - rotAngle * 1.4F;
            float th2 = ((float) (j + 1) / segs) * Mth.TWO_PI - rotAngle * 1.4F;

            consumer.vertex(mat, (rInner - wInner) * Mth.cos(th1), 0.035F, (rInner - wInner) * Mth.sin(th1))
                    .color(1.0F, 0.90F, 0.35F, alpha * 0.85F).uv(0, 0).endVertex();
            consumer.vertex(mat, (rInner + wInner) * Mth.cos(th1), 0.035F, (rInner + wInner) * Mth.sin(th1))
                    .color(1.0F, 0.40F, 0.05F, 0.0F).uv(1, 0).endVertex();
            consumer.vertex(mat, (rInner + wInner) * Mth.cos(th2), 0.035F, (rInner + wInner) * Mth.sin(th2))
                    .color(1.0F, 0.40F, 0.05F, 0.0F).uv(1, 1).endVertex();
            consumer.vertex(mat, (rInner - wInner) * Mth.cos(th2), 0.035F, (rInner - wInner) * Mth.sin(th2))
                    .color(1.0F, 0.90F, 0.35F, alpha * 0.85F).uv(0, 1).endVertex();
        }

        // 环 3: 外延 6.8m 极限震荡前锋波 (Outer Shockwave Wavefront)
        float rOuter = Mth.clamp(t / 0.45F, 0.0F, 1.0F) * 6.8F;
        float wOuter = 0.28F;
        float outerAlpha = (1.0F - Mth.clamp(t / 0.85F, 0.0F, 1.0F)) * alpha;
        if (outerAlpha > 0.01F) {
            for (int j = 0; j < segs; j++) {
                float th1 = ((float) j / segs) * Mth.TWO_PI;
                float th2 = ((float) (j + 1) / segs) * Mth.TWO_PI;

                consumer.vertex(mat, (rOuter - wOuter) * Mth.cos(th1), 0.025F, (rOuter - wOuter) * Mth.sin(th1))
                        .color(1.0F, 0.45F, 0.08F, outerAlpha * 0.8F).uv(0, 0).endVertex();
                consumer.vertex(mat, (rOuter + wOuter) * Mth.cos(th1), 0.025F, (rOuter + wOuter) * Mth.sin(th1))
                        .color(1.0F, 0.10F, 0.01F, 0.0F).uv(1, 0).endVertex();
                consumer.vertex(mat, (rOuter + wOuter) * Mth.cos(th2), 0.025F, (rOuter + wOuter) * Mth.sin(th2))
                        .color(1.0F, 0.10F, 0.01F, 0.0F).uv(1, 1).endVertex();
                consumer.vertex(mat, (rOuter - wOuter) * Mth.cos(th2), 0.025F, (rOuter - wOuter) * Mth.sin(th2))
                        .color(1.0F, 0.45F, 0.08F, outerAlpha * 0.8F).uv(0, 1).endVertex();
            }
        }
    }

    private static void renderRadialSpikes(PoseStack ps, VertexConsumer consumer, float t, float alpha) {
        Matrix4f mat = ps.last().pose();
        float progress = t / 0.70F;
        float spikeLength = 3.6F * (1.0F - (float) Math.pow(progress, 1.5));
        float spikeAlpha = (1.0F - progress) * alpha;

        int numSpikes = 24;
        for (int i = 0; i < numSpikes; i++) {
            float angle = ((float) i / numSpikes) * Mth.TWO_PI + t * 2.0F;
            float elevation = 0.25F + 0.35F * ((i % 3) / 2.0F); // 3-level elevation
            float dx = Mth.cos(angle) * spikeLength;
            float dz = Mth.sin(angle) * spikeLength;
            float dy = elevation * spikeLength;

            float width = 0.22F * (1.0F - progress);
            float perpX = -Mth.sin(angle) * width;
            float perpZ = Mth.cos(angle) * width;

            // 3D spike blade
            consumer.vertex(mat, -perpX, 0.08F, -perpZ).color(1.0F, 0.95F, 0.5F, spikeAlpha * 0.95F).uv(0, 0).endVertex();
            consumer.vertex(mat, perpX, 0.08F, perpZ).color(1.0F, 0.95F, 0.5F, spikeAlpha * 0.95F).uv(1, 0).endVertex();
            consumer.vertex(mat, dx, dy, dz).color(1.0F, 0.25F, 0.02F, 0.0F).uv(1, 1).endVertex();
            consumer.vertex(mat, 0, 0.08F, 0).color(1.0F, 0.95F, 0.5F, spikeAlpha * 0.95F).uv(0, 1).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NuclearShockEntity entity) {
        return KabladeRenderTypes.FALLBACK_TEXTURE;
    }
}

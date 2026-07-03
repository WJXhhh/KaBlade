package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.WindEnchantmentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders Wind Enchantment as four flat XZ-plane magic-circle layers.
 *
 * <p>The normal path keeps the original lightweight textured quads. When
 * Oculus/Iris is using a shader pack, the same quads are submitted through a
 * standard fullbright entity RenderType so the shader pipeline can pick them up.</p>
 */
public class WindEnchantmentRenderer extends EntityRenderer<WindEnchantmentEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final ResourceLocation TEX_EFFECT1 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect1.png");
    private static final ResourceLocation TEX_EFFECT2 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect2.png");
    private static final ResourceLocation TEX_EFFECT3 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect3.png");
    private static final ResourceLocation TEX_EFFECT4 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect4.png");

    private static final Layer[] LAYERS = {
            new Layer(TEX_EFFECT1, "effect1", 1.0F, 1.0F),
            new Layer(TEX_EFFECT2, "effect2", 0.5F, 1.0F),
            new Layer(TEX_EFFECT3, "effect3", 0.1F, 1.0F),
            new Layer(TEX_EFFECT4, "effect4", 0.1F, 0.4F),
    };

    public WindEnchantmentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WindEnchantmentEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int renderTick = entity.getRenderTick();
        if (renderTick <= 0) {
            return;
        }

        boolean shaderpackFallback = KabladeRenderTypes.useShaderFallbackTextures();
        for (Layer layer : LAYERS) {
            float angle = renderTick * entity.rates.getOrDefault(layer.rateKey, 0.0F);

            poseStack.pushPose();
            poseStack.translate(0.0F, layer.yOffset, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float halfSize = 10.0F * layer.scale;

            if (shaderpackFallback) {
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(layer.texture));
                emitTopFaceFullbright(vc, pose, halfSize);
                emitBottomFaceFullbright(vc, pose, halfSize);
            } else {
                VertexConsumer vc = buffer.getBuffer(KabladeRenderTypes.windEnchantment(layer.texture));
                emitTopFace(vc, mat, halfSize);
                emitBottomFace(vc, mat, halfSize);
            }

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WindEnchantmentEntity entity) {
        return TEX_EFFECT1;
    }

    private static void emitTopFace(VertexConsumer vc, Matrix4f mat, float halfSize) {
        vertex(vc, mat, -halfSize, halfSize, 0.0F, 0.0F);
        vertex(vc, mat, halfSize, halfSize, 1.0F, 0.0F);
        vertex(vc, mat, halfSize, -halfSize, 1.0F, 1.0F);
        vertex(vc, mat, -halfSize, -halfSize, 0.0F, 1.0F);
    }

    private static void emitBottomFace(VertexConsumer vc, Matrix4f mat, float halfSize) {
        vertex(vc, mat, -halfSize, -halfSize, 0.0F, 0.0F);
        vertex(vc, mat, halfSize, -halfSize, 1.0F, 0.0F);
        vertex(vc, mat, halfSize, halfSize, 1.0F, 1.0F);
        vertex(vc, mat, -halfSize, halfSize, 0.0F, 1.0F);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float z, float u, float v) {
        vc.vertex(mat, x, 0.0F, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .endVertex();
    }

    private static void emitTopFaceFullbright(VertexConsumer vc, PoseStack.Pose pose, float halfSize) {
        Matrix4f mat = pose.pose();
        Matrix3f normal = pose.normal();
        fullbrightVertex(vc, mat, normal, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        fullbrightVertex(vc, mat, normal, halfSize, halfSize, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        fullbrightVertex(vc, mat, normal, halfSize, -halfSize, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        fullbrightVertex(vc, mat, normal, -halfSize, -halfSize, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
    }

    private static void emitBottomFaceFullbright(VertexConsumer vc, PoseStack.Pose pose, float halfSize) {
        Matrix4f mat = pose.pose();
        Matrix3f normal = pose.normal();
        fullbrightVertex(vc, mat, normal, -halfSize, -halfSize, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F);
        fullbrightVertex(vc, mat, normal, halfSize, -halfSize, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
        fullbrightVertex(vc, mat, normal, halfSize, halfSize, 1.0F, 1.0F, 0.0F, -1.0F, 0.0F);
        fullbrightVertex(vc, mat, normal, -halfSize, halfSize, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F);
    }

    private static void fullbrightVertex(VertexConsumer vc, Matrix4f mat, Matrix3f normal,
                                         float x, float z, float u, float v,
                                         float normalX, float normalY, float normalZ) {
        vc.vertex(mat, x, 0.0F, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private record Layer(ResourceLocation texture, String rateKey, float yOffset, float scale) {}
}

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
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 风之结界的四层贴地法阵。
 *
 * <p>1.12.2 原版绘制的是 XZ 平面上的 20×20 四边形，不是面向镜头的竖直 billboard。
 * 每层像旧版一样分别提交上、下两个面，保留原有的绕序和 UV 朝向。</p>
 */
public class WindEnchantmentRenderer extends EntityRenderer<WindEnchantmentEntity> {

    private static final ResourceLocation TEX_EFFECT1 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect1.png");
    private static final ResourceLocation TEX_EFFECT2 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect2.png");
    private static final ResourceLocation TEX_EFFECT3 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect3.png");
    private static final ResourceLocation TEX_EFFECT4 =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/entity/wind_enchantment/effect4.png");

    private static final Layer[] LAYERS = {
            new Layer(TEX_EFFECT1, "effect1", 1.0f, 1.0f),
            new Layer(TEX_EFFECT2, "effect2", 0.5f, 1.0f),
            new Layer(TEX_EFFECT3, "effect3", 0.1f, 1.0f),
            new Layer(TEX_EFFECT4, "effect4", 0.1f, 0.4f),
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

        for (Layer layer : LAYERS) {
            RenderType renderType = KabladeRenderTypes.windEnchantment(layer.texture);
            VertexConsumer vc = buffer.getBuffer(renderType);

            // 1.12.2 原版直接使用整数 renderTick，不做 partial-tick 插值。
            float angle = renderTick * entity.rates.getOrDefault(layer.rateKey, 0.0F);

            poseStack.pushPose();
            poseStack.translate(0, layer.yOffset, 0);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));

            Matrix4f mat = poseStack.last().pose();
            float halfSize = 10.0f * layer.scale;

            emitTopFace(vc, mat, halfSize);
            emitBottomFace(vc, mat, halfSize);

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WindEnchantmentEntity entity) {
        return TEX_EFFECT1;
    }

    private static void emitTopFace(VertexConsumer vc, Matrix4f mat, float halfSize) {
        // 等价于旧版绘制前的 rotate(-180, X)：向上绕序，V 轴翻转。
        vertex(vc, mat, -halfSize, halfSize, 0, 0);
        vertex(vc, mat, halfSize, halfSize, 1, 0);
        vertex(vc, mat, halfSize, -halfSize, 1, 1);
        vertex(vc, mat, -halfSize, -halfSize, 0, 1);
    }

    private static void emitBottomFace(VertexConsumer vc, Matrix4f mat, float halfSize) {
        vertex(vc, mat, -halfSize, -halfSize, 0, 0);
        vertex(vc, mat, halfSize, -halfSize, 1, 0);
        vertex(vc, mat, halfSize, halfSize, 1, 1);
        vertex(vc, mat, -halfSize, halfSize, 0, 1);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat,
                               float x, float z, float u, float v) {
        // VertexConsumer#color 在这里命中 0..255 整数重载。
        vc.vertex(mat, x, 0, z).color(255, 255, 255, 255).uv(u, v).endVertex();
    }

    private record Layer(ResourceLocation texture, String rateKey, float yOffset, float scale) {}
}

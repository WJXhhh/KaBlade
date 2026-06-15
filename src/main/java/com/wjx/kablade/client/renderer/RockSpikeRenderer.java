package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.client.model.RockSpikeModel;
import com.wjx.kablade.entity.RockSpikeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 岩刺渲染器。模型几何是静态的，「从地里顶出来再缩回」的动感全靠这里用 {@link PoseStack} 做：
 * <ul>
 *   <li>出生头 {@link #RISE_TICKS} tick：缓出式上顶（先快后稳），之前几乎全埋在地下（被地形挡住）。</li>
 *   <li>临终前 {@link #SINK_TICKS} tick：缓入式缩回地里。</li>
 *   <li>按 {@code variant} 给每根刺一点固定倾斜，整片刺林才不呆板。</li>
 * </ul>
 */
public class RockSpikeRenderer extends EntityRenderer<RockSpikeEntity> {

    /** 复用原版石头贴图，免去额外美术文件；偏暖灰的 tint 呼应「千岩」的土岩质感。 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");

    private static final float RISE_TICKS = 4.0F;
    private static final float SINK_TICKS = 5.0F;
    /** 出土后灼热发光持续多久（tick）再冷却成普通石头。 */
    private static final float HEAT_FADE_TICKS = 11.0F;

    private static final float TINT_R = 0.82F;
    private static final float TINT_G = 0.76F;
    private static final float TINT_B = 0.68F;

    /** 模型最高点 = 32 单位 = 2 格（再乘体型）。 */
    private static final float SPIKE_BLOCKS = 32.0F / 16.0F;

    private final RockSpikeModel model;

    public RockSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RockSpikeModel(context.bakeLayer(RockSpikeModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RockSpikeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        int life = entity.getLifetime();
        float scale = entity.getScaleFactor();
        float worldHeight = SPIKE_BLOCKS * scale;

        // 出土：缓出（先快后稳，砸地的爆发感）
        float rise = Mth.clamp(age / RISE_TICKS, 0.0F, 1.0F);
        float riseEase = 1.0F - (1.0F - rise) * (1.0F - rise);

        // 缩回：缓入
        float sink = 0.0F;
        float sinkStart = life - SINK_TICKS;
        if (age > sinkStart) {
            float s = Mth.clamp((age - sinkStart) / SINK_TICKS, 0.0F, 1.0F);
            sink = s * s;
        }

        float exposure = riseEase * (1.0F - sink);     // 0..1 露出地面的比例
        float buried = (1.0F - exposure) * worldHeight; // 仍埋在地下的高度（世界格）

        poseStack.pushPose();
        poseStack.translate(0.0D, -buried, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        // 固定倾斜（由 variant 决定，两端一致）：约 −7°..+7°
        float lean = (((entity.getVariant() * 47) % 21) - 10) * 0.7F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(lean));

        // 翻转到世界朝向 + 应用体型（两个负号保持手性，正面剔除不会翻）
        poseStack.scale(-scale, -scale, scale);

        VertexConsumer vc = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
                TINT_R, TINT_G, TINT_B, 1.0F);

        // 熔岩余热：刚出土时整根叠加一层全亮的橙红辉光（加色混合），随后逐渐冷却消失，
        // 配上轻微的脉动，像一根烧红的岩矛在慢慢变暗。
        float heat = 1.0F - Mth.clamp(age / HEAT_FADE_TICKS, 0.0F, 1.0F);
        if (heat > 0.02F) {
            float pulse = 0.78F + 0.22F * Mth.sin(age * 0.9F);
            float k = heat * heat * pulse;   // 平方衰减，冷却收尾更干净
            VertexConsumer glow = buffer.getBuffer(RenderType.eyes(TEXTURE));
            this.model.renderToBuffer(poseStack, glow, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.5F * k, 0.55F * k, 0.14F * k, 1.0F);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RockSpikeEntity entity) {
        return TEXTURE;
    }
}

package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FallingPetalsMarkRenderer {

    private static final ResourceLocation SAKURA_BRAND =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/icon/sakura_brand.png");
    private static final RenderType SAKURA_BRAND_RENDER_TYPE =
            KabladeRenderTypes.huntingLocker(SAKURA_BRAND);

    private FallingPetalsMarkRenderer() {
    }

    @SubscribeEvent
    public static <T extends LivingEntity, M extends EntityModel<T>> void onRenderLiving(RenderLivingEvent.Post<T, M> event) {
        if (!FallingPetalsClientState.isMarked(event.getEntity().getId())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, event.getEntity().getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((event.getEntity().tickCount + event.getPartialTick()) * 8.0F));
        poseStack.scale(1.0F, 1.0F, 1.0F);

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(SAKURA_BRAND_RENDER_TYPE);
        int light = 0x00F000F0;
        iconVertex(vc, mat, -0.5F, -0.5F, 0.0F, 1.0F, light);
        iconVertex(vc, mat, 0.5F, -0.5F, 1.0F, 1.0F, light);
        iconVertex(vc, mat, 0.5F, 0.5F, 1.0F, 0.0F, light);
        iconVertex(vc, mat, -0.5F, 0.5F, 0.0F, 0.0F, light);
        poseStack.popPose();
    }

    private static void iconVertex(VertexConsumer vc, Matrix4f mat, float x, float y, float u, float v, int light) {
        vc.vertex(mat, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}

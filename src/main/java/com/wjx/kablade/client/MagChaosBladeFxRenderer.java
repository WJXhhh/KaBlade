package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
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
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MagChaosBladeFxRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/effect/tex_mag_chaos_blade_effect.png");
    private static final Map<Integer, ActiveFx> ACTIVE = new ConcurrentHashMap<>();

    private MagChaosBladeFxRenderer() {
    }

    public static void start(int entityId, int duration, double x, double y, double z, float yaw) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        ACTIVE.put(entityId, new ActiveFx(now, now + duration, x, y, z, yaw));
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
        var renderType = KabladeRenderTypes.magChaosBlade(TEXTURE);
        VertexConsumer vc = buffers.getBuffer(renderType);

        Iterator<Map.Entry<Integer, ActiveFx>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ActiveFx> entry = iterator.next();
            ActiveFx fx = entry.getValue();
            if (tick > fx.endTick) {
                iterator.remove();
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            double age = tick + partial - fx.startTick;
            float alpha = Mth.clamp((float) (fx.endTick - (tick + partial)) / 3.0F, 0.0F, 1.0F)
                    * Mth.clamp((float) age / 1.0F, 0.0F, 1.0F);

            poseStack.pushPose();
            poseStack.translate(fx.x - camera.x, fx.y - camera.y, fx.z - camera.z);
            poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(fx.yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(0.0D, -2.0D, 0.0D);
            poseStack.translate(0.0D, 0.0D, 1.0D);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(5.25F, 5.25F, 5.25F);
            emitQuad(vc, poseStack.last().pose(), alpha);
            poseStack.popPose();
        }

        buffers.endBatch(renderType);
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f mat, float alpha) {
        vertex(vc, mat, -0.5F, -0.25F, 0.0F, 0.0F, 1.0F, alpha);
        vertex(vc, mat, 0.5F, -0.25F, 0.0F, 1.0F, 1.0F, alpha);
        vertex(vc, mat, 0.5F, 0.75F, 0.0F, 1.0F, 0.0F, alpha);
        vertex(vc, mat, -0.5F, 0.75F, 0.0F, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                               float u, float v, float alpha) {
        vc.vertex(mat, x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0x00F000F0)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static final class ActiveFx {
        private final long startTick;
        private long endTick;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;

        private ActiveFx(long startTick, long endTick, double x, double y, double z, float yaw) {
            this.startTick = startTick;
            this.endTick = endTick;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }
}

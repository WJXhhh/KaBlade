package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.renderer.RaidenCycloneOculusPipeline;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.network.SunfireImpactPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

/** Expanding sunfire rings; Oculus uses the existing depth-aware HDR geometry pipeline. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SunfireImpactRenderer {
    private static final List<Fx> ACTIVE = new ArrayList<>();
    private SunfireImpactRenderer() { }
    public static void start(SunfireImpactPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level != null) ACTIVE.add(new Fx(new Vec3(packet.x(), packet.y(), packet.z()), level.getGameTime()));
    }
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        boolean oculus = ShaderCompat.shouldUseOculusPostPath();
        if (event.getStage() != (oculus ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                : RenderLevelStageEvent.Stage.AFTER_PARTICLES)) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        double now = mc.level.getGameTime() + event.getPartialTick();
        ACTIVE.removeIf(fx -> now - fx.start > 16);
        if (ACTIVE.isEmpty()) return;
        if (oculus) {
            RaidenCycloneOculusPipeline.render(event, (bright, dark) -> emit(event, bright, now));
        } else {
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            EffectVertexBatch batch = new EffectVertexBatch();
            emit(event, batch.getBuffer(KabladeRenderTypes.sunfireImpact()), now);
            batch.submit(buffers);
            buffers.endBatch(KabladeRenderTypes.sunfireImpact());
        }
    }
    private static void emit(RenderLevelStageEvent event, VertexConsumer vertex, double now) {
        Vec3 camera = event.getCamera().getPosition();
        for (Fx fx : ACTIVE) {
            float t = (float) Math.max(0, Math.min(1, (now - fx.start) / 16.0));
            float alpha = (1 - t) * 0.9F;
            float radius = 0.45F + 3.55F * (1 - (1 - t) * (1 - t));
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(fx.pos.x - camera.x, fx.pos.y - camera.y + 0.08, fx.pos.z - camera.z);
            Matrix4f matrix = pose.last().pose();
            ring(vertex, matrix, radius, 0.35F, alpha);
            ring(vertex, matrix, radius * 0.77F, 0.15F, alpha * 0.7F);
            pose.popPose();
        }
    }
    private static void ring(VertexConsumer out, Matrix4f matrix, float radius, float width, float alpha) {
        int segments = 96;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            float u0 = (float)i / segments, u1 = (float)(i + 1) / segments;
            point(out, matrix, a, radius - width, u0, 0, alpha);
            point(out, matrix, a, radius + width, u0, 1, alpha);
            point(out, matrix, b, radius + width, u1, 1, alpha);
            point(out, matrix, b, radius - width, u1, 0, alpha);
        }
    }
    private static void point(VertexConsumer out, Matrix4f matrix, double angle, float r, float u, float v, float alpha) {
        out.vertex(matrix, (float)Math.cos(angle) * r, 0, (float)Math.sin(angle) * r)
                .color(1.0F, 0.82F, 0.08F, alpha).uv(u, v).endVertex();
    }
    private record Fx(Vec3 pos, long start) { }
}

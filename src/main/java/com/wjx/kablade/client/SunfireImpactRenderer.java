package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.client.renderer.RaidenCycloneOculusPipeline;
import com.wjx.kablade.client.shader.ShaderCompat;
import com.wjx.kablade.entity.SunfireImpactEntity;
import com.wjx.kablade.network.SunfireImpactPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Both render paths consume exactly the same contact-synchronized flame geometry. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT)
public final class SunfireImpactRenderer {
    private SunfireImpactRenderer() { }

    public static void start(SunfireImpactPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        // Only confirmed successful hits receive sparks. Persistent flames use tracked cast data.
        for (int id : packet.hitIds()) {
            var target = level.getEntity(id);
            if (target == null) continue;
            for (int i = 0; i < 24; i++) {
                var random = level.random;
                level.addParticle(i % 3 == 0 ? ParticleTypes.CRIT : ParticleTypes.FLAME,
                        target.getX() + (random.nextDouble() - 0.5) * target.getBbWidth(),
                        target.getY() + random.nextDouble() * target.getBbHeight(),
                        target.getZ() + (random.nextDouble() - 0.5) * target.getBbWidth(),
                        (random.nextDouble() - 0.5) * 0.3, 0.12 + random.nextDouble() * 0.25,
                        (random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        boolean oculus = ShaderCompat.shouldUseOculusPostPath();
        if (event.getStage() != (oculus ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                : RenderLevelStageEvent.Stage.AFTER_PARTICLES)) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var casts = new java.util.ArrayList<SunfireImpactEntity>();
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof SunfireImpactEntity cast && cast.isAlive()
                    && cast.distanceToSqr(event.getCamera().getPosition()) < 96 * 96) casts.add(cast);
        }
        if (casts.isEmpty()) return;
        if (oculus && RaidenCycloneOculusPipeline.render(event, (bright, dark) -> {
            for (var cast : casts) SunfireImpactGeometry.emit(event, bright, cast);
        })) return;
        var buffers = mc.renderBuffers().bufferSource();
        EffectVertexBatch batch = new EffectVertexBatch();
        var vertex = batch.getBuffer(KabladeRenderTypes.sunfireImpact());
        for (var cast : casts) SunfireImpactGeometry.emit(event, vertex, cast);
        batch.submit(buffers);
        buffers.endBatch(KabladeRenderTypes.sunfireImpact());
    }
}

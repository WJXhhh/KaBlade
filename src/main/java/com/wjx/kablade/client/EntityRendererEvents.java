package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.client.model.RockSpikeModel;
import com.wjx.kablade.client.renderer.AuroraVeilRenderer;
import com.wjx.kablade.client.renderer.CutMetalRingRenderer;
import com.wjx.kablade.client.renderer.DawnCrescentRenderer;
import com.wjx.kablade.client.renderer.OriginFreeSwordRenderer;
import com.wjx.kablade.client.renderer.RockSpikeRenderer;
import com.wjx.kablade.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client mod-bus subscriber: registers custom entity renderers and model layers. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EntityRendererEvents {

    private EntityRendererEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROCK_SPIKE.get(), RockSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.AURORA_VEIL.get(), AuroraVeilRenderer::new);
        event.registerEntityRenderer(ModEntities.DAWN_CRESCENT.get(), DawnCrescentRenderer::new);
        event.registerEntityRenderer(ModEntities.CUT_METAL_RING.get(), CutMetalRingRenderer::new);
        event.registerEntityRenderer(ModEntities.ORIGIN_FREE_SWORD.get(), OriginFreeSwordRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RockSpikeModel.LAYER, RockSpikeModel::createBodyLayer);
    }
}

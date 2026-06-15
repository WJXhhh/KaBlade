package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.client.model.RockSpikeModel;
import com.wjx.kablade.client.renderer.RockSpikeRenderer;
import com.wjx.kablade.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client mod-bus subscriber: registers the rock-spike renderer and its hand-built model layer. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RockSpikeClientEvents {

    private RockSpikeClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROCK_SPIKE.get(), RockSpikeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RockSpikeModel.LAYER, RockSpikeModel::createBodyLayer);
    }
}

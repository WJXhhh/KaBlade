package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.client.model.RockSpikeModel;
import com.wjx.kablade.client.renderer.*;
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
        event.registerEntityRenderer(ModEntities.VORPAL_BLACK_HOLE.get(), VorpalBlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.RAIKIRI_SHIELD.get(), RaikiriShieldRenderer::new);
        event.registerEntityRenderer(ModEntities.CONFINEMENT_FORCE_FIELD.get(), ConfinementForceFieldRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_BLADE_EDGE.get(), FrostBladeRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOCK_IMPACT.get(), ShockImpactRenderer::new);
        event.registerEntityRenderer(ModEntities.ZAIZAN.get(), ZaizanRenderer::new);

        // SP Light SA entities
        event.registerEntityRenderer(ModEntities.EX_SLASH_DRIVE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.LACERATE_DRIVE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.FLARE_EDGE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.AQUA_EDGE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_SWORD_EX.get(), PhantomSwordExRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_SWORD.get(), PhantomSwordExRenderer::new);
        event.registerEntityRenderer(ModEntities.STAR_SWORD.get(), PhantomSwordExRenderer::new);
        event.registerEntityRenderer(ModEntities.STAGE_LIGHT.get(), StageLightRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_ENCHANTMENT.get(), WindEnchantmentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RockSpikeModel.LAYER, RockSpikeModel::createBodyLayer);
    }
}

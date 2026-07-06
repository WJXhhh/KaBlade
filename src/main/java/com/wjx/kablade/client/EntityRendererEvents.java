package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.client.model.RaikiriShieldModel;
import com.wjx.kablade.client.model.RockSpikeModel;
import com.wjx.kablade.client.renderer.*;
import com.wjx.kablade.init.ModEntities;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
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
        event.registerEntityRenderer(ModEntities.FREEZE_DOMAIN.get(), FreezeDomainRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOCK_IMPACT.get(), ShockImpactRenderer::new);
        event.registerEntityRenderer(ModEntities.THUNDER_EDGE_ATTACK.get(), ThunderEdgeAttackRenderer::new);
        event.registerEntityRenderer(ModEntities.FENGXUAN_DIMENSION.get(), FengxuanDimensionRenderer::new);
        event.registerEntityRenderer(ModEntities.ZAIZAN.get(), ZaizanRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_SAKURA.get(), CrimsonSakuraRenderer::new);
        event.registerEntityRenderer(ModEntities.TUNA.get(), TunaRenderer::new);
        event.registerEntityRenderer(ModEntities.RAIN_UMBRELLA.get(), RainUmbrellaRenderer::new);
        event.registerEntityRenderer(ModEntities.SUMMONED_HEDRA.get(), SummonedHedraRenderer::new);

        // SP Light SA entities
        event.registerEntityRenderer(ModEntities.EX_SLASH_DRIVE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.LACERATE_DRIVE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.FLARE_EDGE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.AQUA_EDGE.get(), ExDriveRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_SWORD_EX.get(), PhantomSwordExRenderer::new);
        event.registerEntityRenderer(ModEntities.BUTTERFLY_SWORD.get(), ButterflySwordRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_SWORD.get(), PhantomSwordExRenderer::new);
        event.registerEntityRenderer(ModEntities.STAR_SWORD.get(), PhantomSwordExRenderer::new);
        event.registerEntityRenderer(ModEntities.STAGE_LIGHT.get(), StageLightRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_ENCHANTMENT.get(), WindEnchantmentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RockSpikeModel.LAYER, RockSpikeModel::createBodyLayer);
        event.registerLayerDefinition(RaikiriShieldModel.LAYER, RaikiriShieldModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onAddLayers(final EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new FreezeLayer<>(renderer));
            }
        }

        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            addFreezeLayer(event, entityType);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addFreezeLayer(EntityRenderersEvent.AddLayers event, EntityType<?> entityType) {
        if (entityType == EntityType.PLAYER) {
            return;
        }

        try {
            LivingEntityRenderer renderer = event.getRenderer((EntityType) entityType);
            if (renderer != null) {
                if (entityType == EntityType.SLIME) {
                    renderer.addLayer(new SlimeFreezeLayer(renderer, event.getEntityModels()));
                } else {
                    renderer.addLayer(new FreezeLayer(renderer));
                }
            }
        } catch (ClassCastException ignored) {
            // Non-living renderers are present in the same renderer map; they cannot receive living layers.
        }
    }
}

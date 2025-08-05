package com.wjx.kablade.util.handlers;

import com.wjx.kablade.Entity.*;
import com.wjx.kablade.Entity.Render.*;
import com.wjx.kablade.ExSA.entity.*;
import com.wjx.kablade.ExSA.entity.render.RenderPhantomSwordEx;
import mods.flammpfeil.slashblade.client.renderer.entity.RenderPhantomSwordBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import com.wjx.kablade.ExSA.entity.render.RenderDriveEx;

public class RenderHandler {
    public static void registerEntityRenders() {
        RenderingRegistry.registerEntityRenderingHandler(SummonBladeOfFrostBlade.class, RenderPhantomSwordBase::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonSwordFree.class, RenderFreeSword::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVorpalBlackHole.class, RenderVorpalBlackHole::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRaikiriBlade.class, RenderRaikiriBlade::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonedSwordBasePlus.class, RenderSummonedSwordBasePlus::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityWine.class, RenderWine::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityFreezeDomain.class, RenderFreezeDomain::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityWindEnchantment.class, RenderWindEnchantment::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityDriveAdd.class,RenderEntityDriveAdd::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySlashDimensionAdd.class,RenderSlashDimensionAdd::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityThunderEdgeAttack.class,RenderThunderEdgeAttack::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonedButterfly.class,RenderSummonedButterFly::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonedSwordPotionEffectAdd.class,RenderSummonedSwordPotion::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCrimsonSakuraAttack.class,RenderCrimsonSakuraAttack::new);
        RenderingRegistry.registerEntityRenderingHandler(ExSaEntityDrive.class,RenderEntityDriveAdd::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityAquaEdge.class,RenderEntityDriveAdd::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityFlareEdge.class,RenderEntityDriveAdd::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPhantomSwordEx.class, RenderPhantomSwordEx::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLightningSword.class, RenderPhantomSwordEx::new);
        RenderingRegistry.registerEntityRenderingHandler(ExSaEntityDrive.class,RenderDriveEx::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTuna.class,RenderEntityTuna::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRainUmbrella.class,RenderEntityRainUmbrella::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonHedra.class,RenderSummonedHedra::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityConfinementForceField.class,RenderConfinementForceField::new);
    }
}

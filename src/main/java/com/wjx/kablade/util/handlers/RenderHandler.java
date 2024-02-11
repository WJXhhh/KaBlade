package com.wjx.kablade.util.handlers;

import com.wjx.kablade.Entity.*;
import com.wjx.kablade.Entity.Render.*;
import mods.flammpfeil.slashblade.client.renderer.entity.RenderPhantomSwordBase;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

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
    }
}

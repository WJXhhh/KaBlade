package com.wjx.kablade.util.handlers;

import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import com.wjx.kablade.Entity.Render.RenderFreeSword;
import com.wjx.kablade.Entity.Render.RenderVorpalBlackHole;
import com.wjx.kablade.Entity.SummonBladeOfFrostBlade;
import mods.flammpfeil.slashblade.client.renderer.entity.RenderPhantomSwordBase;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class RenderHandler {
    public static void registerEntityRenders() {
        RenderingRegistry.registerEntityRenderingHandler(SummonBladeOfFrostBlade.class, RenderPhantomSwordBase::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonSwordFree.class, RenderFreeSword::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVorpalBlackHole.class, RenderVorpalBlackHole::new);
    }
}

package com.wjx.kablade.util.handlers;

import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Entity.Render.RenderFreeSword;
import com.wjx.kablade.Entity.SummonBladeOfFrostBlade;
import mods.flammpfeil.slashblade.client.renderer.entity.RenderPhantomSwordBase;
import mods.flammpfeil.slashblade.client.renderer.entity.RenderSpinningSword;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class RenderHandler {
    public static void registerEntityRenders() {
        RenderingRegistry.registerEntityRenderingHandler(SummonBladeOfFrostBlade.class, RenderPhantomSwordBase::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonSwordFree.class, RenderFreeSword::new);
    }
}

package com.wjx.kablade.util.handlers;

import com.wjx.kablade.Entity.*;
import com.wjx.kablade.Entity.Render.RenderFreeSword;
import com.wjx.kablade.Entity.Render.RenderRaikiriBlade;
import com.wjx.kablade.Entity.Render.RenderSummonedSwordBasePlus;
import com.wjx.kablade.Entity.Render.RenderVorpalBlackHole;
import mods.flammpfeil.slashblade.client.renderer.entity.RenderPhantomSwordBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class RenderHandler {
    public static void registerEntityRenders() {
        RenderingRegistry.registerEntityRenderingHandler(SummonBladeOfFrostBlade.class, RenderPhantomSwordBase::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonSwordFree.class, RenderFreeSword::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVorpalBlackHole.class, RenderVorpalBlackHole::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRaikiriBlade.class, RenderRaikiriBlade::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySummonedSwordBasePlus.class, RenderSummonedSwordBasePlus::new);
    }
}

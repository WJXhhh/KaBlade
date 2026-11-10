package com.wjx.kablade.blades.honkai.claymore;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;

import java.util.List;

/** 日炎大剑 */
public class Sunfire extends BladeDefineBase {
    public Sunfire(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai_claymore/sunfire/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai_claymore/sunfire/tex.png"))
                        .effectColor(0xFF6600)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance().build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "sunfire";
    }
}

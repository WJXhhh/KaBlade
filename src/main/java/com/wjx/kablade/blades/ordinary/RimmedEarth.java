package com.wjx.kablade.blades.ordinary;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class RimmedEarth extends BladeDefineBase {
    public RimmedEarth(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key),new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/rimmed_earth/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/rimmed_earth/tex.png"))
                        .effectColor(0x8B6B3F)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(2.0F)
                        .maxDamage(60)
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 1))
                ));
    }

    @Override
    public String getKey() {
        return "hangtu";
    }
}

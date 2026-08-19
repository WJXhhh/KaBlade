package com.wjx.kablade.blades.honkai.claymore;

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

/**
 * 融核动力剑·改——沿用融核动力剑初型的属性与模型，仅替换为 EX 贴图。
 */
public class NuclearPRIEX extends BladeDefineBase {
    public NuclearPRIEX(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai_claymore/nuclear_pri/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai_claymore/nuclear_pri/tex2.png"))
                        .effectColor(0xFFBB22)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(9.0F)
                        .maxDamage(350)
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "nuclear_pri_ex";
    }
}

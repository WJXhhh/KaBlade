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

/**
 * 流萤「竹光」——竹系列 Lv2。由铁刃「竹光」+ 萤石粉升级。
 * 自带亡灵杀手 II，不可妖化，复用竹光的模型。
 */
public class BambooLumi extends BladeDefineBase {
    public BambooLumi(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/bamboo_lumi/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/bamboo_lumi/tex.png"))
                        .effectColor(0xE8D44D)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(220)
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "bamboo_lumi";
    }
}

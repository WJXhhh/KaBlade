package com.wjx.kablade.blades.ordinary;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 战刃「竹光」——竹系列 Lv2。由铁刃「竹光」+ 铁锭升级。
 * 自带锋利 II，默认妖刀。
 */
public class BambooBattler extends BladeDefineBase {
    public BambooBattler(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/bamboo_battler/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/bamboo_battler/tex.png"))
                        .effectColor(0x607D8B)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(280)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 2),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "bamboo_battler";
    }
}

package com.wjx.kablade.blades.ordinary;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;

import java.util.List;

/**
 * 铁刃「竹光」——竹系列 Lv1。由原版竹光刀 + 铁锭升级而来。
 * 自带妖刀属性，使用独立模型与贴图，耐久较高。
 */
public class BambooIron extends BladeDefineBase {
    public BambooIron(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/bamboo_iron/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/bamboo_iron/tex.png"))
                        .effectColor(0x607D8B)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(190)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "bamboo_iron";
    }
}

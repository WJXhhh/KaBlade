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
 * 自铭「嶙峋」——岩石线 Lv1（花岗岩）。由夯土刀 + 花岗岩升级而来。
 * 复用夯土刀的模型（mdl.obj），仅替换贴图；默认附带锋利 II。
 */
public class RockyHuagang extends BladeDefineBase {
    public RockyHuagang(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/rimmed_earth/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/rocky_huagang/tex.png"))
                        .effectColor(0xB97A6B)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(5.0F)
                        .maxDamage(130)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "wjx/ordinary/rocky_huagang";
    }
}

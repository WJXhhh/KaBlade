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
 * 自铭「矍铄」——岩石线 Lv1（安山岩）。由夯土刀 + 安山岩升级而来。
 * 复用夯土刀的模型（mdl.obj），仅替换贴图；默认附带耐久 II。
 */
public class RockyAnshan extends BladeDefineBase {
    public RockyAnshan(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/rimmed_earth/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/rocky_anshan/tex.png"))
                        .effectColor(0x9E9E9E)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(5.0F)
                        .maxDamage(150)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "wjx/ordinary/rocky_anshan";
    }
}

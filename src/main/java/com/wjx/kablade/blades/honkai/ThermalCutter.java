package com.wjx.kablade.blades.honkai;

import com.wjx.kablade.blades.ModSlashArts;
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
 * 热能切割刃——崩坏线复合系列 Lv2（平行分支），由复合刀·朱雀 + 烈焰棒合成。
 * 从 1.12.2 移植而来，专属 SA「熔铁之刃」（1.12.2 SA 286）。
 * <p>
 * 属性：攻击 11.0、耐久 500、默认妖化、自带火焰附加 II。
 */
public class ThermalCutter extends BladeDefineBase {
    public ThermalCutter(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/rhomphaia/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/rhomphaia/tex_the.png"))
                        .effectColor(0xDAA520)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(11.0F)
                        .maxDamage(500)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.MOLTEN_BLADE.getId())
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "fire_aspect"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "thermal_cutter";
    }
}

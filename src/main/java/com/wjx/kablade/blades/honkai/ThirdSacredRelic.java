package com.wjx.kablade.blades.honkai;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.init.ModSpecialEffects;
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
 * 3rd圣遗物——崩坏线银河新星线延续，由银河追光合成。
 * 从 1.12.2 移植而来，使用 SlashBlade 原版 SA「樱花终结」（1.12.2 SA 7）。
 * <p>
 * 属性：攻击 19.0、耐久 800、默认妖化、自带击退 II + 锋利 II。
 */
public class ThirdSacredRelic extends BladeDefineBase {
    public ThirdSacredRelic(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/third_sacred/mdl3rdsacredrelic.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/third_sacred/tex3rdsacredrelic.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(19.0F)
                        .maxDamage(800)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ResourceLocation.fromNamespaceAndPath("slashblade", "sakura_end"))
                        .addSpecialEffect(ModSpecialEffects.HOLY_ENERGY_IMPACT.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "third_sacred";
    }
}

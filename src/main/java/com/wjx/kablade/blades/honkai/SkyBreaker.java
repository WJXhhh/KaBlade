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
 * 开天剑 —— 崩坏线苗刀分支，由苗刀·电魂 + 红石 + 铬钼钢锭 + 萤石粉合成。
 * 从 1.12.2 移植而来，无 SA，自带特殊效果「天罚（DivinePenalty）」。
 * <p>
 * 属性：攻击 15.0、耐久 660、默认妖化、自带亡灵杀手 II、锋利 II。
 */
public class SkyBreaker extends BladeDefineBase {
    public SkyBreaker(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/sky_breaker/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/sky_breaker/tex.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(15.0F)
                        .maxDamage(660)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .addSpecialEffect(ModSpecialEffects.DIVINE_PENALTY.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 2),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "sky_breaker";
    }
}

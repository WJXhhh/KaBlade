package com.wjx.kablade.blades.honkai;

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
 * 脉冲太刀19式——崩坏线村正分支，由妖刀村正 + 红石块 + 活塞合成。
 * 从 1.12.2 移植而来，无专属 SA（1.12.2 原版也未设置 SpecialAttackType）。
 * <p>
 * 属性：攻击 12.0、耐久 510、默认妖化、自带击退 I。
 */
public class PulseKatanaType19 extends BladeDefineBase {
    public PulseKatanaType19(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/pulse_katanas/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/pulse_katanas/tex_t19.png"))
                        .effectColor(0x33CCFF)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(12.0F)
                        .maxDamage(510)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 3),
                        new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "pulse_katana_t19";
    }
}

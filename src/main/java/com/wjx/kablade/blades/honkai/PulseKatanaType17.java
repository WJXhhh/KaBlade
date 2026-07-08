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
 * 脉冲太刀17式——崩坏线村正分支，由妖刀雨村 + 红石块 + 活塞合成。
 * 从 1.12.2 移植而来，专属 SA「寒霜灵刃」（1.12.2 SA 285）。
 * <p>
 * 属性：攻击 11.0、耐久 500、默认妖化、自带击退 I。
 * 也是等离子影秀的合成前置之一。
 */
public class PulseKatanaType17 extends BladeDefineBase {
    public PulseKatanaType17(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/pulse_katanas/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/pulse_katanas/tex_t17.png"))
                        .effectColor(0x00FFEE)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(11.0F)
                        .maxDamage(500)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.FROST_BLADE.getId())
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "pulse_katana_t17";
    }
}

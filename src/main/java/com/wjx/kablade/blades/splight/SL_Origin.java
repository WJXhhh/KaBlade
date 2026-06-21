package com.wjx.kablade.blades.splight;

import com.wjx.kablade.blades.ModSlashArts;
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
 * 龙一文字线原典刀：龙一「源」。
 * <p>
 * 从 1.12.2 {@code SL_Origin} 移植而来，SA「雷剑裁决」，自带特殊效果「圣光」「爆裂驱动」。
 * 属性：攻击 14.0、耐久 191、默认妖化。
 */
public class SL_Origin extends BladeDefineBase {
    public SL_Origin(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.SP_LIGHT),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/splight/splight/origin/model.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/splight/splight/origin/texture.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(14.0F)
                        .maxDamage(191)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.LIGHTNING_SWORDS.getId())
                        .addSpecialEffect(ModSpecialEffects.SP_LIGHTING.getId())
                        .addSpecialEffect(ModSpecialEffects.BURST_DRIVE.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "looting"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 5),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 3),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 4),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "bane_of_arthropods"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "splight_origin";
    }
}

package com.wjx.kablade.blades.splight;

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
 * 龙一文字线年轻刀（金）：龙一「幼」。
 * <p>
 * 从 1.12.2 {@code SL_Young1} 移植而来，SA「长枪（穿刺）」，自带特殊效果「圣光」。
 * 属性：攻击 7.0、耐久 114、默认妖化。
 */
public class SL_Young1 extends BladeDefineBase {
    public SL_Young1(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.SP_LIGHT),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/splight/splight/splight/young/model.obj"))
                        .textureName(ResourceUtil.getLocation("model/splight/splight/splight/young/texture_o.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(7.0F)
                        .maxDamage(114)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ResourceLocation.fromNamespaceAndPath("slashblade", "piercing"))
                        .addSpecialEffect(ModSpecialEffects.SP_LIGHTING.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "looting"), 1),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 3),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 1),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 3)
                )
        ));
    }

    @Override
    public String getKey() {
        return "splight_young1";
    }
}

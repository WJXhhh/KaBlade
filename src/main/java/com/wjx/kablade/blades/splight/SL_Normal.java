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
 * 龙一文字线常规刀：龙一「无」。
 * <p>
 * 从 1.12.2 {@code SL_Normal} 移植而来，SA「超越斩」，自带特殊效果「圣光」。
 * 属性：攻击 8.0、耐久 60、默认妖化。
 */
public class SL_Normal extends BladeDefineBase {
    public SL_Normal(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.SP_LIGHT),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceLocation.fromNamespaceAndPath("slashblade", "model/named/sange/sange.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/splight/splight/normal/texture.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(60)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.OVER_SLASH.getId())
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
        return "splight_normal";
    }
}

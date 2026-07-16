package com.wjx.kablade.blades.ordinary;

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
 * 源能刃「碎钢」——由复合刃「斩铁」+ 重力微粒 + 钻石 + 红石块在工作台中合成。
 * 从 1.12.2 移植而来，专属 SA「领域压杀」，自带特殊效果「源能裁决」。
 * <p>
 * 属性：攻击 8.0、耐久 700、默认妖化。
 * 自带锋利 V、力量 II（保留 1.12.2 原版配置）。
 */
public class Originyer extends BladeDefineBase {
    public Originyer(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/originyer/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/originyer/tex.png"))
                        .effectColor(0x00FFFF)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(700)
                        .slashArtsType(ModSlashArts.DOMAIN_SUPPRESSION.getId())
                        .addSpecialEffect(ModSpecialEffects.ORIPURSUIT.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 5),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "originyer";
    }
}

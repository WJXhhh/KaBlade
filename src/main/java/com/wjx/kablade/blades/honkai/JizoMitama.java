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
 * 地藏御魂——崩坏线银河新星线延续，前置刀为影鵺。
 * 属性：攻击 31.0、耐久 750、默认妖化、自带亡灵杀手 III、击退 II、锋利 VII。
 */
public class JizoMitama extends BladeDefineBase {
    public JizoMitama(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/jizo_mitama/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/jizo_mitama/tex.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(31.0F)
                        .maxDamage(750)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.SOUL_APPEARANCE.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 6),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 7),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 3)
                )
        ));
    }

    @Override
    public String getKey() {
        return "jizo_mitama";
    }
}

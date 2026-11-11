package com.wjx.kablade.blades.honkai.claymore;

import com.wjx.kablade.init.ModSpecialEffects;
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
 * 黎明审判 (Judgment by Dawn)
 */
public class JudgmentByDawn extends BladeDefineBase {
    public JudgmentByDawn(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai_claymore/judgment_by_dawn/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai_claymore/judgment_by_dawn/tex.png"))
                        .effectColor(0xFFFF00)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(19.0F)
                        .maxDamage(570)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .addSpecialEffect(ModSpecialEffects.LIGHTNING_PUNISHMENT.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 3),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 3),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "thorns"), 1)
                )
        ));
    }

    @Override
    public String getKey() {
        return "judgment_by_dawn";
    }
}

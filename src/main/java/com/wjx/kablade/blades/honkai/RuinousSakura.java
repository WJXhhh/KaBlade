package com.wjx.kablade.blades.honkai;

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
 * 魂妖刀「血樱寂灭」——崩坏线高阶武器。
 * 赤染樱的升级形态，属性全面强化。
 *
 * <p>对比 赤染樱：attack 24→28, durability 800→1000, 附魔全面+1
 */
public class RuinousSakura extends BladeDefineBase {
    public RuinousSakura(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/ruinous_sakura/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/ruinous_sakura/mdl.png"))
                        .effectColor(0xF3002C)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(34.0F)
                        .maxDamage(1200)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.BLOODFYRE_FRENZY.getId())
                        .addSpecialEffect(ModSpecialEffects.FUEL_THE_RUIN.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "fire_aspect"), 5),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 6),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 4),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 5)
                )
        ));
    }

    @Override
    public String getKey() {
        return "ruinous_sakura";
    }
}

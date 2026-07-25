package com.wjx.kablade.blades.honkai;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.ModSlashArts;
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
 * Seven Thunders of Retribution: Narukami.
 *
 * <p>The blade carries Narukami Divinity and intentionally has no Special Effect.
 */
public class SevenThundersNarukami extends BladeDefineBase {
    public SevenThundersNarukami(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/str_narukami/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/str_narukami/tex.png"))
                        .effectColor(0xC15CFF)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(64.0F)
                        .maxDamage(1600)
                        .slashArtsType(ModSlashArts.NARUKAMI_DIVINITY.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 10),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 10),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 10),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 10)
                )
        ));
    }

    @Override
    public String getKey() {
        return "str_narukami";
    }
}

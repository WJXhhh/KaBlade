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

/** Mag Storm, ported from the 1.12.2 Honkai blade. */
public class MagStorm extends BladeDefineBase {
    public MagStorm(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/mag_storm/mdlMagStorm.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/mag_storm/texMagStorm.png"))
                        .effectColor(0x00FFFF)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(23.0F)
                        .maxDamage(1000)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.MAG_CHAOS_BLADE.getId())
                        .addSpecialEffect(ModSpecialEffects.EM_INDUCTION.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 5),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 3),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 4),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 4)
                )
        ));
    }

    @Override
    public String getKey() {
        return "mag_storm";
    }
}

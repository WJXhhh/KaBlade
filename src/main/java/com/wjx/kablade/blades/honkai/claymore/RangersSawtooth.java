package com.wjx.kablade.blades.honkai.claymore;

import com.wjx.kablade.blades.ModSlashArts;
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
 * 游骑兵锯齿 (Ranger's Sawtooth)
 */
public class RangersSawtooth extends BladeDefineBase {
    public RangersSawtooth(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai_claymore/rangers_sawtooth/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai_claymore/rangers_sawtooth/tex.png"))
                        .effectColor(0x8A2BE2)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(17.0F)
                        .maxDamage(580)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.FLAME_BLOCK.getId())
                        .addSpecialEffect(ModSpecialEffects.FLAME_THORNS.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 1),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 1)
                )
        ));
    }

    @Override
    public String getKey() {
        return "rangers_sawtooth";
    }
}

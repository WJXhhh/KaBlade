package com.wjx.kablade.blades.honkai.claymore;

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
 * 超重剑·冲锋 (Vanguard)
 */
public class Vanguard extends BladeDefineBase {
    public Vanguard(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai_claymore/super_heavy_sword/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai_claymore/super_heavy_sword/tex_2.png"))
                        .effectColor(0xFFBB22)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(11.0F)
                        .maxDamage(450)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.NUCLEAR_SHOCK.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 1),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 1)
                )
        ));
    }

    @Override
    public String getKey() {
        return "vanguard";
    }
}

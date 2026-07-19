package com.wjx.kablade.blades.honkai;

import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;

/**
 * 御灵刀「寒狱冰天」——崩坏线高阶武器。
 * 从 1.12.2 移植本体属性与模型贴图。
 *
 * <p>1.12.2 原值：baseAttack=26.0, amplifier=2.5, durability=1200, 妖刀,
 * StandbyRenderType=1(KATANA), SummonedSwordColor=3388211(0x33B333 翠绿)
 */
public class FrozenNaraka extends BladeDefineBase {
    public FrozenNaraka(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/frozen_naraka/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/frozen_naraka/tex.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .effectColor(0x00FFFF)  // 刀光与仿灵刀同为青色
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(32.0F)
                        .maxDamage(1200)
                        .slashArtsType(ModSlashArts.UTPALA_AURA.getId())
                        .defaultSwordType(java.util.List.of(SwordType.BEWITCHED))
                        .addSpecialEffect(ModSpecialEffects.GLACIAL_BANE.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 5),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 5)
                )
        ));
    }

    @Override
    public String getKey() {
        return "frozen_naraka";
    }
}

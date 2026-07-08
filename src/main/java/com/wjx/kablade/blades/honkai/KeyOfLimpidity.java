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
 * 澄凝之钥 —— 崩坏线高阶武器。
 * 从 1.12.2 移植本体属性与模型贴图。
 *
 * <p>1.12.2 原值：baseAttack=29.0, amplifier=2.5, durability=1200, 妖刀,
 * StandbyRenderType=1(KATANA), SummonedSwordColor=0xFFAAFF
 */
public class KeyOfLimpidity extends BladeDefineBase {
    public KeyOfLimpidity(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/key_of_limpidity/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/key_of_limpidity/tex.png"))
                        .effectColor(0xFFAAFF)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(29.0F)
                        .maxDamage(1200)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.SWORD_ENLIGHTENMENT.getId())
                        .addSpecialEffect(ModSpecialEffects.TRUE_SELF.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 5),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 5)
                )
        ));
    }

    @Override
    public String getKey() {
        return "key_of_limpidity";
    }
}

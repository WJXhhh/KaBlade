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
 * 妖精剑·希尔文 —— 崩坏线进阶武器，由反力场打刀11式 + 破晓者：塔尔瓦合成。
 * 从 1.12.2 移植而来。
 * <p>
 * 属性：攻击 18.0（基础）、耐久 720、默认妖化、自带锋利 II。
 * SA：风之结界（{@link com.wjx.kablade.slasharts.WindEnchantmentArts}）
 * SE：风之力（{@link com.wjx.kablade.specialeffect.PowerOfWind}）
 */
public class FairySword extends BladeDefineBase {
    public FairySword(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/fairy_sword/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/fairy_sword/tex.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .effectColor(16642509)  // 召唤剑颜色，1.12.2 原值
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(18.0F)
                        .maxDamage(720)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.WIND_ENCHANTMENT.getId())
                        .addSpecialEffect(ModSpecialEffects.POWER_OF_WIND.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "fairy_sword";
    }
}

package com.wjx.kablade.blades.honkai;

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
 * 藏锋 —— 崩坏线苗刀分支，由苗刀·雷妖 + 萤石 + 雷电结晶合成。
 * 从 1.12.2 移植而来，无 SA，自带特殊效果「乱流（Turbulence）」。
 * <p>
 * 属性：攻击 13.0、耐久 700、默认妖化、自带锋利 II、召唤剑颜色 0x536474。
 */
public class Osahoko extends BladeDefineBase {
    public Osahoko(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/osahoko/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/osahoko/tex.png"))
                        .effectColor(0x536474)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(13.0F)
                        .maxDamage(700)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .addSpecialEffect(ModSpecialEffects.TURBULENCE.getId())
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "osahoko";
    }
}

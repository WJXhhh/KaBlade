package com.wjx.kablade.blades.honkai;

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
 * 苗刀·雷妖——崩坏线村正系列 Lv4，由妖刀雨村 + 红石 + 铁块升级。
 * 从 1.12.2 移植而来，SA 为 SlashBlade 内置「樱花」（1.12.2 SA 2）。
 * <p>
 * 属性：攻击 12.0、耐久 500、默认妖化、自带锋利 I。
 */
public class ByoRai extends BladeDefineBase {
    public ByoRai(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/byoto/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/byoto/tex_rai.png"))
                        .effectColor(0x4A6FA5)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(12.0F)
                        .maxDamage(500)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ResourceLocation.fromNamespaceAndPath("slashblade", "sakura_end"))
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "byorai";
    }
}

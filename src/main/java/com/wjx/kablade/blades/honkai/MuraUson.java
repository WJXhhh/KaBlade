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
 * 妖刀雨村——崩坏线村正系列 Lv3，由堀川国广 + 铁刃「竹光」+ 铁块融合。
 * 从 1.12.2 移植而来。
 * <p>
 * 属性：攻击 8.0、耐久 380、默认妖化、自带锋利 I。
 */
public class MuraUson extends BladeDefineBase {
    public MuraUson(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/muramasa/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/muramasa/tex_uson.png"))
                        .effectColor(0x6B8E9E)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(380)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "murauson";
    }
}

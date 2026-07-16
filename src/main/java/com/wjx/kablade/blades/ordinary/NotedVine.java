package com.wjx.kablade.blades.ordinary;

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
 * 铭刀「青藤」——自然线 Lv1。由夯土刀 + 藤蔓 + 小麦种子升级而来。
 * 自带妖刀属性，使用独立模型与贴图，攻击力较高。
 */
public class NotedVine extends BladeDefineBase {
    public NotedVine(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/noted_vine/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/noted_vine/tex.png"))
                        .effectColor(0x2E7D32)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(150)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "noted_vine";
    }
}

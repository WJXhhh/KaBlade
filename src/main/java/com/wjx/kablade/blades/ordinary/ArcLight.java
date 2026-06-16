package com.wjx.kablade.blades.ordinary;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.ModSlashArts;
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
 * 弧光刃「流芒」—— 竹系列 Lv3。
 * 由流萤「竹光」+ 萤石粉 + 铁块在铁砧上锻合而成。
 * 从 1.12.2 移植而来，专属 SA「弧光破晓」。
 * <p>
 * 属性：攻击 7.0、耐久 400、默认妖化。
 * 自带亡灵杀手 III、力量 I（保留 1.12.2 原版配置）。
 */
public class ArcLight extends BladeDefineBase {
    public ArcLight(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/arc_light/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/arc_light/tex.png"))
                        .effectColor(0xFFE4B5)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(7.0F)
                        .maxDamage(400)
                        .slashArtsType(ModSlashArts.BREAK_THE_DAWN.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 3),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 1)
                )
        ));
    }

    @Override
    public String getKey() {
        return "arc_light";
    }
}

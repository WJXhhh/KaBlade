package com.wjx.kablade.blades.allweapon;

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
 * 光剑「监视者」—— 万物皆刃线招牌刀。
 * 从 1.12.2 移植，专属 SA「七彩斩」（{@code rainbow_slash}）：7 道彩虹色扇形飞刃 + 附魔台粒子。
 * <p>
 * 世界合成：雷击铁质刀架，光照≥10、精炼≥50、基础刀带摔落保护Ⅳ时变身。
 * <p>属性：攻击 10、耐久 230、默认妖化、飞刃淡蓝。附魔（1.12.2 越级原样）：无限 20。
 */
public class AwGuangJian extends BladeDefineBase {
    public AwGuangJian(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/guangjian/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/guangjian/tex.png"))
                        .effectColor(0xBBBBEE)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(10.0F)
                        .maxDamage(230)
                        .slashArtsType(ModSlashArts.RAINBOW_SLASH.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "infinity"), 20)
                )
        ));
    }

    @Override
    public String getKey() {
        return "guangjian";
    }
}

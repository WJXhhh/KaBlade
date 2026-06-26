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
 * 奉神刀「鹿」—— 万物皆刃线最强招牌刀。
 * 从 1.12.2 移植，专属 SA「追星」（{@code star_chase}）：锁定目标头顶召唤 32 把黄色索敌幻影剑雨。
 * <p>
 * 世界合成：玩家击打铁质刀架，上下三层黑曜石/信标/石英框/玻璃顶结构、精炼≥250 时变身。
 * <p>属性：攻击 14、耐久 250、默认妖化、飞刃金黄。附魔（1.12.2 越级原样）：无限 20、锋利 20、抢夺 20。
 */
public class AwFengShen extends BladeDefineBase {
    public AwFengShen(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/fengshen/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/fengshen/tex.png"))
                        .effectColor(0xBBBB1E)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(14.0F)
                        .maxDamage(250)
                        .slashArtsType(ModSlashArts.STAR_CHASE.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "infinity"), 20),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 20),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "looting"), 20)
                )
        ));
    }

    @Override
    public String getKey() {
        return "fengshen";
    }
}

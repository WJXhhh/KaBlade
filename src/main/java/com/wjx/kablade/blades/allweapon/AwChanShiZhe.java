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
 * 夜空之剑「阐释者」—— 万物皆刃线招牌刀。
 * 从 1.12.2 移植，专属 SA「星爆」（{@code star_burst}）：21 道红色飞刃随机角度爆裂扩散、加速。
 * <p>
 * 世界合成：玩家击打铁质刀架，刀架下方摆出 3×3 方块阵
 * （角=唱片机/睡莲/仙人掌/冰，芯=荧石；4 向旋转任一），精炼≥50、骄魂≥200 时变身。
 * <p>属性：攻击 10、耐久 230、默认妖化。附魔（1.12.2 越级原样）：击退 6、冲击 6、无限 20。
 */
public class AwChanShiZhe extends BladeDefineBase {
    public AwChanShiZhe(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/chanshizhe/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/chanshizhe/tex.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(10.0F)
                        .maxDamage(230)
                        .slashArtsType(ModSlashArts.STAR_BURST.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 6),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "punch"), 6),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "infinity"), 20)
                )
        ));
    }

    @Override
    public String getKey() {
        return "chanshizhe";
    }
}

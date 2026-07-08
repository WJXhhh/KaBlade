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
 * 白「天锁斩月」—— 万物皆刃线招牌刀。
 * 从 1.12.2 移植，专属 SA「月牙天冲」（{@code moon_fang}）：5 道逐渐变大的白色同心月牙波朝前推进。
 * <p>
 * 世界合成：玩家手持命名为「Moe_Meng」的命名牌击打铁质刀架，精炼≥50 时变身。
 * <p>属性：攻击 10、耐久 230、默认妖化、飞刃皓白。附魔（1.12.2 越级原样）：耐久 20。
 */
public class AwZhanYue extends BladeDefineBase {
    public AwZhanYue(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/allweapon/zhanyue/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/allweapon/zhanyue/tex.png"))
                        .effectColor(0xEEEEEF)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(10.0F)
                        .maxDamage(230)
                        .slashArtsType(ModSlashArts.MOON_FANG.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 20)
                )
        ));
    }

    @Override
    public String getKey() {
        return "zhanyue";
    }
}

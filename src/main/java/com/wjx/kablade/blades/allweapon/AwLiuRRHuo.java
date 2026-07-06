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
 * 炎王「流刃若火」—— 万物皆刃线招牌刀。
 * 从 1.12.2 移植，专属 SA「焰击」（{@code raging_fire}）：一道朝前的大型红色火焰飞刃。
 * <p>
 * 世界合成：下界、基础刀放在铁质刀架（bladestand_1）上并立于岩浆之上，
 * 以火焰伤害击打刀架，精炼≥50、基础刀带火焰保护Ⅳ时变身。
 * <p>属性：攻击 10、耐久 230、默认妖化、飞刃赤红。附魔（1.12.2 越级原样）：火焰附加 20、力量 20。
 */
public class AwLiuRRHuo extends BladeDefineBase {
    public AwLiuRRHuo(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/liurrh/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/liurrh/tex.png"))
                        .effectColor(0xFF0000)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(10.0F)
                        .maxDamage(230)
                        .slashArtsType(ModSlashArts.RAGING_FIRE.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "fire_aspect"), 20),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 20)
                )
        ));
    }

    @Override
    public String getKey() {
        return "liurrh";
    }
}

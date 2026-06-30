package com.wjx.kablade.blades.honkai;

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
 * 等离子影秀——崩坏线复合系列 Lv3，由结晶逆刃刀 + 脉冲T17 + 钼剑合成（1.12.2 原版）。
 * 从 1.12.2 移植而来，专属 SA「绝对零度」（1.12.2 SA 293）。
 * <p>
 * 属性：攻击 16.0、耐久 660、默认妖化、自带亡灵杀手 II + 锋利 II。
 * <p>
 * 注意：1.12.2 合成依赖脉冲T17和钼剑，这两个材料尚未迁移到 1.20.1，故暂无合成表。
 */
public class PlasmaKagehide extends BladeDefineBase {
    public PlasmaKagehide(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/raikiri_plas/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/raikiri_plas/tex_plasma_kagehide.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(16.0F)
                        .maxDamage(660)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.ABSOLUTE_ZERO.getId())
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 2),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "plasma_kagehide";
    }
}

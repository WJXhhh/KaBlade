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
 * 轩辕·脉冲太刀 —— 崩坏线脉冲太刀分支，由脉冲太刀17式 + 铬锭 + 雪合成。
 * 从 1.12.2 移植而来，专属 SA「寒霜灵刃」（1.12.2 SA 285）。
 * <p>
 * 属性：攻击 14.0、耐久 600、默认妖化、自带击退 II、召唤剑颜色 0x00FFEE。
 */
public class XuanYuanKatana extends BladeDefineBase {
    public XuanYuanKatana(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/xuanyuan_katana/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/xuanyuan_katana/tex.png"))
                        .effectColor(0x00FFEE)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(14.0F)
                        .maxDamage(600)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.FROST_BLADE.getId())
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 5),
                        new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "xuanyuan_katana";
    }
}

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
 * 复合刀·柳叶——崩坏线复合系列 Lv1，由势州村正 + 钻石块 + 树叶合成。
 * 从 1.12.2 移植而来，专属 SA「锋刀抚柳」（1.12.2 SA 284）。
 * <p>
 * 属性：攻击 8.0、耐久 360、默认妖化、自带锋利 I。
 */
public class FuheLiuye extends BladeDefineBase {
    public FuheLiuye(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/fuhe/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/fuhe/tex_liuye.png"))
                        .effectColor(0x6B8E23)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(360)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.CHOP_WILLOW.getId())
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "fuheliuye";
    }
}

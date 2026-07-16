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
 * 复合刀·朱雀——崩坏线复合系列 Lv1（平行分支），由势州村正 + 金块 + 烈焰棒合成。
 * 从 1.12.2 移植而来，专属 SA「罪业之火」（1.12.2 SA 283）。
 * <p>
 * 属性：攻击 8.0、耐久 360、默认妖化、自带横扫之刃 II。
 */
public class FuheZhuque extends BladeDefineBase {
    public FuheZhuque(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/fuhe/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/fuhe/tex_zhuque.png"))
                        .effectColor(0xFF4500)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(360)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.FIRE_OF_SIN.getId())
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 2),
                        new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sweeping"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "fuhezhuque";
    }
}

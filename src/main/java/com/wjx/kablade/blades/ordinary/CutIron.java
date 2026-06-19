package com.wjx.kablade.blades.ordinary;

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
 * 复合刃「斩铁」——由战刃「竹光」+ 铬锭 + 钻石在工作台中合成。
 * 从 1.12.2 移植而来，SA 类型 290。
 * <p>
 * 属性：攻击 8.0、耐久 400、默认妖化。
 * 自带锋利 IV、力量 I（保留 1.12.2 原版配置）。
 */
public class CutIron extends BladeDefineBase {
    public CutIron(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/cut_iron/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/cut_iron/tex.png"))
                        .effectColor(0xAAAAAA)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(400)
                        .slashArtsType(ModSlashArts.CUT_METAL.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 1)
                )
        ));
    }

    @Override
    public String getKey() {
        return "cut_iron";
    }
}

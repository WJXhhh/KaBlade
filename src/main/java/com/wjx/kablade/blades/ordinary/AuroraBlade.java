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
 * 极光刃「映天」—— 极光线终极刀。
 * 由弧光刃「流芒」+ 钻石 + 极光金属剑在工作台中合成。
 * 从 1.12.2 移植而来，专属 SA「极光闪耀」。
 * <p>
 * 属性：攻击 9.0、耐久 500、默认妖化。
 * 自带亡灵杀手 IV、力量 II（保留 1.12.2 原版配置）。
 */
public class AuroraBlade extends BladeDefineBase {
    public AuroraBlade(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/aurora_blade/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/aurora_blade/tex.png"))
                        .effectColor(0x00FF7F)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(9.0F)
                        .maxDamage(500)
                        .slashArtsType(ModSlashArts.AURORA_SHINING.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 4),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 2)
                )
        ));
    }

    @Override
    public String getKey() {
        return "aurora_blade";
    }
}

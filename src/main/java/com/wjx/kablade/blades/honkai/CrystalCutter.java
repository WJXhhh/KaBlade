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
 * 结晶逆刃刀——崩坏线复合系列 Lv2，由复合刀·柳叶 + 钻石块合成。
 * 从 1.12.2 移植而来，专属 SA「霜冻彗星」（1.12.2 SA 287）。
 * <p>
 * 属性：攻击 11.0、耐久 500、默认妖化、自带亡灵杀手 II。
 */
public class CrystalCutter extends BladeDefineBase {
    public CrystalCutter(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai/rhomphaia/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai/rhomphaia/tex_cry.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(11.0F)
                        .maxDamage(500)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.FROST_COMET.getId())
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "crystal_cutter";
    }
}

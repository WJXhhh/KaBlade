package com.wjx.kablade.blades.ordinary;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.ModSlashArts;
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
 * 千岩之锋 —— 岩石线 Lv2。
 * 由自铭「矍铄」+ 自铭「嶙峋」+ 自铭「明磬」+ 铁块三合一升级而来。
 * 自带妖刀、锋利 II、耐久 II，专属 SA「岩石撼击」。
 */
public class RockyEX extends BladeDefineBase {
    public RockyEX(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ORDINARY),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/rocky_ex/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/rocky_ex/tex.png"))
                        .effectColor(0x8B7355)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(400)
                        .slashArtsType(ModSlashArts.ROCK_STRIKE.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 2),
                        new EnchantmentDefinition(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 3)
                )
        ));
    }

    @Override
    public String getKey() {
        return "rocky_ex";
    }
}

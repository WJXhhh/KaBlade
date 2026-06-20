package com.wjx.kablade.blades.honkai;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;

import java.util.List;

/**
 * 堀川国广——崩坏线村正系列 Lv2，由势州村正 + 红石块升级。
 * 从 1.12.2 移植而来。
 * <p>
 * 属性：攻击 7.0、耐久 330、非妖化、无 SA。
 */
public class MuraHori extends BladeDefineBase {
    public MuraHori(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/muramasa/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/muramasa/tex_hori.png"))
                        .effectColor(0xB97A6B)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(7.0F)
                        .maxDamage(330)
                        .build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "murahori";
    }
}

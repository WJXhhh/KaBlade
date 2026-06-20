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
 * 势州村正——崩坏线村正系列 Lv1 入口刀。
 * 从 1.12.2 移植而来，由钻石剑 + 铁块合成。
 * <p>
 * 属性：攻击 6.0、耐久 260、非妖化、无 SA。
 */
public class MuraSeshu extends BladeDefineBase {
    public MuraSeshu(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/muramasa/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/muramasa/tex_seshu.png"))
                        .effectColor(0x9E9E9E)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(260)
                        .build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "muraseshu";
    }
}

package com.wjx.kablade.blades.allweapon;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;

import java.util.List;

/**
 * AllWeapon: Lvluo.
 * 1.12.2 SA id 401 was commented out, so this blade intentionally has no slash art.
 */
public class AwLvluo extends BladeDefineBase {
    public AwLvluo(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/allweapon/lvluo/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/allweapon/lvluo/tex.png"))
                        .effectColor(0x33ee33)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(6.0F)
                        .maxDamage(200)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "lvluo";
    }
}

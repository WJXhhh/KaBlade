package com.wjx.kablade.blades.allweapon;

import com.wjx.kablade.blades.ModSlashArts;
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
 * AllWeapon: Bailan.
 * 1.12.2 SA id 412 -> AL_HuanyingdieS / PHANTOM_BUTTERFLY_S.
 * The old blade only declared a texture, so this definition keeps the default model.
 */
public class AwBailan extends BladeDefineBase {
    public AwBailan(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .textureName(ResourceUtil.getLocation("model/allweapon/bailan/tex.png"))
                        .effectColor(0x33ee33)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(5.0F)
                        .maxDamage(200)
                        .slashArtsType(ModSlashArts.PHANTOM_BUTTERFLY_S.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "bailan";
    }
}

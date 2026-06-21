package com.wjx.kablade.blades.splight;

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
 * 龙一文字线起点：龙一「空」。
 * <p>
 * 从 1.12.2 {@code SL_Initial} 移植而来，无 SA，使用默认散华模型与 SP Light 常规贴图。
 * 属性：攻击 7.0、耐久 1500、默认妖化。
 */
public class SL_Initial extends BladeDefineBase {
    public SL_Initial(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.SP_LIGHT),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceLocation.fromNamespaceAndPath("slashblade", "model/named/sange/sange.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/splight/splight/normal/texture.png"))
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(7.0F)
                        .maxDamage(1500)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "looting"), 1),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 2),
                        new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 1)
                )
        ));
    }

    @Override
    public String getKey() {
        return "splight_initial";
    }
}

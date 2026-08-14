package com.wjx.kablade.blades.honkai.claymore;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

/**
 * 大剑——崩坏线大剑系列 Lv1 入口刀。
 * <p>
 * 属性：攻击 8.0、耐久 300、非妖化、无 SA。
 */
public class GreatSword extends BladeDefineBase {
    public GreatSword(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/honkai_claymore/greatsword/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/honkai_claymore/greatsword/tex.png"))
                        .effectColor(0xDDDDEE)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(8.0F)
                        .maxDamage(300)
                        .build(),
                List.of(new EnchantmentDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "greatsword";
    }
}

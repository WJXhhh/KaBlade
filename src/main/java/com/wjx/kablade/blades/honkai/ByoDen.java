package com.wjx.kablade.blades.honkai;

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
 * 苗刀·电魂——崩坏线村正系列 Lv4（平行分支），由妖刀村正 + 红石 + 铁块升级。
 * 从 1.12.2 移植而来，无 SA，自带亡灵杀手 II。
 * <p>
 * 属性：攻击 12.0、耐久 500、默认妖化。
 */
public class ByoDen extends BladeDefineBase {
    public ByoDen(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/byoto/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/byoto/tex_den.png"))
                        .effectColor(0x8B7355)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(12.0F)
                        .maxDamage(500)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 2))
        ));
    }

    @Override
    public String getKey() {
        return "byoden";
    }
}

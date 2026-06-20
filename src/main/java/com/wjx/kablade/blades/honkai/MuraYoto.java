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
 * 妖刀村正——崩坏线村正系列 Lv3（平行分支），由势州村正 + 自铭「嶙峋」+ 钻石块融合。
 * 从 1.12.2 移植而来，专属 SA「撕裂灵刃」（1.12.2 SA 281）。
 * <p>
 * 属性：攻击 9.0、耐久 400、默认妖化、自带锋利 I。
 */
public class MuraYoto extends BladeDefineBase {
    public MuraYoto(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.HONKAI),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/muramasa/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/muramasa/tex_yoto.png"))
                        .effectColor(0x8B0000)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(9.0F)
                        .maxDamage(400)
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .slashArtsType(ModSlashArts.LACERATE_BLADE.getId())
                        .build(),
                List.of(new EnchantmentDefinition(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 1))
        ));
    }

    @Override
    public String getKey() {
        return "murayoto";
    }
}

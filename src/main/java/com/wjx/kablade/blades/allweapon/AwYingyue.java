package com.wjx.kablade.blades.allweapon;

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
 * 万物皆刃系列：映月
 * 从 1.12.2 移植，专属斩击：{@link ModSlashArts#RANDOM_SA}
 * <p>
 * 1.12.2 原版 SA 为随机SA（AL_RandomSA），每次拔刀从所有已注册 SA 中随机选一个执行。
 * 另外此刀无世界合成配方，获取方式待定。
 */
public class AwYingyue extends BladeDefineBase {
    public AwYingyue(BootstapContext<SlashBladeDefinition> context) {
        String key = getKey();
        context.register(createBladeKey(key), new SlashBladeDefinition(
                getBaseBladeId(BaseBladeType.ALL_WEAPON),
                ResourceUtil.getLocation(key),
                RenderDefinition.Builder.newInstance()
                        .modelName(ResourceUtil.getLocation("model/named/yingyue/mdl.obj"))
                        .textureName(ResourceUtil.getLocation("model/named/yingyue/tex.png"))
                        .effectColor(0x000000)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(10.0F)
                        .maxDamage(230)
                        .slashArtsType(ModSlashArts.RANDOM_SA.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of()
        ));
    }

    @Override
    public String getKey() {
        return "yingyue";
    }
}

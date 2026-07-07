package com.wjx.kablade.property;

import com.wjx.kablade.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 玩家属性（HUD buff）注册入口。
 * <p>
 * 所有 {@link PlayerProperty} 在此集中注册。由 {@link Main#commonSetup(FMLCommonSetupEvent)} 调用。
 */
public final class KabladeBladeProperties {

    private KabladeBladeProperties() {
    }

    /** 供 {@link Main#commonSetup(FMLCommonSetupEvent)} 调用，注册所有属性条目。 */
    public static void register() {
        // 风之力（PowerOfWind SE）：玩家手持妖精剑·希尔文时激活
        PlayerPropertyRegistry.register(PlayerProperty.builder("fair_pow")
                .displayName(Component.translatable("prop.kablade.fair_pow"))
                .capabilityKey("fair_pow")
                .maxValue(1)
                .build());

        // 风之结界（WindEnchantmentEntity AO E光环）：靠近结界光环时激活
        PlayerPropertyRegistry.register(PlayerProperty.builder("wind_enchantment_boost")
                .displayName(Component.translatable("prop.kablade.wind_enchantment_boost"))
                .capabilityKey("wind_enchantment_boost")
                .maxValue(5)
                .build());

        // 聚光舞台（StageLightEntity 范围光环）：攻击伤害 +10%、攻击速度 +10%
        PlayerPropertyRegistry.register(PlayerProperty.builder("stage_light")
                .displayName(Component.translatable("prop.kablade.stage_light").withStyle(ChatFormatting.YELLOW))
                .capabilityKey("stage_light")
                .maxValue(10)
                .build());

        // 圣能冲击（HolyEnergyImpact SE）：评级越高伤害越高，HUD 始终全黄显示
        PlayerPropertyRegistry.register(PlayerProperty.builder("holy_energy_impact")
                .displayName(Component.translatable("prop.kablade.holy_energy_impact").withStyle(ChatFormatting.YELLOW))
                .capabilityKey("holy_energy_impact")
                .maxValue(1)
                .build());

        // 雷切护盾（Raikiri Blade Shield）：护盾剩余耐久（动态计算）
        PlayerPropertyRegistry.register(PlayerProperty.builder("raikiri_shield_blood")
                .displayName(Component.translatable("prop.kablade.raikiri_shield_blood").withStyle(ChatFormatting.AQUA))
                .capabilityKey("raikiri_shield_blood")
                .maxValue(Integer.MAX_VALUE)
                .build());

        // 乱流（Turbulence SE — 藏锋专属）：被攻击后 100 tick 内反击触发雷电 + 麻痹
        PlayerPropertyRegistry.register(PlayerProperty.builder("turbulence")
                .displayName(Component.translatable("prop.kablade.turbulence").withStyle(ChatFormatting.DARK_PURPLE))
                .capabilityKey("turbulence")
                .maxValue(100)
                .build());

        PlayerPropertyRegistry.register(PlayerProperty.builder("glacial_bane_extra_tick")
                .displayName(Component.translatable("prop.kablade.glacial_bane_extra_tick").withStyle(ChatFormatting.AQUA))
                .capabilityKey("glacial_bane_extra_tick")
                .maxValue(120)
                .build());

        PlayerPropertyRegistry.register(PlayerProperty.builder("kami_of_war_count")
                .displayName(Component.translatable("prop.kablade.kami_of_war_count").withStyle(ChatFormatting.YELLOW))
                .capabilityKey("kami_of_war_count")
                .maxValue(6)
                .build());

        // 真我（TrueSelf SE — 澄凝之钥）：预知叠层（上限 3），每层 +10% 伤害
        PlayerPropertyRegistry.register(PlayerProperty.builder("foresight")
                .displayName(Component.translatable("prop.kablade.foresight").withStyle(ChatFormatting.LIGHT_PURPLE))
                .capabilityKey("foresight")
                .maxValue(3)
                .build());
    }
}

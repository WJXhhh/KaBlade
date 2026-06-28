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
    }
}

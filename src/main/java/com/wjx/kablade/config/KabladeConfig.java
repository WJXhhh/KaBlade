package com.wjx.kablade.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Kablade 的通用配置（生成于 {@code config/kablade-common.toml}，启动游戏前即可编辑）。
 *
 * <p>只有两个全局倍率：对<b>所有</b>拔刀剑的基础攻击与最大耐久统一缩放，默认都是 1.0（不改变原属性）。
 * 倍率在刀被创建的那一刻应用（见 {@code BladeAttributeOverride}），所以无需为每把刀单独配置。
 *
 * <p>例：夯土刀定义里攻击为 2.0，把 {@code attack_multiplier} 调成 2.0，启动后新造的夯土刀攻击即为 4.0。
 */
public final class KabladeConfig {

    public static final ForgeConfigSpec SPEC;

    /** 攻击力倍率：成品刀的基础攻击 = 定义值 × 本倍率。默认 1.0。 */
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;

    /** 耐久倍率：成品刀的最大耐久（max_damage）= 定义值 × 本倍率。默认 1.0。 */
    public static final ForgeConfigSpec.DoubleValue DURABILITY_MULTIPLIER;

    /** When true, Kablade SA target selectors ignore players entirely. */
    public static final ForgeConfigSpec.BooleanValue FILTER_PLAYERS_IN_SA_TARGETING;

    /** When true, Kablade SA/SE damage skips protected tamed pets. */
    public static final ForgeConfigSpec.BooleanValue PROTECT_TAMED_PETS_IN_SA_TARGETING;

    public static final ForgeConfigSpec.BooleanValue SA_ALL_USE_TARGET_SELECTOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                        "对所有拔刀剑生效的全局属性倍率（默认 1.0 = 不变）。",
                        "改完后启动游戏，之后新创建的刀会按倍率缩放；存档里已存在的刀不受影响。")
                .push("blade_multiplier");

        ATTACK_MULTIPLIER = builder
                .comment("攻击力倍率：例如夯土刀默认攻击 2.0，倍率设 2.0 → 实际 4.0。")
                .defineInRange("attack_multiplier", 1.0D, 0.0D, 1024.0D);

        DURABILITY_MULTIPLIER = builder
                .comment("耐久倍率：作用于刀的最大耐久（max_damage）。")
                .defineInRange("durability_multiplier", 1.0D, 0.0D, 1024.0D);

        builder.pop();
        builder.comment("Slash Art targeting behavior.")
                .push("slash_art_targeting");

        FILTER_PLAYERS_IN_SA_TARGETING = builder
                .comment("When true, Kablade slash art target selectors filter out players regardless of team.")
                .define("filter_players", true);

        PROTECT_TAMED_PETS_IN_SA_TARGETING = builder
                .comment("When true, Kablade slash art and special effect damage will not hit the user's own tamed pets; allied owners' pets also follow scoreboard friendly-fire rules.")
                .define("protect_tamed_pets", true);

        SA_ALL_USE_TARGET_SELECTOR = builder.comment("When true,SaTargeting will all use SlashBlade:Resharped TargetSelector.").define("sa_all_use_targets", false);

        builder.pop();
        SPEC = builder.build();
    }

    private KabladeConfig() {
    }
}

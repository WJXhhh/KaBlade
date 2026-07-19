package com.wjx.kablade.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class KabladeClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<SkillShaderMode> SKILL_SHADER_MODE;
    public static final ForgeConfigSpec.EnumValue<RaidenCycloneQuality> RAIDEN_CYCLONE_QUALITY;
    public static final ForgeConfigSpec.BooleanValue RAIDEN_CYCLONE_CAMERA_SHAKE;
    public static final ForgeConfigSpec.BooleanValue RAIDEN_CYCLONE_REDUCED_FLASH;
    public static final ForgeConfigSpec.EnumValue<RaizanCleaveQuality> RAIZAN_CLEAVE_QUALITY;
    public static final ForgeConfigSpec.BooleanValue RAIZAN_CLEAVE_CAMERA_SHAKE;
    public static final ForgeConfigSpec.BooleanValue RAIZAN_CLEAVE_REDUCED_FLASH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Client-side rendering options for KBlade skill effects.")
                .push("skill_shader");

        SKILL_SHADER_MODE = builder
                .comment(
                        "AUTO keeps the vanilla custom ShaderInstance path unless Oculus/Iris with an active shader pack is detected.",
                        "FORCE_VANILLA_CUSTOM always uses the current custom ShaderInstance render path.",
                        "FORCE_OCULUS_POST forces the Oculus-compatible post-processing path when its framebuffer hooks are available.")
                .defineEnum("mode", SkillShaderMode.AUTO);

        builder.pop();

        builder.comment("Raiden's Cyclone client effect options.")
                .push("raiden_cyclone");
        RAIDEN_CYCLONE_QUALITY = builder
                .comment("HIGH is the reference-quality presentation.")
                .defineEnum("quality", RaidenCycloneQuality.HIGH);
        RAIDEN_CYCLONE_CAMERA_SHAKE = builder
                .comment("Enable short local camera impulses on Raiden's Cyclone hits.")
                .define("camera_shake", true);
        RAIDEN_CYCLONE_REDUCED_FLASH = builder
                .comment("Reduce full-screen white flashes without removing world-space effect layers.")
                .define("reduced_flash", false);
        builder.pop();

        builder.comment("Raizan Cleave client effect options.")
                .push("raizan_cleave");
        RAIZAN_CLEAVE_QUALITY = builder
                .comment("HIGH keeps the complete reference layer and particle counts.")
                .defineEnum("quality", RaizanCleaveQuality.HIGH);
        RAIZAN_CLEAVE_CAMERA_SHAKE = builder
                .comment("Enable short local camera impulses on Raizan Cleave hit frames.")
                .define("camera_shake", true);
        RAIZAN_CLEAVE_REDUCED_FLASH = builder
                .comment("Reduce screen-space flashes while retaining all world-space effects.")
                .define("reduced_flash", false);
        builder.pop();
        SPEC = builder.build();
    }

    private KabladeClientConfig() {
    }

    public enum SkillShaderMode {
        AUTO,
        FORCE_VANILLA_CUSTOM,
        FORCE_OCULUS_POST
    }

    public enum RaidenCycloneQuality {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum RaizanCleaveQuality {
        LOW,
        MEDIUM,
        HIGH
    }
}

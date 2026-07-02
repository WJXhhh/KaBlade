package com.wjx.kablade.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class KabladeClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<SkillShaderMode> SKILL_SHADER_MODE;

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
        SPEC = builder.build();
    }

    private KabladeClientConfig() {
    }

    public enum SkillShaderMode {
        AUTO,
        FORCE_VANILLA_CUSTOM,
        FORCE_OCULUS_POST
    }
}

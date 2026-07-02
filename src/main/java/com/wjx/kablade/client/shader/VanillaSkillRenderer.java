package com.wjx.kablade.client.shader;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class VanillaSkillRenderer {
    private VanillaSkillRenderer() {
    }

    public static boolean isActive() {
        return !ShaderCompat.shouldUseOculusPostPath();
    }
}

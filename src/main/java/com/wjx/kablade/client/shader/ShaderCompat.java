package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.wjx.kablade.config.KabladeClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class ShaderCompat {
    private static final long SHADER_PACK_CACHE_NANOS = 250_000_000L;
    private static boolean cachedShaderPackInUse;
    private static long shaderPackCacheExpiresAt;

    private ShaderCompat() {
    }

    public static boolean shouldUseOculusPostPath() {
        KabladeClientConfig.SkillShaderMode mode = KabladeClientConfig.SKILL_SHADER_MODE.get();
        if (mode == KabladeClientConfig.SkillShaderMode.FORCE_VANILLA_CUSTOM) {
            return false;
        }
        if (mode == KabladeClientConfig.SkillShaderMode.FORCE_OCULUS_POST) {
            return isOculusLikeModLoaded();
        }
        return isOculusLikeModLoaded() && isShaderPackInUse();
    }

    public static SkillShaderTarget currentTarget() {
        if (shouldUseOculusPostPath()) {
            Optional<SkillShaderTarget> target = OculusFramebufferAccess.findTranslucentTarget();
            if (target.isPresent() && target.get().isComplete()) {
                return target.get();
            }
        }

        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        return new SkillShaderTarget(
                main.frameBufferId,
                main.getColorTextureId(),
                main.getDepthTextureId(),
                main.width,
                main.height,
                false);
    }

    public static boolean isOculusLikeModLoaded() {
        ModList mods = ModList.get();
        return mods.isLoaded("oculus") || mods.isLoaded("iris");
    }

    public static boolean isShaderPackInUse() {
        long now = System.nanoTime();
        if (now < shaderPackCacheExpiresAt) {
            return cachedShaderPackInUse;
        }

        Optional<Boolean> oculusConfigEnabled = readOculusConfigShaderState();
        if (oculusConfigEnabled.isPresent() && !oculusConfigEnabled.get()) {
            cacheShaderPackState(false, now);
            return false;
        }

        boolean enabled = readIrisApiShaderPackState()
                .orElseGet(ShaderCompat::readLegacyShaderPackState);
        cacheShaderPackState(enabled, now);
        return enabled;
    }

    public static void invalidateShaderPackCache() {
        shaderPackCacheExpiresAt = 0L;
    }

    private static void cacheShaderPackState(boolean enabled, long now) {
        cachedShaderPackInUse = enabled;
        shaderPackCacheExpiresAt = now + SHADER_PACK_CACHE_NANOS;
    }

    private static Optional<Boolean> readIrisApiShaderPackState() {
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            Object result = irisApi.getMethod("isShaderPackInUse").invoke(instance);
            if (result instanceof Boolean enabled) {
                return Optional.of(enabled);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }
        return Optional.empty();
    }

    private static Optional<Boolean> readOculusConfigShaderState() {
        try {
            Path config = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config")
                    .resolve("oculus.properties");
            if (!Files.isRegularFile(config)) {
                return Optional.empty();
            }

            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(config)) {
                properties.load(reader);
            }
            String enabled = properties.getProperty("enableShaders");
            if (enabled != null) {
                return Optional.of(Boolean.parseBoolean(enabled));
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return Optional.empty();
    }

    private static boolean readLegacyShaderPackState() {
        try {
            Class<?> iris = Class.forName("net.coderbot.iris.Iris");
            Object result = iris.getMethod("isPackInUseQuick").invoke(null);
            return result instanceof Boolean enabled && enabled;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}

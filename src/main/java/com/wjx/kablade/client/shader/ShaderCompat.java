package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.wjx.kablade.config.KabladeClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class ShaderCompat {
    private static final long SHADER_PACK_CACHE_NANOS = 250_000_000L;
    private static boolean cachedShaderPackInUse;
    private static long shaderPackCacheExpiresAt;
    private static volatile boolean handRendererAccessResolved;
    private static Object handRendererInstance;
    private static Method handRendererIsActive;
    private static volatile boolean shadowRendererAccessResolved;
    private static Field shadowRendererActive;

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

    /**
     * True while Oculus/Iris is replaying the first-person item into its dedicated hand pass.
     *
     * <p>There is no public Iris API for this short-lived rendering state. Reflection keeps
     * Oculus optional and lets callers select a hand-compatible stock shader only for the
     * affected draw, without changing third-person or item-entity rendering.</p>
     */
    public static boolean isRenderingShaderPackHand() {
        if (!isOculusLikeModLoaded() || !isShaderPackInUse()) {
            return false;
        }

        resolveHandRendererAccess();
        if (handRendererInstance == null || handRendererIsActive == null) {
            return false;
        }

        try {
            Object active = handRendererIsActive.invoke(handRendererInstance);
            return active instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /** True while Oculus/Iris is rendering the shadow map instead of the visible scene. */
    public static boolean isRenderingShaderPackShadow() {
        if (!isOculusLikeModLoaded() || !isShaderPackInUse()) {
            return false;
        }

        resolveShadowRendererAccess();
        if (shadowRendererActive == null) {
            return false;
        }

        try {
            return shadowRendererActive.getBoolean(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
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

    private static void resolveHandRendererAccess() {
        if (handRendererAccessResolved) {
            return;
        }

        synchronized (ShaderCompat.class) {
            if (handRendererAccessResolved) {
                return;
            }

            for (String className : new String[]{
                    "net.irisshaders.iris.pathways.HandRenderer",
                    "net.coderbot.iris.pathways.HandRenderer"
            }) {
                try {
                    Class<?> handRendererClass = Class.forName(className);
                    Field instanceField = handRendererClass.getField("INSTANCE");
                    Method isActiveMethod = handRendererClass.getMethod("isActive");
                    handRendererInstance = instanceField.get(null);
                    handRendererIsActive = isActiveMethod;
                    break;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
            handRendererAccessResolved = true;
        }
    }

    private static void resolveShadowRendererAccess() {
        if (shadowRendererAccessResolved) {
            return;
        }

        synchronized (ShaderCompat.class) {
            if (shadowRendererAccessResolved) {
                return;
            }

            for (String className : new String[]{
                    "net.irisshaders.iris.shadows.ShadowRenderer",
                    "net.coderbot.iris.shadows.ShadowRenderer"
            }) {
                try {
                    Class<?> shadowRendererClass = Class.forName(className);
                    shadowRendererActive = shadowRendererClass.getField("ACTIVE");
                    break;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                }
            }
            shadowRendererAccessResolved = true;
        }
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

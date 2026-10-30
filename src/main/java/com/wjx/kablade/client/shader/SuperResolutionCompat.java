package com.wjx.kablade.client.shader;

import com.wjx.kablade.Main;
import net.minecraftforge.fml.ModList;
import org.joml.Vector2f;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/** Optional SR v3 bridge. No SR classes are linked from the normal client/server paths. */
final class SuperResolutionCompat {
    private static final String ROOT = "io.homo.superresolution.";
    private static boolean resolved;
    private static Access access;
    private static boolean loggedFailure;

    private SuperResolutionCompat() {
    }

    static SkillShaderTarget adapt(SkillShaderTarget target) {
        if (!target.shaderPackTarget() || !ModList.get().isLoaded("super_resolution")) return target;
        try {
            if (!resolved) {
                resolved = true;
                access = new Access();
            }
            if (access == null) return target;
            return target.withLayout(access.layout(target.width(), target.height()));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            if (!loggedFailure) {
                loggedFailure = true;
                Main.LOGGER.warn("KBlade could not read the optional Super Resolution v3 render state; "
                        + "retaining the ordinary shader target mapping.", ex);
            }
            return target;
        }
    }

    static final class Access {
        private final Class<?> api;
        private final Class<?> config;
        private final Class<?> iris;
        private final Class<?> dispatcher;
        private final Method adaptJitter;

        private Access() throws ReflectiveOperationException {
            api = Class.forName(ROOT + "api.SuperResolutionAPI");
            config = Class.forName(ROOT + "common.config.SuperResolutionConfig");
            iris = Class.forName(ROOT + "shadercompat.IrisShaderCompatUtils");
            dispatcher = Class.forName(ROOT + "shadercompat.IrisShaderCompatUpscaleDispatcher");
            // Use the processor's conversion, not a duplicated list of algorithms/Y flips.
            Class<?> processor = Class.forName(ROOT + "common.minecraft.handler.shadercompat.SRCompatProcessor");
            adaptJitter = processor.getMethod("adaptJitterForShaderpack", Vector2f.class,
                    Class.forName(ROOT + "api.AbstractAlgorithm"),
                    Class.forName(ROOT + "common.minecraft.handler.shadercompat.SRShaderCompatData"),
                    Class.forName(ROOT + "api.registry.AlgorithmDescription"));
        }

        // Injectable class boundary lets standalone checks exercise mode/config/reload handling
        // without initializing either Minecraft or SR's native algorithm backend.
        Access(Class<?> api, Class<?> config, Class<?> iris, Class<?> dispatcher, Method adaptJitter) {
            this.api = api;
            this.config = config;
            this.iris = iris;
            this.dispatcher = dispatcher;
            this.adaptJitter = adaptJitter;
        }

        SkillRenderLayout layout(int textureWidth, int textureHeight) throws ReflectiveOperationException {
            SkillRenderLayout full = SkillRenderLayout.full(textureWidth, textureHeight);
            if (!Boolean.TRUE.equals(call(config, "isEnableUpscale"))
                    || !Boolean.TRUE.equals(call(iris, "shouldApplySuperResolutionChanges"))) return full;
            Object description = call(api, "getCurrentAlgorithmDescription");
            Object algorithm = call(api, "getCurrentAlgorithm");
            // None is the frame-generation-only path: SR_SHOULD_APPLY_{SCALE,JITTER}=0.
            if (description == null || algorithm == null
                    || "none".equals(call(description, "getCodeName"))) return full;
            Object pack = ((Optional<?>) call(iris, "getCurrentShaderPackConfig")).orElse(null);
            Object profile = ((Optional<?>) call(iris, "getCurrentConfig")).orElse(null);
            if (pack == null || profile == null || !Integer.valueOf(3).equals(field(pack, "version"))) return full;
            Object upscale = field(profile, "upscale");
            if (!Boolean.TRUE.equals(field(profile, "enabled"))
                    || !Boolean.TRUE.equals(field(upscale, "enabled"))) return full;

            // The v3 input regions explicitly declare the packed scene convention. Do not
            // assume every installed SR shaderpack uses a lower-left region or resize textures.
            Map<?, ?> inputs = (Map<?, ?>) field(upscale, "inputTextures");
            if (!renderRegion(inputs.get("color")) || !renderRegion(inputs.get("depth"))) return full;
            int renderWidth = ((Number) call(api, "getRenderWidth")).intValue();
            int renderHeight = ((Number) call(api, "getRenderHeight")).intValue();
            int screenWidth = ((Number) call(api, "getScreenWidth")).intValue();
            int screenHeight = ((Number) call(api, "getScreenHeight")).intValue();
            // Accept both full-size packed allocations and physically resized scene targets.
            if (!((textureWidth == screenWidth && textureHeight == screenHeight)
                    || (textureWidth == renderWidth && textureHeight == renderHeight))
                    || renderWidth > textureWidth || renderHeight > textureHeight) return full;

            Vector2f jitter = new Vector2f();
            Object jitterConfig = field(profile, "jitter");
            if (Boolean.TRUE.equals(field(jitterConfig, "enabled"))
                    && Boolean.TRUE.equals(call(description, "isSupportJitter"))) {
                if (!"MOD".equals(field(jitterConfig, "source").toString())) {
                    throw new IllegalStateException("SR shaderpack-owned jitter has no verified pixel convention");
                }
                // This is exactly the SRJitterOffset supplier in SRCompatV3Processor.
                // Copy first: processors may mutate the vector while adapting it.
                Vector2f raw = new Vector2f((Vector2f) call(dispatcher, "getJitterOffset"));
                jitter.set((Vector2f) adaptJitter.invoke(call(pack, "getProcessor"),
                        raw, algorithm, pack, description));
            }
            return new SkillRenderLayout(textureWidth, textureHeight, 0, 0,
                    renderWidth, renderHeight, jitter.x, jitter.y);
        }
    }

    private static boolean renderRegion(Object input) throws ReflectiveOperationException {
        if (input == null || !Boolean.TRUE.equals(field(input, "enabled"))) return false;
        Object region = field(input, "region");
        return Integer.valueOf(0).equals(call(region, "getX"))
                && Integer.valueOf(0).equals(call(region, "getY"))
                && Integer.valueOf(-1).equals(call(region, "getW"))
                && Integer.valueOf(-1).equals(call(region, "getH"));
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        Class<?> type = target instanceof Class<?> clazz ? clazz : target.getClass();
        return type.getMethod(method).invoke(target instanceof Class<?> ? null : target);
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getField(name).get(target);
    }
}

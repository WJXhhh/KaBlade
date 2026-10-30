package com.wjx.kablade.client.shader;

import org.joml.Vector2f;

import java.util.Map;
import java.util.Optional;

/** Exercises the actual reflection bridge against synthetic runtime state, with no SR/MC boot. */
final class SuperResolutionBridgeChecks {
    static void run() throws Exception {
        var bridge = new SuperResolutionCompat.Access(Api.class, Config.class, Iris.class, Dispatcher.class,
                Processor.class.getMethod("adaptJitterForShaderpack", Vector2f.class,
                        Object.class, Object.class, Object.class));
        assertLayout(bridge.layout(1919, 1079), 1279, 719, 0.25F, -0.375F);
        // The processor mutates its input; the dispatcher-owned jitter must remain intact.
        if (Dispatcher.jitter.y != 0.375F) throw new AssertionError("SR jitter was mutated");
        Config.enabled = false;
        assertFull(bridge.layout(1919, 1079));
        Config.enabled = true;
        Iris.supported = false;
        assertFull(bridge.layout(1919, 1079));
        Iris.supported = true;
        Api.description.name = "none"; // frame generation only
        assertFull(bridge.layout(1919, 1079));
        Api.description.name = "temporal";
        Api.description.temporal = false; // spatial upscaler
        assertLayout(bridge.layout(1919, 1079), 1279, 719, 0, 0);
        Api.description.temporal = true;
        Api.renderWidth = 1919;
        Api.renderHeight = 1079; // native ratio still needs temporal jitter
        assertLayout(bridge.layout(1919, 1079), 1919, 1079, 0.25F, -0.375F);
        Api.screenWidth = 853;
        Api.screenHeight = 479;
        Api.renderWidth = 568;
        Api.renderHeight = 319;
        Dispatcher.jitter.set(-0.125F, -0.25F); // next frame + window resize
        assertLayout(bridge.layout(853, 479), 568, 319, -0.125F, 0.25F);
        assertLayout(bridge.layout(568, 319), 568, 319, -0.125F, 0.25F); // actual small allocations
        assertFull(bridge.layout(1920, 1080)); // stale allocation during resize
        Iris.pack = new Pack(); // shaderpack/profile reload, no stale frame state
        Iris.profile = new Profile();
        Iris.profile.enabled = false;
        assertFull(bridge.layout(853, 479));
        Iris.profile.enabled = true;
        Iris.pack.version = 2;
        assertFull(bridge.layout(853, 479));
        Iris.pack.version = 3;
        Iris.profile.upscale.inputTextures.get("color").region.w = -2; // full-display input
        assertFull(bridge.layout(853, 479));
        SkillShaderTarget vanilla = new SkillShaderTarget(1, 2, 3, 65, 37, false);
        if (SuperResolutionCompat.adapt(vanilla) != vanilla) throw new AssertionError("Vanilla adapted");
        try {
            Class.forName("io.homo.superresolution.api.SuperResolutionAPI");
            throw new AssertionError("Probe unexpectedly depends on the optional SR mod");
        } catch (ClassNotFoundException expected) {
            // Normal test/build classpath has no Super Resolution dependency.
        }
        System.out.println("SR bridge mode/config/resize/reload checks passed without the SR mod.");
    }

    private static void assertFull(SkillRenderLayout layout) {
        if (!layout.equals(SkillRenderLayout.full(layout.textureWidth(), layout.textureHeight()))) {
            throw new AssertionError("Expected unchanged full-size mapping: " + layout);
        }
    }

    private static void assertLayout(SkillRenderLayout actual, int width, int height, float x, float y) {
        if (!actual.equals(new SkillRenderLayout(actual.textureWidth(), actual.textureHeight(),
                0, 0, width, height, x, y))) throw new AssertionError(actual);
    }

    public static class Api {
        public static int screenWidth = 1919, screenHeight = 1079, renderWidth = 1279, renderHeight = 719;
        public static Description description = new Description();
        public static Object getCurrentAlgorithmDescription() { return description; }
        public static Object getCurrentAlgorithm() { return new Object(); }
        public static int getScreenWidth() { return screenWidth; }
        public static int getScreenHeight() { return screenHeight; }
        public static int getRenderWidth() { return renderWidth; }
        public static int getRenderHeight() { return renderHeight; }
    }

    public static class Config {
        public static boolean enabled = true;
        public static boolean isEnableUpscale() { return enabled; }
    }

    public static class Iris {
        public static boolean supported = true;
        public static Pack pack = new Pack();
        public static Profile profile = new Profile();
        public static boolean shouldApplySuperResolutionChanges() { return supported; }
        public static Optional<Pack> getCurrentShaderPackConfig() { return Optional.of(pack); }
        public static Optional<Profile> getCurrentConfig() { return Optional.of(profile); }
    }

    public static class Pack {
        public int version = 3;
        public Object getProcessor() { return new Processor(); }
    }

    public static class Profile {
        public boolean enabled = true;
        public Upscale upscale = new Upscale();
        public Jitter jitter = new Jitter();
    }

    public static class Upscale {
        public boolean enabled = true;
        public Map<String, Input> inputTextures = Map.of("color", new Input(), "depth", new Input());
    }

    public static class Input {
        public boolean enabled = true;
        public Region region = new Region();
    }

    public static class Region {
        public int w = -1;
        public int getX() { return 0; }
        public int getY() { return 0; }
        public int getW() { return w; }
        public int getH() { return -1; }
    }

    public static class Jitter {
        public boolean enabled = true;
        public String source = "MOD";
    }

    public static class Description {
        public String name = "temporal";
        public boolean temporal = true;
        public String getCodeName() { return name; }
        public boolean isSupportJitter() { return temporal; }
    }

    public static class Dispatcher {
        public static Vector2f jitter = new Vector2f(0.25F, 0.375F);
        public static Vector2f getJitterOffset() { return jitter; }
    }

    public static class Processor {
        public Vector2f adaptJitterForShaderpack(Vector2f jitter, Object algorithm, Object config, Object description) {
            return jitter.mul(1, -1);
        }
    }
}

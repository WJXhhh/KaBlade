package com.wjx.kablade.client.model;

import com.wjx.kablade.config.ModConfig;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/**
 * 根据运行环境为拔刀剑静态 VBO 选择保守的默认策略。
 * AUTO 只把明确识别出的 Android/Pojav/OpenGL ES 转译环境降级；用户仍可在配置中强制覆盖。
 */
public final class BladeRenderHardwareProfile {
    private static final long MIB = 1024L * 1024L;

    private static Snapshot snapshot;

    private BladeRenderHardwareProfile() {
    }

    public static synchronized Snapshot get() {
        if (snapshot == null) {
            snapshot = detect();
        }
        return snapshot;
    }

    public static synchronized void invalidate() {
        snapshot = null;
    }

    private static Snapshot detect() {
        String osName = value(System.getProperty("os.name"));
        String vmName = value(System.getProperty("java.vm.name"));
        String runtimeName = value(System.getProperty("java.runtime.name"));
        String javaHome = value(System.getProperty("java.home"));
        String userHome = value(System.getProperty("user.home"));
        String libraryPath = value(System.getProperty("java.library.path"));
        String pojavEnvironment = value(safeEnvironment("POJAV_LAUNCHER"));
        String pojavRenderer = value(safeEnvironment("POJAV_RENDERER"));
        String glVendor = safeGlString(GL11.GL_VENDOR);
        String glRenderer = safeGlString(GL11.GL_RENDERER);
        String glVersion = safeGlString(GL11.GL_VERSION);

        String combined = (osName + ' ' + vmName + ' ' + runtimeName + ' ' + javaHome + ' '
                + userHome + ' ' + libraryPath + ' ' + pojavEnvironment + ' ' + pojavRenderer + ' '
                + glVendor + ' ' + glRenderer + ' ' + glVersion)
                .toLowerCase(Locale.ROOT);

        boolean mobile = containsAny(combined,
                "android", "pojav", "openjdk mobile", "dalvik");
        boolean translatedGles = containsAny(combined,
                "opengl es", "gl4es", "libglues", "angle", "ltw renderer");
        long maxHeapMiB = Runtime.getRuntime().maxMemory() / MIB;

        Tier tier;
        if (mobile) {
            tier = Tier.MOBILE;
        } else if (translatedGles) {
            tier = Tier.COMPATIBILITY;
        } else if (maxHeapMiB < 2048L) {
            tier = Tier.LOW;
        } else if (maxHeapMiB < 4096L) {
            tier = Tier.MEDIUM;
        } else {
            tier = Tier.HIGH;
        }

        long initialMiB;
        long automaticMaxMiB;
        switch (tier) {
            case MOBILE:
            case COMPATIBILITY:
                initialMiB = 16L;
                automaticMaxMiB = 64L;
                break;
            case LOW:
                initialMiB = 32L;
                automaticMaxMiB = 96L;
                break;
            case MEDIUM:
                initialMiB = 64L;
                automaticMaxMiB = 256L;
                break;
            default:
                initialMiB = 96L;
                automaticMaxMiB = 512L;
                break;
        }

        int configuredMaxMiB = ModConfig.GeneralConf.SlashBladeVboCacheMaxMiB;
        long maximumMiB = configuredMaxMiB > 0 ? configuredMaxMiB : automaticMaxMiB;
        initialMiB = Math.min(initialMiB, maximumMiB);

        String mode = normalizeMode(ModConfig.GeneralConf.SlashBladeVboMode);
        boolean vboEnabled = "ON".equals(mode)
                || ("AUTO".equals(mode) && tier != Tier.MOBILE && tier != Tier.COMPATIBILITY);

        return new Snapshot(tier, vboEnabled, initialMiB * MIB, maximumMiB * MIB,
                maxHeapMiB, glVendor, glRenderer, glVersion, mode);
    }

    private static String normalizeMode(String mode) {
        if (mode == null) {
            return "AUTO";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return "ON".equals(normalized) || "OFF".equals(normalized) ? normalized : "AUTO";
    }

    private static String safeGlString(int name) {
        try {
            return value(GL11.glGetString(name));
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }

    private static String safeEnvironment(String name) {
        try {
            return System.getenv(name);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    private static String value(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public enum Tier {
        MOBILE,
        COMPATIBILITY,
        LOW,
        MEDIUM,
        HIGH
    }

    public static final class Snapshot {
        private final Tier tier;
        private final boolean vboEnabled;
        private final long initialCacheBytes;
        private final long maximumCacheBytes;
        private final long maxHeapMiB;
        private final String glVendor;
        private final String glRenderer;
        private final String glVersion;
        private final String mode;

        private Snapshot(Tier tier, boolean vboEnabled, long initialCacheBytes, long maximumCacheBytes,
                         long maxHeapMiB, String glVendor, String glRenderer, String glVersion, String mode) {
            this.tier = tier;
            this.vboEnabled = vboEnabled;
            this.initialCacheBytes = initialCacheBytes;
            this.maximumCacheBytes = maximumCacheBytes;
            this.maxHeapMiB = maxHeapMiB;
            this.glVendor = glVendor;
            this.glRenderer = glRenderer;
            this.glVersion = glVersion;
            this.mode = mode;
        }

        public Tier getTier() {
            return tier;
        }

        public boolean isVboEnabled() {
            return vboEnabled;
        }

        public long getInitialCacheBytes() {
            return initialCacheBytes;
        }

        public long getMaximumCacheBytes() {
            return maximumCacheBytes;
        }

        public long getMaxHeapMiB() {
            return maxHeapMiB;
        }

        public String getGlVendor() {
            return glVendor;
        }

        public String getGlRenderer() {
            return glRenderer;
        }

        public String getGlVersion() {
            return glVersion;
        }

        public String getMode() {
            return mode;
        }

        public int getModelWarmupBatchSize() {
            if (tier == Tier.MOBILE || tier == Tier.COMPATIBILITY) {
                return 2;
            }
            if (tier == Tier.LOW) {
                return 4;
            }
            return Integer.MAX_VALUE;
        }

        public int getTextureWarmupBatchSize() {
            if (tier == Tier.MOBILE || tier == Tier.COMPATIBILITY) {
                return 4;
            }
            if (tier == Tier.LOW) {
                return 8;
            }
            return Integer.MAX_VALUE;
        }

        public boolean usesIncrementalWarmup() {
            return getModelWarmupBatchSize() != Integer.MAX_VALUE;
        }
    }
}

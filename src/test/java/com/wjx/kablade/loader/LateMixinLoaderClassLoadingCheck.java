package com.wjx.kablade.loader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.regex.Pattern;

/** Regression check for LaunchWrapper's prefix-based TweakClass exclusions. */
public final class LateMixinLoaderClassLoadingCheck {
    public static void main(String[] args) throws Exception {
        String tweaker = args[0];
        // Launch.launch() deliberately does not append a dot to this prefix.
        String excludedPrefix = tweaker.substring(0, tweaker.lastIndexOf('.'));
        String[] paths = System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator));
        URL[] sources = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            sources[i] = new File(paths[i]).toURI().toURL();
        }

        Class<?> launchLoaderType = Class.forName("net.minecraft.launchwrapper.LaunchClassLoader");
        try (URLClassLoader launchLoader = (URLClassLoader) launchLoaderType
                .getConstructor(URL[].class).newInstance((Object) sources)) {
            if (launchLoader.getResource("com/wjx/kablade/mixinloader/KaBladeLateMixinLoader.class") != null) {
                throw new AssertionError("Stale late loader in excluded package; run a clean build");
            }
            Method exclude = launchLoaderType.getMethod("addClassLoaderExclusion", String.class);
            exclude.invoke(launchLoader, excludedPrefix);
            // Mixin itself is shared with the parent; MixinBooter's loader API is not.
            exclude.invoke(launchLoader, "org.spongepowered.asm.");

            Class<?> api = Class.forName("zone.rong.mixinbooter.ILateMixinLoader", true, launchLoader);
            Class<?> loader = Class.forName("com.wjx.kablade.loader.KaBladeLateMixinLoader", true, launchLoader);
            Object instance = api.cast(loader.getConstructor().newInstance());
            if (loader.getClassLoader() != launchLoader || api.getClassLoader() != launchLoader) {
                throw new AssertionError("Late loader and MixinBooter API must use LaunchClassLoader");
            }
            Object configs = api.getMethod("getMixinConfigs").invoke(instance);
            if (!Collections.singletonList("mixins.kablade.json").equals(configs)) {
                throw new AssertionError("Unexpected late mixin configs: " + configs);
            }
        }
        System.out.println("Late mixin loader class-loading check passed");
    }
}

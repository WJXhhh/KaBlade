package com.wjx.kablade.client.compat;

import java.lang.reflect.Method;
import java.util.function.Function;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Applies optional RenderOverrideEvent setters when the installed SlashBlade exposes them. */
public final class SlashBladeRenderOverrideCompat {
    private static final Method SET_RENDER_TYPE = findMethod("setGetRenderType", Function.class);
    private static final Method SET_PACKED_LIGHT = findMethod("setPackedLightIn", int.class);

    private SlashBladeRenderOverrideCompat() {}

    public static void setRenderType(RenderOverrideEvent event, Function<ResourceLocation, RenderType> type) {
        invoke(SET_RENDER_TYPE, event, type);
    }

    public static void setPackedLight(RenderOverrideEvent event, int light) {
        invoke(SET_PACKED_LIGHT, event, light);
    }

    private static Method findMethod(String name, Class<?> parameter) {
        try {
            return RenderOverrideEvent.class.getMethod(name, parameter);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static void invoke(Method method, RenderOverrideEvent event, Object value) {
        if (method == null) return;
        try {
            method.invoke(event, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The setter is optional on older SlashBlade versions.
        }
    }
}

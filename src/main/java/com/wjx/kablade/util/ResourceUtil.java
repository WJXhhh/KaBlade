package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import net.minecraft.resources.ResourceLocation;

public class ResourceUtil {
    public static ResourceLocation getLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, path);
    }
}

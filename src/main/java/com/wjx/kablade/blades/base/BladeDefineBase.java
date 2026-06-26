package com.wjx.kablade.blades.base;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public abstract class BladeDefineBase {
    public BladeDefineBase(BootstapContext<SlashBladeDefinition> context) {}

    public BladeDefineBase() {}

    /**
     * 通过enum快速获取对应拔刀剑的ResourceLocation
     * @param type 填入你要获取的拔刀剑基本物品对应的enum值，一般自制刀用ORDINARY，崩坏的拔刀剑用HONKAI
     */
    public static ResourceLocation getBaseBladeId(BaseBladeType type) {
        switch (type) {
            case ORDINARY -> {
                return ResourceLocation.fromNamespaceAndPath(Main.MODID, "kablade_blade_named");
            }
            case HONKAI -> {
                return ResourceLocation.fromNamespaceAndPath(Main.MODID, "kablade_honkai_named");
            }
            case SP_LIGHT -> {
                return ResourceLocation.fromNamespaceAndPath(Main.MODID, "kablade_sl_named");
            }
            case ALL_WEAPON -> {
                return ResourceLocation.fromNamespaceAndPath(Main.MODID, "kablade_aw_named");
            }
        }
        throw new IllegalStateException("Unknown blade type " + type);
    }

    public static ResourceKey<SlashBladeDefinition> createBladeKey(String path) {
        return ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY,ResourceLocation.fromNamespaceAndPath(Main.MODID, path));
    }

    public enum BaseBladeType{
        ORDINARY,HONKAI,SP_LIGHT,ALL_WEAPON
    }

    public abstract String getKey();
}

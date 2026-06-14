package com.wjx.kablade.blades;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.ordinary.RimmedEarth;
import com.wjx.kablade.blades.ordinary.RockyAnshan;
import com.wjx.kablade.blades.ordinary.RockyHuagang;
import com.wjx.kablade.blades.ordinary.RockyShanchang;
import com.wjx.kablade.init.ModItems;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.item.CreativeModeTab;

/** Registers named blades that all use kablade:kablade_blade as their carrier item. */
public final class BladeLoader {
    public static BladeDefineBase RIMMED_EARTH;
    public static BladeDefineBase ROCKY_ANSHAN;
    public static BladeDefineBase ROCKY_HUAGANG;
    public static BladeDefineBase ROCKY_SHANCHANG;

    public static void bootstrap(BootstapContext<SlashBladeDefinition> context) {
        RIMMED_EARTH = new RimmedEarth(context);
        // 岩石线 Lv1：夯土刀 + 安山岩/花岗岩/闪长岩
        ROCKY_ANSHAN = new RockyAnshan(context);
        ROCKY_HUAGANG = new RockyHuagang(context);
        ROCKY_SHANCHANG = new RockyShanchang(context);
    }

    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters,
                                       CreativeModeTab.Output output) {
        HolderLookup.RegistryLookup<SlashBladeDefinition> definitions =
                SlashBlade.getSlashBladeDefinitionRegistry(parameters.holders());
        definitions.listElements()
                .map(Holder.Reference::value)
                .filter(definition -> {
                    if (ModItems.KABLADE_BLADE.getId() != null) {
                        return ModItems.KABLADE_BLADE.getId().equals(definition.getItemName());
                    }
                    return false;
                })
                .map(SlashBladeDefinition::getBlade)
                .filter(stack -> !stack.isEmpty())
                .forEach(output::accept);
    }
}

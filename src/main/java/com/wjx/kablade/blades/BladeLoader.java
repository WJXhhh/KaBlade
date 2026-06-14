package com.wjx.kablade.blades;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.ordinary.RimmedEarth;
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

    public static void bootstrap(BootstapContext<SlashBladeDefinition> context) {
        RIMMED_EARTH = new RimmedEarth(context);
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

package com.wjx.kablade.data;

import com.wjx.kablade.Main;
import com.wjx.kablade.blade.KabladeBlades;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KabladeDataGen {

    private KabladeDataGen() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        RegistrySetBuilder blades = new RegistrySetBuilder()
                .add(SlashBladeDefinition.REGISTRY_KEY, KabladeBlades::bootstrap);

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                generator.getPackOutput(), event.getLookupProvider(), blades, Set.of(Main.MODID)));
    }
}

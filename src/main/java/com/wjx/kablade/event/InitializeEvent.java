package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.blades.KabladeBlades;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

//需手动运行gradlew runData
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class InitializeEvent {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        RegistrySetBuilder blades = new RegistrySetBuilder()
                .add(SlashBladeDefinition.REGISTRY_KEY, KabladeBlades::bootstrap);

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                generator.getPackOutput(), event.getLookupProvider(), blades, Set.of(Main.MODID)));
    }
}

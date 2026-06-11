package com.wjx.kablade.blade;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.init.ModSlashArts;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.List;

/** Registers named blades that all use kablade:kablade_blade as their carrier item. */
public final class KabladeBlades {

    public static final ResourceKey<SlashBladeDefinition> HANGTU = key("hangtu");

    private KabladeBlades() {
    }

    public static void bootstrap(BootstapContext<SlashBladeDefinition> context) {
        context.register(HANGTU, new SlashBladeDefinition(
                id("kablade_blade"),
                HANGTU.location(),
                RenderDefinition.Builder.newInstance()
                        .modelName(id("model/named/hangtu/mdl.obj"))
                        .textureName(id("model/named/hangtu/tex.png"))
                        .effectColor(0x8B6B3F)
                        .standbyRenderType(CarryType.KATANA)
                        .build(),
                PropertiesDefinition.Builder.newInstance()
                        .baseAttackModifier(2.0F)
                        .maxDamage(60)
                        .slashArtsType(ModSlashArts.HANGTU.getId())
                        .defaultSwordType(List.of(SwordType.BEWITCHED))
                        .build(),
                List.of()));
    }

    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters,
                                       CreativeModeTab.Output output) {
        HolderLookup.RegistryLookup<SlashBladeDefinition> definitions =
                SlashBlade.getSlashBladeDefinitionRegistry(parameters.holders());
        definitions.listElements()
                .map(reference -> reference.value())
                .filter(definition -> ModItems.KABLADE_BLADE.getId().equals(definition.getItemName()))
                .map(SlashBladeDefinition::getBlade)
                .filter(stack -> !stack.isEmpty())
                .forEach(output::accept);
    }

    private static ResourceKey<SlashBladeDefinition> key(String path) {
        return ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, path);
    }
}

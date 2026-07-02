package com.wjx.kablade.client;

import com.wjx.kablade.init.ModItems;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Builds complete display stacks for named-blade recipe ingredients without changing their matching rules. */
public final class BladeRecipePreviewHydrator {
    private BladeRecipePreviewHydrator() {
    }

    public static void hydrate(ItemStack stack) {
        if (!isKabladeCarrier(stack)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        syncStateFromTag(stack);
        String translationKey = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getTranslationKey)
                .orElse("");
        if (translationKey.isBlank()) {
            return;
        }

        ResourceLocation carrierId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        HolderLookup.RegistryLookup<SlashBladeDefinition> definitions =
                SlashBlade.getSlashBladeDefinitionRegistry(minecraft.level.registryAccess());
        Optional<SlashBladeDefinition> definition = definitions.listElements()
                .map(Holder.Reference::value)
                .filter(value -> value.getTranslationKey().equals(translationKey))
                .filter(value -> value.getItemName().equals(carrierId))
                .findFirst();
        if (definition.isEmpty()) {
            return;
        }

        int[] requestedCounts = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(BladeRecipePreviewHydrator::readCounts)
                .orElseGet(() -> new int[3]);
        Map<Enchantment, Integer> requestedEnchantments = new LinkedHashMap<>(stack.getAllEnchantments());

        ItemStack hydrated = definition.get().getBlade(stack.getItem());
        if (hydrated.isEmpty()) {
            return;
        }

        Map<Enchantment, Integer> mergedEnchantments = new LinkedHashMap<>(hydrated.getAllEnchantments());
        requestedEnchantments.forEach((enchantment, level) ->
                mergedEnchantments.merge(enchantment, level, Math::max));
        EnchantmentHelper.setEnchantments(mergedEnchantments, hydrated);

        hydrated.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setProudSoulCount(Math.max(state.getProudSoulCount(), requestedCounts[0]));
            state.setKillCount(Math.max(state.getKillCount(), requestedCounts[1]));
            state.setRefine(Math.max(state.getRefine(), requestedCounts[2]));
            hydrated.getOrCreateTag().put("bladeState", state.serializeNBT());
        });
        if(stack.hasTag()){
            stack.setTag(hydrated.getTag() == null ? null : hydrated.getTag().copy());
            syncStateFromTag(stack);
        }
    }

    private static boolean isKabladeCarrier(ItemStack stack) {
        return stack.is(ModItems.KABLADE_BLADE.get())
                || stack.is(ModItems.KABLADE_HONKAI_BLADE.get())
                || stack.is(ModItems.KABLADE_SL_BLADE.get())
                || stack.is(ModItems.KABLADE_AW_BLADE.get());
    }

    private static void syncStateFromTag(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("bladeState")) {
            stack.getCapability(ItemSlashBlade.BLADESTATE)
                    .ifPresent(state -> state.deserializeNBT(stack.getTag().getCompound("bladeState")));
        }
    }

    private static int[] readCounts(ISlashBladeState state) {
        return new int[]{state.getProudSoulCount(), state.getKillCount(), state.getRefine()};
    }
}

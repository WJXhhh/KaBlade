package com.wjx.kablade.compat.emi;

import com.wjx.kablade.Main;
import com.wjx.kablade.client.BladeRecipePreviewHydrator;
import com.wjx.kablade.init.ModItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers EMI support for KBlade carriers and SlashBlade-shaped recipes.
 */
@EmiEntrypoint
public final class KbladeEmiPlugin implements EmiPlugin {

    public static final Comparison BLADE_COMPARISON = Comparison.compareData(KbladeEmiPlugin::getBladeIdentity);

    @Override
    public void register(EmiRegistry registry) {
        registry.setDefaultComparison(ModItems.KABLADE_BLADE.get(), BLADE_COMPARISON);
        registry.setDefaultComparison(ModItems.KABLADE_HONKAI_BLADE.get(), BLADE_COMPARISON);
        registry.setDefaultComparison(ModItems.KABLADE_SL_BLADE.get(), BLADE_COMPARISON);
        registry.setDefaultComparison(ModItems.KABLADE_AW_BLADE.get(), BLADE_COMPARISON);

        Set<ResourceLocation> carrierIds = getCarrierIds();
        registry.removeEmiStacks(stack -> carrierIds.contains(stack.getId()) && getBladeState(stack) == null);

        RecipeManager recipeManager = registry.getRecipeManager();
        RegistryAccess registryAccess = resolveRegistryAccess();
        if (registryAccess == null) {
            Main.LOGGER.warn("[{}] EMI registry access unavailable during plugin registration; using raw blade preview stacks", Main.MODID);
        } else {
            registerNamedBladeStacks(registry, registryAccess, carrierIds);
        }

        Set<ResourceLocation> slashBladeRecipeIds = new LinkedHashSet<>();
        for (var recipe : recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe instanceof SlashBladeShapedRecipe shapedBladeRecipe && shouldWrapRecipe(shapedBladeRecipe)) {
                slashBladeRecipeIds.add(recipe.getId());
            }
        }

        registry.removeRecipes(recipe ->
                recipe.getId() != null && slashBladeRecipeIds.contains(recipe.getId()));

        for (var recipe : recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe instanceof SlashBladeShapedRecipe shapedBladeRecipe && shouldWrapRecipe(shapedBladeRecipe)) {
                registry.addRecipe(new EmiShapedBladeRecipe(shapedBladeRecipe, registryAccess));
            }
        }
    }

    private static boolean shouldWrapRecipe(SlashBladeShapedRecipe recipe) {
        ResourceLocation outputBlade = recipe.getOutputBlade();
        return Main.MODID.equals(recipe.getId().getNamespace())
                || outputBlade != null && Main.MODID.equals(outputBlade.getNamespace());
    }

    private static void registerNamedBladeStacks(EmiRegistry registry, RegistryAccess registryAccess, Set<ResourceLocation> carrierIds) {
        for (SlashBladeDefinition definition : registryAccess.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)) {
            if (!carrierIds.contains(definition.getItemName())) {
                continue;
            }
            ItemStack stack = definition.getBlade();
            if (stack.isEmpty()) {
                continue;
            }
            BladeRecipePreviewHydrator.hydrate(stack);
            registry.addEmiStack(EmiStack.of(stack).comparison(BLADE_COMPARISON));
        }
    }

    private static Set<ResourceLocation> getCarrierIds() {
        return Set.of(
                ModItems.KABLADE_BLADE.getId(),
                ModItems.KABLADE_HONKAI_BLADE.getId(),
                ModItems.KABLADE_SL_BLADE.getId(),
                ModItems.KABLADE_AW_BLADE.getId());
    }

    private static RegistryAccess resolveRegistryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            return connection.registryAccess();
        }
        return null;
    }

    public static String getBladeIdentity(EmiStack stack) {
        String translationKey = getBladeTranslationKey(stack);
        if (!translationKey.isEmpty()) {
            return translationKey;
        }
        CompoundTag bladeState = getBladeState(stack);
        if (bladeState != null) {
            String model = bladeState.getString("ModelName");
            String texture = bladeState.getString("TextureName");
            if (!model.isEmpty() || !texture.isEmpty()) {
                return model + "|" + texture;
            }
        }
        return stack.getId().toString();
    }

    private static CompoundTag getBladeState(EmiStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains("bladeState")) {
            return stack.getNbt().getCompound("bladeState");
        }
        return null;
    }

    private static String getBladeTranslationKey(EmiStack stack) {
        CompoundTag bladeState = getBladeState(stack);
        if (bladeState != null) {
            return bladeState.getString("translationKey");
        }

        ItemStack itemStack = stack.getItemStack();
        if (!itemStack.isEmpty()) {
            return itemStack.getCapability(
                            mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE)
                    .map(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState::getTranslationKey)
                    .orElse("");
        }
        return "";
    }
}

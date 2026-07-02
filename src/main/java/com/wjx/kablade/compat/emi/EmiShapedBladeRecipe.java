package com.wjx.kablade.compat.emi;

import com.wjx.kablade.client.BladeRecipePreviewHydrator;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps SlashBlade-shaped recipes so EMI can render them as crafting recipes.
 */
public final class EmiShapedBladeRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 116;
    private static final int HEIGHT = 54;
    private static final int SLOT_X = 0;
    private static final int SLOT_Y = 0;
    private static final int SLOT_SIZE = 18;
    private static final int OUTPUT_X = 94;
    private static final int OUTPUT_Y = 18;
    private static final int ARROW_X = 62;
    private static final int ARROW_Y = 18;

    public EmiShapedBladeRecipe(SlashBladeShapedRecipe recipe, @Nullable RegistryAccess registryAccess) {
        super(VanillaEmiRecipeCategories.CRAFTING, getEmiRecipeId(recipe), WIDTH, HEIGHT);

        List<EmiIngredient> inputList = new ArrayList<>(9);
        for (int row = 0; row < recipe.getHeight(); row++) {
            for (int col = 0; col < recipe.getWidth(); col++) {
                int idx = row * recipe.getWidth() + col;
                if (idx < recipe.getIngredients().size()) {
                    inputList.add(toEmiIngredient(recipe.getIngredients().get(idx)));
                } else {
                    inputList.add(EmiStack.EMPTY);
                }
            }
        }
        while (inputList.size() < 9) {
            inputList.add(EmiStack.EMPTY);
        }
        this.inputs = inputList;

        ItemStack output = resolveOutput(recipe, registryAccess);
        this.outputs = List.of(EmiStack.of(output).comparison(KbladeEmiPlugin.BLADE_COMPARISON));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < 9; i++) {
            int x = SLOT_X + (i % 3) * SLOT_SIZE;
            int y = SLOT_Y + (i / 3) * SLOT_SIZE;
            if (i < inputs.size() && !inputs.get(i).isEmpty()) {
                widgets.addSlot(inputs.get(i), x, y);
            } else {
                widgets.addSlot(EmiStack.EMPTY, x, y);
            }
        }

        widgets.addTexture(EmiTexture.EMPTY_ARROW, ARROW_X, ARROW_Y);
        widgets.addSlot(outputs.get(0), OUTPUT_X, OUTPUT_Y)
                .large(true)
                .recipeContext(this);
    }

    private static EmiIngredient toEmiIngredient(Ingredient ingredient) {
        if (ingredient instanceof SlashBladeIngredient) {
            ItemStack[] items = ingredient.getItems();
            List<EmiStack> emiStacks = new ArrayList<>();
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    BladeRecipePreviewHydrator.hydrate(stack);
                    emiStacks.add(EmiStack.of(stack).comparison(KbladeEmiPlugin.BLADE_COMPARISON));
                }
            }
            if (!emiStacks.isEmpty()) {
                return EmiIngredient.of(emiStacks);
            }
        }
        return EmiIngredient.of(ingredient);
    }

    private static ItemStack resolveOutput(SlashBladeShapedRecipe recipe, @Nullable RegistryAccess registryAccess) {
        if (registryAccess != null) {
            SlashBladeDefinition definition = registryAccess
                    .registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                    .get(recipe.getOutputBlade());
            if (definition != null) {
                ItemStack definedBlade = definition.getBlade();
                if (!definedBlade.isEmpty()) {
                    BladeRecipePreviewHydrator.hydrate(definedBlade);
                    ensureTranslationKey(definedBlade, recipe);
                    return definedBlade;
                }
            }
        }

        ItemStack result = recipe.getResultItem(registryAccess != null ? registryAccess : RegistryAccess.EMPTY);
        if (registryAccess != null && result.getItem() instanceof ItemSlashBlade) {
            BladeRecipePreviewHydrator.hydrate(result);
        }
        ensureTranslationKey(result, recipe);
        return result;
    }

    private static ResourceLocation getEmiRecipeId(SlashBladeShapedRecipe recipe) {
        ResourceLocation id = recipe.getId();
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "emi/" + id.getPath());
    }

    private static void ensureTranslationKey(ItemStack stack, SlashBladeShapedRecipe recipe) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) {
            return;
        }
        if (recipe.getOutputBlade().equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
            return;
        }

        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            String recipeKey = Util.makeDescriptionId("item", recipe.getOutputBlade());
            if (!recipeKey.equals(state.getTranslationKey())) {
                state.setNonEmpty();
                state.setTranslationKey(recipeKey);
                stack.getOrCreateTag().put("bladeState", state.serializeNBT());
            }
        });
    }

}

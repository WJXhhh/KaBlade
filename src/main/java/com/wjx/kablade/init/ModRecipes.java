package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.recipe.BladeGrindingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Main.MODID);
    public static final RegistryObject<RecipeSerializer<BladeGrindingRecipe>> BLADE_GRINDING =
            REGISTRY.register("blade_grinding", () -> new SimpleCraftingRecipeSerializer<>(BladeGrindingRecipe::new));

    private ModRecipes() {
    }
}

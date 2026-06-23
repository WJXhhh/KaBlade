package com.wjx.kablade.mixin.client;

import com.wjx.kablade.client.BladeRecipePreviewHydrator;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin {
    @Inject(method = "getItems", at = @At("RETURN"))
    private void kablade$hydrateBladeRecipePreview(CallbackInfoReturnable<ItemStack[]> cir) {
        if ((Object) this instanceof SlashBladeIngredient) {
            for (ItemStack stack : cir.getReturnValue()) {
                BladeRecipePreviewHydrator.hydrate(stack);
            }
        }
    }
}

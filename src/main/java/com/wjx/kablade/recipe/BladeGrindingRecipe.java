package com.wjx.kablade.recipe;

import com.wjx.kablade.init.ModRecipes;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** The 1.12 grinding layout: stone above and to the right of the blade. */
public final class BladeGrindingRecipe extends CustomRecipe {
    private static final int SOUL_COST = 2;

    public BladeGrindingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private ItemStack findBlade(CraftingContainer container) {
        if (container.getWidth() < 2 || container.getHeight() < 2) return ItemStack.EMPTY;
        ItemStack stone = container.getItem(1);
        ItemStack blade = container.getItem(container.getWidth());
        if (!stone.is(ItemTags.STONE_TOOL_MATERIALS) || !(blade.getItem() instanceof ItemSlashBlade)
                || blade.getDamageValue() <= 0) return ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i != 1 && i != container.getWidth() && !container.getItem(i).isEmpty()) return ItemStack.EMPTY;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .filter(state -> state.getProudSoulCount() > SOUL_COST)
                .map(state -> blade).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !findBlade(container).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack blade = findBlade(container);
        if (blade.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = blade.copy();
        result.setCount(1);
        result.setDamageValue(result.getDamageValue() - 1);
        result.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setProudSoulCount(state.getProudSoulCount() - SOUL_COST);
            result.getOrCreateTag().put("bladeState", state.serializeNBT());
        });
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BLADE_GRINDING.get();
    }
}

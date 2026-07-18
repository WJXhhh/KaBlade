package com.wjx.kablade.SlashBlade.blades.recipe;

import com.wjx.kablade.Main;
import keinsleif.timeless_ivy.helper.ItemNBTHelper;
import keinsleif.timeless_ivy.item.InitItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 为调用 setNoRepair() 的耐久物品补充永恒常春藤配方。
 * 普通可修复物品仍由 TimelessIvy 自带配方处理，避免重复匹配。
 */
public class RecipeRegenIvyCompat extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    public RecipeRegenIvyCompat() {
        setRegistryName(new ResourceLocation(Main.MODID, "timeless_ivy_attach_no_repair"));
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        ItemStack tool = ItemStack.EMPTY;
        boolean hasRegenIvy = false;
        int repairMaterialCount = 0;

        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (stack.isItemStackDamageable()
                    && !item.isRepairable()
                    && (!stack.hasTagCompound()
                    || !ItemNBTHelper.getBoolean(stack, "Botania_regenIvy", false))) {
                tool = stack;
            } else if (item == InitItem.regenIvy) {
                hasRegenIvy = true;
            }
        }

        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (!tool.isEmpty() && tool.getItem().getIsRepairable(tool, stack)) {
                repairMaterialCount++;
            } else if (stack != tool && item != InitItem.regenIvy) {
                return false;
            }
        }

        return !tool.isEmpty() && hasRegenIvy && repairMaterialCount == 3;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack tool = ItemStack.EMPTY;

        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty()
                    && stack.isItemStackDamageable()
                    && !stack.getItem().isRepairable()) {
                tool = stack;
            }
        }

        if (tool.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = tool.copy();
        ItemNBTHelper.setBoolean(result, "Botania_regenIvy", true);
        result.setCount(1);
        return result;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}

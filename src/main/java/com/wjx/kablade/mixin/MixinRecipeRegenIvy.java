package com.wjx.kablade.mixin;

import keinsleif.timeless_ivy.helper.ItemNBTHelper;
import keinsleif.timeless_ivy.item.InitItem;
import keinsleif.timeless_ivy.recipe.RecipeRegenIvy;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 兼容调用 setNoRepair() 的耐久物品，例如 KaBlade 的拔刀剑。
 */
@Mixin(value = RecipeRegenIvy.class, remap = false)
public abstract class MixinRecipeRegenIvy {

    /**
     * @author KaBlade
     * @reason 常春藤附着应只要求物品具有可用耐久，不应要求原版工作台修复权限。
     */
    @Overwrite
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
                    && (!stack.hasTagCompound() || !ItemNBTHelper.getBoolean(stack, "Botania_regenIvy", false))) {
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

    /**
     * @author KaBlade
     * @reason 与 matches 使用相同的可用耐久判断，避免输出目标与匹配目标不一致。
     */
    @Overwrite
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack tool = ItemStack.EMPTY;

        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.isItemStackDamageable()) {
                tool = stack;
            }
        }

        ItemStack result = tool.copy();
        ItemNBTHelper.setBoolean(result, "Botania_regenIvy", true);
        result.setCount(1);
        return result;
    }
}

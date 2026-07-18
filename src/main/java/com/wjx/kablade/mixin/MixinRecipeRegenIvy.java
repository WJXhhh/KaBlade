package com.wjx.kablade.mixin;

import keinsleif.timeless_ivy.helper.ItemNBTHelper;
import keinsleif.timeless_ivy.item.InitItem;
import keinsleif.timeless_ivy.recipe.RecipeRegenIvy;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 兼容调用 setNoRepair() 的耐久物品，例如 KaBlade 的拔刀剑。
 */
@Mixin(value = RecipeRegenIvy.class, remap = false)
public abstract class MixinRecipeRegenIvy {

    @Inject(
            method = {
                    "matches(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Z",
                    "func_77569_a(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Z"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void kablade$matches(InventoryCrafting inv, World worldIn,
                                 CallbackInfoReturnable<Boolean> cir) {
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
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(!tool.isEmpty() && hasRegenIvy && repairMaterialCount == 3);
    }

    @Inject(
            method = {
                    "getCraftingResult(Lnet/minecraft/inventory/InventoryCrafting;)Lnet/minecraft/item/ItemStack;",
                    "func_77572_b(Lnet/minecraft/inventory/InventoryCrafting;)Lnet/minecraft/item/ItemStack;"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void kablade$getCraftingResult(InventoryCrafting inv,
                                           CallbackInfoReturnable<ItemStack> cir) {
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
        cir.setReturnValue(result);
    }
}

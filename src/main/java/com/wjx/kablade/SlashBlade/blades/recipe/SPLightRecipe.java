package com.wjx.kablade.SlashBlade.blades.recipe;

import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class SPLightRecipe extends RecipeAwakeBlade {

    ItemStack sphere;
    ItemStack sphere2;

    public SPLightRecipe(ResourceLocation loc, ItemStack result, ItemStack requiredStateBlade, Object... recipe) {
        super(loc, result, requiredStateBlade, recipe);
    }

    public SPLightRecipe(ResourceLocation loc,final ItemStack result, final ItemStack requiredStateBlade, final ItemStack reqiredSphere, final ItemStack reqiredSphere2, final Object[] recipe) {
        super(loc,result, requiredStateBlade, recipe);
        this.sphere = reqiredSphere;
        this.sphere2 = reqiredSphere2;
    }

    @Override
    public boolean matches(final InventoryCrafting inv, final World world) {
        final boolean result = super.matches(inv, world);
        if (!result) {
            return false;
        }
        if (this.sphere == null) {
            return false;
        }
        if (this.sphere2 == null) {
            return false;
        }
        boolean isPass1 = false;
        boolean isPass2 = false;
        final int requiredsa1 = ItemSlashBlade.SpecialAttackType.get(this.sphere.getTagCompound());
        final int requiredsa2 = ItemSlashBlade.SpecialAttackType.get(this.sphere2.getTagCompound());
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            final ItemStack current = inv.getStackInSlot(i);
            if (current.isItemEqual(this.sphere)) {
                final int currentsa = ItemSlashBlade.SpecialAttackType.get(current.getTagCompound());
                if (requiredsa1 == currentsa) {
                    isPass1 = true;
                }
                else if (requiredsa2 == currentsa) {
                    isPass2 = true;
                }
            }
        }
        return isPass1 && isPass2;
    }
}

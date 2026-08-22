package com.wjx.kablade.SlashBlade.blades.recipe;

import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;

/** 按命名刀内部名称校验主刀，并继承其成长数据。 */
public class SlashBladeNamedRecipe extends ShapedOreRecipe {
    private final ItemStack requiredBlade;

    public SlashBladeNamedRecipe(ResourceLocation location, ItemStack result,
                                 ItemStack requiredBlade, Object... recipe) {
        super(location, result, recipe);
        this.requiredBlade = requiredBlade;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world) || requiredBlade.isEmpty()) return false;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (sameNamedBlade(requiredBlade, current)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack result = super.getCraftingResult(inventory);
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (!sameNamedBlade(requiredBlade, current)) continue;

            NBTTagCompound oldTag = ItemSlashBlade.getItemTagCompound(current).copy();
            NBTTagCompound resultTag = ItemSlashBlade.getItemTagCompound(result);
            if (ItemSlashBladeNamed.CurrentItemName.exists(resultTag)) {
                ItemStack registered = SlashBlade.getCustomBlade(
                        ItemSlashBladeNamed.CurrentItemName.get(resultTag));
                if (!registered.isEmpty()) result = registered;
            }
            NBTTagCompound newTag = ItemSlashBlade.getItemTagCompound(result);
            ItemSlashBlade.KillCount.set(newTag, ItemSlashBlade.KillCount.get(oldTag));
            ItemSlashBlade.ProudSoul.set(newTag, ItemSlashBlade.ProudSoul.get(oldTag));
            ItemSlashBlade.RepairCount.set(newTag, ItemSlashBlade.RepairCount.get(oldTag));
            if (oldTag.hasUniqueId("Owner")) newTag.setUniqueId("Owner", oldTag.getUniqueId("Owner"));
            break;
        }
        return result;
    }

    private static boolean sameNamedBlade(ItemStack required, ItemStack actual) {
        if (required.isEmpty() || actual.isEmpty() || !(actual.getItem() instanceof ItemSlashBlade)) return false;
        NBTTagCompound expected = ItemSlashBlade.getItemTagCompound(required);
        NBTTagCompound present = ItemSlashBlade.getItemTagCompound(actual);
        return ItemSlashBladeNamed.CurrentItemName.exists(expected)
                && ItemSlashBladeNamed.CurrentItemName.exists(present)
                && ItemSlashBladeNamed.CurrentItemName.get(expected)
                .equals(ItemSlashBladeNamed.CurrentItemName.get(present));
    }
}

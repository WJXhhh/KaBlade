package com.wjx.kablade.SlashBlade.blades.recipe;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.TagPropertyAccessor;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.Map;

public class SlashBladeRecipeModding extends ShapedOreRecipe {
    ItemStack requiredStateBlade;

    public SlashBladeRecipeModding(ResourceLocation loc,ItemStack result, ItemStack requiredStateBlade, Object... recipe) {
        super(loc, result, recipe);
        this.requiredStateBlade = requiredStateBlade;
    }

    int tagValueCompare(TagPropertyAccessor<Integer> access, NBTTagCompound reqTag, NBTTagCompound srcTag){
        return access.get(reqTag).compareTo(access.get(srcTag));
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {

        boolean result = super.matches(inv, world);

        if(result && !requiredStateBlade.isEmpty()){        //Check recipe legality
            requiredStateBlade.setItemDamage(OreDictionary.WILDCARD_VALUE);     //Use wildcard value means that any damage blade can be used to craft
            for(int idx = 0; idx < inv.getSizeInventory(); idx++){      //Scan CraftTable
                ItemStack curIs = inv.getStackInSlot(idx);
                if(!curIs.isEmpty()
                        && curIs.getItem() instanceof ItemSlashBlade
                        && curIs.hasTagCompound()){


                    //Check SlashBlade requirement
                    Map<Enchantment,Integer> oldItemEnchants = EnchantmentHelper.getEnchantments(requiredStateBlade);
                    for(Map.Entry<Enchantment,Integer> enchant: oldItemEnchants.entrySet())
                    {
                        int level = EnchantmentHelper.getEnchantmentLevel(enchant.getKey(),curIs);
                        if(level < enchant.getValue()){
                            return false;               //cur Blade's echant must be enought to participate craft
                        }
                    }

                    NBTTagCompound reqTag = ItemSlashBlade.getItemTagCompound(requiredStateBlade);
                    NBTTagCompound srcTag = ItemSlashBlade.getItemTagCompound(curIs);

                    if(!curIs.getTranslationKey().equals(requiredStateBlade.getTranslationKey())) //Check specific SlashBlade
                        return false;

                    if(0 < tagValueCompare(ItemSlashBlade.ProudSoul, reqTag, srcTag))
                        return false;
                    if(0 < tagValueCompare(ItemSlashBlade.KillCount, reqTag, srcTag))
                        return false;
                    if(0 < tagValueCompare(ItemSlashBlade.RepairCount, reqTag, srcTag))
                        return false;



                    break;
                }
            }
        }


        return result;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting var1) {
        ItemStack result = super.getCraftingResult(var1);

        for(int idx = 0; idx < var1.getSizeInventory(); idx++){  //Scan source Blade
            ItemStack curIs = var1.getStackInSlot(idx);
            if(!curIs.isEmpty()
                    && curIs.getItem() instanceof ItemSlashBlade
                    && curIs.hasTagCompound()){

                NBTTagCompound oldTag = curIs.getTagCompound();
                oldTag = (NBTTagCompound)oldTag.copy();

                {
                    NBTTagCompound newTag;
                    newTag = ItemSlashBlade.getItemTagCompound(result);

                    if(ItemSlashBladeNamed.CurrentItemName.exists(newTag)){
                        ItemStack tmp;
                        String key = ItemSlashBladeNamed.CurrentItemName.get(newTag);
                        tmp = SlashBlade.getCustomBlade(key);

                        if(!tmp.isEmpty())
                            result = tmp;
                    }
                }

                NBTTagCompound newTag;
                newTag = ItemSlashBlade.getItemTagCompound(result);

                //Extend Origin Data
                ItemSlashBlade.KillCount.set(newTag, ItemSlashBlade.KillCount.get(oldTag));
                ItemSlashBlade.ProudSoul.set(newTag, ItemSlashBlade.ProudSoul.get(oldTag));
                ItemSlashBlade.RepairCount.set(newTag, ItemSlashBlade.RepairCount.get(oldTag));

                if(oldTag.hasUniqueId("Owner"))
                    newTag.setUniqueId("Owner",oldTag.getUniqueId("Owner"));

                if(oldTag.hasKey(ItemSlashBlade.adjustXStr))
                    newTag.setFloat(ItemSlashBlade.adjustXStr,oldTag.getFloat(ItemSlashBlade.adjustXStr));

                if(oldTag.hasKey(ItemSlashBlade.adjustYStr))
                    newTag.setFloat(ItemSlashBlade.adjustYStr,oldTag.getFloat(ItemSlashBlade.adjustYStr));

                if(oldTag.hasKey(ItemSlashBlade.adjustZStr))
                    newTag.setFloat(ItemSlashBlade.adjustZStr,oldTag.getFloat(ItemSlashBlade.adjustZStr));

                {
                    Map<Enchantment,Integer> newItemEnchants = EnchantmentHelper.getEnchantments(result);
                    Map<Enchantment,Integer> oldItemEnchants = EnchantmentHelper.getEnchantments(curIs);
                    for(Enchantment enchantIndex : oldItemEnchants.keySet())
                    {
                        Enchantment enchantment = enchantIndex;

                        int destLevel = newItemEnchants.containsKey(enchantIndex) ? newItemEnchants.get(enchantIndex) : 0;
                        int srcLevel = oldItemEnchants.get(enchantIndex);

                        srcLevel = Math.max(srcLevel, destLevel); //Get highest level


                        boolean canApplyFlag = enchantment.canApply(result);
                        if(canApplyFlag){
                            for(Enchantment curEnchantIndex : newItemEnchants.keySet()){
                                if (curEnchantIndex != enchantIndex && !enchantment.isCompatibleWith(curEnchantIndex) /*canApplyTogether*/)
                                {
                                    canApplyFlag = false;
                                    break;
                                }
                            }
                            if (canApplyFlag)
                                newItemEnchants.put(enchantIndex, Integer.valueOf(srcLevel));
                        }
                    }
                    EnchantmentHelper.setEnchantments(newItemEnchants, result);
                }
            }
        }

        return result;
    }
}

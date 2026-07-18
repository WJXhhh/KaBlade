package com.wjx.kablade.SlashBlade.blades.recipe;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.List;

/**
 * 替换原版 RecipeInstantRepair，使用完全相同的合成摆法：
 *   " X"
 *   "B "
 *
 * 配置开启：KaBlade 刀免费修复（不消耗 ProudSoul）
 * 配置关闭或原版 SlashBlade 刀：交给 SlashBlade 原配方处理
 */
public class RecipeKaBladeRepair extends ShapedOreRecipe {

    public RecipeKaBladeRepair() {
        super(new ResourceLocation(Main.MODID, "cobblestone_repair"),
                new ItemStack(SlashBlade.weapon, 1, 0),
                " X",
                "B ",
                'X', "cobblestone",
                'B', new ItemStack(SlashBlade.weapon, 1, 0));
        this.setRegistryName(new ResourceLocation(Main.MODID, "cobblestone_repair"));
    }

    @Override
    public boolean matches(InventoryCrafting cInv, World world) {
        // 配置关闭时完全不匹配，交给原版配方处理
        if (!ModConfig.GeneralConf.KaBladeFreeRepair)
            return false;

        if (cInv.getWidth() != 2 || cInv.getHeight() != 2)
            return false;

        if (!cInv.getStackInRowAndColumn(0, 0).isEmpty())
            return false;
        if (!cInv.getStackInRowAndColumn(1, 1).isEmpty())
            return false;

        ItemStack stone = cInv.getStackInRowAndColumn(1, 0);
        if (stone.isEmpty())
            return false;

        List<ItemStack> ores = OreDictionary.getOres("cobblestone");
        if (!containsMatch(false, ores, stone))
            return false;

        ItemStack target = cInv.getStackInRowAndColumn(0, 1);
        if (target.isEmpty() || !(target.getItem() instanceof ItemSlashBlade))
            return false;
        if (!ItemSlashUtil.KAITEMBLADE.contains(target.getItem()))
            return false;
        if (target.getItemDamage() <= 0)
            return false;

        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting cInv) {
        ItemStack stone = cInv.getStackInRowAndColumn(1, 0);
        ItemStack target = cInv.getStackInRowAndColumn(0, 1);

        ItemStack result = target.copy();

        if (result.isEmpty() || !(result.getItem() instanceof ItemSlashBlade))
            return result;

        int damage = result.getItemDamage();
        if (damage <= 0)
            return result;

        int repair = Math.min(stone.getCount(), damage);

        result.setItemDamage(damage - repair);
        if (result.hasTagCompound()) {
            ItemSlashBlade.RepairCount.set(result.getTagCompound(), repair);
        }

        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        // 委托给原版逻辑处理圆石消耗
        NonNullList<ItemStack> ret = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        ItemStack stone = inv.getStackInRowAndColumn(1, 0);
        ItemStack target = inv.getStackInRowAndColumn(0, 1);

        int repair = 0;
        if (!target.isEmpty()
                && target.getItem() instanceof ItemSlashBlade
                && ItemSlashUtil.KAITEMBLADE.contains(target.getItem())) {
            int damage = target.getItemDamage();
            if (damage > 0) {
                repair = Math.min(stone.getCount(), damage);
            }
        }

        if (repair > 1) {
            stone.shrink(repair - 1);
        }

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (ret.get(i).isEmpty()) {
                ret.set(i, ForgeHooks.getContainerItem(inv.getStackInSlot(i)));
            }
        }

        return ret;
    }

    private static boolean containsMatch(boolean strict, List<ItemStack> inputs, ItemStack... targets) {
        for (ItemStack input : inputs) {
            for (ItemStack target : targets) {
                if (OreDictionary.itemMatches(target, input, strict)) {
                    return true;
                }
            }
        }
        return false;
    }
}

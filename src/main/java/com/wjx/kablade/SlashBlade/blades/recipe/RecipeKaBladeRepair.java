package com.wjx.kablade.SlashBlade.blades.recipe;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.RecipeInstantRepair;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.List;
import org.apache.logging.log4j.Level;

/**
 * 替换原版 RecipeInstantRepair，使用完全相同的合成摆法：
 *   " X"
 *   "B "
 *
 * KaBlade 刀 → 免费修复（不消耗 ProudSoul）
 * 原版 SlashBlade 刀 → 正常消耗 ProudSoul（行为与原版一致）
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
        if (!containsMatch(false, ores, new ItemStack(Blocks.COBBLESTONE)))
            return false;

        ItemStack target = cInv.getStackInRowAndColumn(0, 1);
        if (target.isEmpty() || !(target.getItem() instanceof ItemSlashBlade))
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

        // 计算修复量
        int repair = Math.min(stone.getCount(), damage);

        // KaBlade 刀：免费修复，不消耗 ProudSoul
        if (ItemSlashUtil.KAITEMBLADE.contains(result.getItem())) {
            Main.logger.log(Level.INFO, "[KaBladeRepair] KaBlade free repair: damage=" + damage + " repair=" + repair);
            result.setItemDamage(damage - repair);
            if (result.hasTagCompound()) {
                result.getTagCompound().setInteger("RepairCount", repair);
            }
        } else {
            Main.logger.log(Level.INFO, "[KaBladeRepair] Original blade: consuming ProudSoul");
            // 原版 SlashBlade 刀：正常消耗 ProudSoul（与原版行为一致）
            if (result.hasTagCompound()) {
                NBTTagCompound tag = result.getTagCompound();
                int proudSoul = ItemSlashBlade.ProudSoul.get(tag);
                int repairPoints = proudSoul / RecipeInstantRepair.RepairProudSoulCount;
                repair = Math.min(stone.getCount(), Math.min(repairPoints, damage));

                Main.logger.log(Level.INFO, "[KaBladeRepair] Original PS=" + proudSoul + " repair=" + repair + " repairPoints=" + repairPoints);

                if (repair > 0) {
                    proudSoul -= repair * RecipeInstantRepair.RepairProudSoulCount;
                    result.setItemDamage(damage - repair);
                    ItemSlashBlade.ProudSoul.set(tag, proudSoul);
                    tag.setInteger(RecipeInstantRepair.RepairCountStr, repair);

                    Main.logger.log(Level.INFO, "[KaBladeRepair] After repair: PS=" + proudSoul + " damage=" + (damage - repair));
                }
            }
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
        if (!target.isEmpty() && target.getItem() instanceof ItemSlashBlade) {
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

package com.wjx.kablade.AllWeapon.recipes;

import mods.flammpfeil.slashblade.ItemSlashBlade;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static com.wjx.kablade.Main.bladestr;

public class AWRec {
    public static ItemStack ToRec(ItemStack blade, World world, Vec3d pos,ItemStack mainhand){
        ItemStack stack = blade.copy();
        NBTTagCompound tag = blade.getTagCompound();
        if(tag!=null){
            if(stack.getItem().getClass() == ItemSlashBladeNamed.class){
                if(mainhand.getItem() == Items.DIAMOND_SWORD)
                {//流刃若火
                    if (world.getBlockState(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)).getBlock().equals(Blocks.LAVA)) {
                        ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.liurrh", 1);
                        NBTTagCompound rt = res.getTagCompound();
                        ItemSlashBlade.KillCount.set(rt,ItemSlashBlade.KillCount.get(tag));
                        ItemSlashBlade.ProudSoul.set(rt,ItemSlashBlade.ProudSoul.get(tag));
                        ItemSlashBlade.RepairCount.set(rt,ItemSlashBlade.RepairCount.get(tag));
                        return res;
                    }
                }
            }
        }

        return ItemStack.EMPTY;

    }
}

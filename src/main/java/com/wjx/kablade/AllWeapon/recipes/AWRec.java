package com.wjx.kablade.AllWeapon.recipes;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockGlowstone;
import net.minecraft.block.BlockLeaves;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static com.wjx.kablade.Main.bladestr;
import static com.wjx.kablade.Main.logger;

public class AWRec {
    public static ItemStack ToRec(ItemStack blade, World world, Vec3d pos,ItemStack mainhand,int standType,int dimension,int hit){
        ItemStack stack = blade.copy();
        NBTTagCompound tag = blade.getTagCompound();
        //logger.warn("check "+(tag==null)+" "+stack.getItem().getClass()+" "+mainhand.getDisplayName()+" "+world.getBlockState(new BlockPos( pos.x,  pos.y,  pos.z)).getBlock().getLocalizedName()+" "+(stack.getItem().getClass() == ItemSlashBladeNamed.class)+" "+pos.x+" "+pos.z);
        if(tag!=null){
            ItemStack targetBlade = SlashBlade.findItemStack(bladestr,"slashbladeNamed",1);
            if(blade.getTranslationKey().equals(targetBlade.getTranslationKey())){


                //白兰剑
                if(hit==1&&world.getBlockState(new BlockPos( Math.floor(pos.x),  Math.round(pos.y),  Math.floor(pos.z))).getBlock() instanceof BlockFlower &&standType==1){
                    {
                        if(ItemSlashBlade.RepairCount.get(tag)>=10)
                        {
                            ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.bailan", 1);
                            //logger.warn(res.getDisplayName());
                            NBTTagCompound rt = res.getTagCompound();
                            ItemSlashBlade.KillCount.set(rt, ItemSlashBlade.KillCount.get(tag));
                            ItemSlashBlade.ProudSoul.set(rt, ItemSlashBlade.ProudSoul.get(tag));
                            ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                            world.setBlockState(new BlockPos( Math.floor(pos.x),  Math.round(pos.y),  Math.floor(pos.z)),Blocks.AIR.getDefaultState(),3);
                            return res;
                        }
                    }
                }
                //绿萝
                if(hit==1&&world.getBlockState(new BlockPos( Math.floor(pos.x),  Math.round(pos.y-1),  Math.floor(pos.z))).getBlock() instanceof BlockLeaves &&standType==1){
                    {
                        if(ItemSlashBlade.RepairCount.get(tag)>=10)
                        {
                            ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.lvluo", 1);
                            //logger.warn(res.getDisplayName());
                            NBTTagCompound rt = res.getTagCompound();
                            ItemSlashBlade.KillCount.set(rt, ItemSlashBlade.KillCount.get(tag));
                            ItemSlashBlade.ProudSoul.set(rt, ItemSlashBlade.ProudSoul.get(tag));
                            ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                            world.setBlockState(new BlockPos( Math.floor(pos.x),  Math.round(pos.y-1),  Math.floor(pos.z)),Blocks.AIR.getDefaultState(),3);
                            return res;
                        }
                    }
                }

            }
        }

        return ItemStack.EMPTY;

    }
}

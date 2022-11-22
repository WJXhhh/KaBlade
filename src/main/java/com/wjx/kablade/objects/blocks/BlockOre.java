package com.wjx.kablade.objects.blocks;

import com.wjx.kablade.util.interfaces.IKabladeOre;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;

import java.util.Random;

public class BlockOre extends BlockBase implements IKabladeOre {
    public BlockOre(String name, CreativeTabs tabs,float hardnessIn,int harvestLevel) {
        super(name, Material.ROCK, tabs);
        this.setHardness(hardnessIn);
        this.setHarvestLevel("pickaxe",harvestLevel);
    }
}

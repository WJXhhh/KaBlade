package com.wjx.kablade.objects.blocks;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.BlockInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.interfaces.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

public class BlockBase extends Block implements IHasModel {
    public BlockBase(String name, Material material, CreativeTabs tabs){
        super(material);
        setUnlocalizedName(name);
        setRegistryName(name);
        setCreativeTab(tabs);
        BlockInit.Blocks.add(this);
        ItemInit.ITEMS.add(new ItemBlock(this).setRegistryName(this.getRegistryName()));
    }

    @Override
    public void registerModels(){
        Main.proxy.registerItemRenderer(Item.getItemFromBlock(this),0,"inventory");

    }
}

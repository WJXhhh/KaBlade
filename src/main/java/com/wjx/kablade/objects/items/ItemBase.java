package com.wjx.kablade.objects.items;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.interfaces.IHasModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemBase extends Item implements IHasModel {
    public ItemBase(String name, CreativeTabs tabs){
        setUnlocalizedName(name);
        setRegistryName(name);
        setCreativeTab(tabs);

        ItemInit.ITEMS.add(this);
    }

    public ItemBase(String name){
        setUnlocalizedName(name);
        setRegistryName(name);


        ItemInit.ITEMS.add(this);
    }

    @Override
    public void registerModels(){
       Main.proxy.registerItemRenderer(this,0,"inventory");

    }
}

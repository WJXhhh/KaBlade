package com.wjx.kablade.creativeTab;

import com.wjx.kablade.init.ItemInit;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class tabkablade_bladesgod extends CreativeTabs {
    public tabkablade_bladesgod(String label) {
        super("tabkablade_bladesgod");
    }

    @Nonnull
    public ItemStack getTabIconItem(){
        return new ItemStack(ItemInit.ICON_GOD);
    }
}

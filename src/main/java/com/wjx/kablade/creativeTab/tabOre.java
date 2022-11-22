package com.wjx.kablade.creativeTab;

import com.wjx.kablade.init.ItemInit;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class tabOre extends CreativeTabs {
    public tabOre() {
        super("tabkablade_ore");
    }

    @Nonnull
    public ItemStack getTabIconItem(){
        return new ItemStack(ItemInit.ICON_ORE);
    }
}

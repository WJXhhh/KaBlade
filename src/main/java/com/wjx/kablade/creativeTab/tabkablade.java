package com.wjx.kablade.creativeTab;

import com.wjx.kablade.init.ItemInit;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class tabkablade extends CreativeTabs {
    public tabkablade() {
        super("tabkablade");
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ItemInit.ICON_MAIN);
    }
}

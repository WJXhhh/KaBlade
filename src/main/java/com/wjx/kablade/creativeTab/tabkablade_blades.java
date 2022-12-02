package com.wjx.kablade.creativeTab;

import com.wjx.kablade.init.ItemInit;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class tabkablade_blades extends CreativeTabs {
    public tabkablade_blades(String label) {
        super("tabkablade_blades");
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ItemInit.ICON_NOTED);
    }
}

package com.wjx.kablade.creativeTab;

import com.wjx.kablade.init.ItemInit;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class tabkablade_honkai_greatsword extends CreativeTabs {
    public tabkablade_honkai_greatsword(String label) {
        super("tabkablade_honkai_greatsword");
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ItemInit.ICON_HONKAI_GREATSWORD);
    }
}

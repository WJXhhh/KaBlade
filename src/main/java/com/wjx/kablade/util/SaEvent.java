package com.wjx.kablade.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public abstract class SaEvent {
    public abstract void run(ItemStack stack, EntityPlayer par3EntityPlayer, boolean isJust);
}

package com.wjx.kablade.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public abstract class BladeAttackEvent {
    public abstract void run(ItemStack stack, EntityPlayer player, Entity entity);
}

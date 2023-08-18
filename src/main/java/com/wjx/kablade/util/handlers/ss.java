package com.wjx.kablade.util.handlers;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class ss {
    public void doAttack(ItemStack stack, ItemSlashBlade.ComboSequence comboSeq, EntityPlayer player, CallbackInfo ci, World world, NBTTagCompound tag, EnumSet<ItemSlashBlade.SwordType> swordType, long currentTime, AxisAlignedBB bb, int rank, List<Entity> list, Iterator var12, Entity curEntity) {

        int i = 0;
        /*int i = 0;
        if (curEntity != null){
            for (BladeAttackEvent event : BladeAttackEventManager.events){
                event.run(stack,player,curEntity);
            }
        }*/
    }
}

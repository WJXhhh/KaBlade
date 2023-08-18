package com.wjx.kablade.mixin;

import com.wjx.kablade.util.BladeAttackEvent;
import com.wjx.kablade.util.BladeAttackEventManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import static org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILSOFT;

@Mixin(value = ItemSlashBlade.class,remap = false)
public abstract class MixinItemSlashBladeDoAttack {
    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", ordinal = 0, shift = At.Shift.AFTER), locals = CAPTURE_FAILSOFT,remap = false)
    public void doAttack(ItemStack stack, ItemSlashBlade.ComboSequence comboSeq, EntityPlayer player, CallbackInfo ci, World world, NBTTagCompound tag, EnumSet<ItemSlashBlade.SwordType> swordType, long currentTime, AxisAlignedBB bb, int rank, List<Entity> list, Iterator var12, Entity curEntity) {
        if (curEntity != null){
            for (BladeAttackEvent event : BladeAttackEventManager.events){
                event.run(stack,player,curEntity);
            }
        }
    }
}

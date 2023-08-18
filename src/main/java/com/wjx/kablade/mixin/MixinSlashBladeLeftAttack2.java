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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemSlashBlade.class)
public abstract class MixinSlashBladeLeftAttack2 {
    @Inject(method = "onLeftClickEntity", at = @At(value = "INVOKE",target = "Lmods/flammpfeil/slashblade/item/ItemSlashBlade;getComboSequence(Lnet/minecraft/nbt/NBTTagCompound;)Lmods/flammpfeil/slashblade/item/ItemSlashBlade$ComboSequence;",ordinal = 0,shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD,remap = false)
    public void onLeftClickEntity2(ItemStack stack, EntityPlayer player, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        for (BladeAttackEvent event : BladeAttackEventManager.events){
            event.run(stack,player,entity);
        }
    }
}

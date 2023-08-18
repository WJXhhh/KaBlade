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

@Mixin(value = ItemSlashBlade.class,remap = false)
public abstract class MixinItemSlashBladeLeftAttack1 {
    @Inject(method = "onLeftClickEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;isSwingInProgress:Z", ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD,remap = false)
    public void onLeftClickEntity1(ItemStack stack, EntityPlayer player, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        for (BladeAttackEvent event : BladeAttackEventManager.events){
            event.run(stack,player,entity);
        }
    }
}

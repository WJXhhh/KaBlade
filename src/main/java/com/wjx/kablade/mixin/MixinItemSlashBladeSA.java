package com.wjx.kablade.mixin;

import com.wjx.kablade.util.SaEvent;
import com.wjx.kablade.util.SaEventLister;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILSOFT;

@Mixin(value = ItemSlashBlade.class,remap = false)
public abstract class MixinItemSlashBladeSA {

    @Inject(
            method = "doChargeAttack",
            at = @At(
                    value = "INVOKE", // 表示注入在方法被调用之前
                    shift = At.Shift.AFTER, // 把注入点往后移一位，也就变成注入在方法被调用之后
                    target = "Lmods/flammpfeil/slashblade/item/ItemSlashBlade;getSpecialAttack(Lnet/minecraft/item/ItemStack;)Lmods/flammpfeil/slashblade/specialattack/SpecialAttackBase;",
                    remap = false
            ),
            locals = CAPTURE_FAILSOFT
    )
    public void doChargeAttack(ItemStack stack, EntityPlayer par3EntityPlayer, boolean isJust, CallbackInfo ci){
        for (SaEvent e : SaEventLister.events){
            e.run(stack,par3EntityPlayer,isJust);
        }
    }


}

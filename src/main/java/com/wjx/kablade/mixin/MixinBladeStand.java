package com.wjx.kablade.mixin;

import com.wjx.kablade.util.BladeAttackEvent;
import com.wjx.kablade.util.BladeAttackEventManager;
import com.wjx.kablade.util.BladeStandHurtManager;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityBladeStand.class)
public  class MixinBladeStand {

    @Inject(method = "attackEntityFrom",at = @At(value = "RETURN"),locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private void attackEntityFrom(DamageSource p_70097_1_, float p_70097_2_, CallbackInfoReturnable<Boolean> cir) {
            EntityBladeStand entityBladeStand = (EntityBladeStand) (Object) this;
            for(BladeStandHurtManager.BladeStandHurtEvent event : BladeStandHurtManager.events){
                event.run(entityBladeStand,p_70097_1_);
            }
    }
}

package com.wjx.kablade.mixin;

import com.wjx.kablade.compat.SstBladeCompat;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The source carries a cast snapshot only for Kablade's scoped delayed callbacks. */
@Pseudo
@Mixin(targets = "cn.star.sstmod.slashblade.BladeAffinityHandler", remap = false)
public abstract class SstBladeAffinityHandlerMixin {
    @Inject(method = "onLivingDamage", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void kablade$applyCastSnapshot(LivingDamageEvent event, CallbackInfo callback) {
        if (SstBladeCompat.applyCapturedAffinity(event)) callback.cancel();
    }
}

package com.wjx.kablade.mixin;

import com.wjx.kablade.client.model.StaticBladeMeshCache;
import mods.flammpfeil.slashblade.client.model.obj.GroupObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GroupObject.class, remap = false)
public abstract class MixinSlashBladeGroupObject {
    @Inject(method = "render()V", at = @At("HEAD"), cancellable = true)
    private void kablade$renderStaticVbo(CallbackInfo ci) {
        if (StaticBladeMeshCache.render((GroupObject) (Object) this)) {
            ci.cancel();
        }
    }
}

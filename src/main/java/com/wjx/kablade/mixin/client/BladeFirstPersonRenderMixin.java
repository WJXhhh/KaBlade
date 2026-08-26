package com.wjx.kablade.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.kablade.client.BladeLuminousTextureLayer;
import mods.flammpfeil.slashblade.client.renderer.model.BladeFirstPersonRender;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BladeFirstPersonRender.class, remap = false)
public abstract class BladeFirstPersonRenderMixin {

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void kablade$beginLuminousFirstPerson(PoseStack poseStack,
                                                  MultiBufferSource buffers,
                                                  int packedLight,
                                                  CallbackInfo ci) {
        BladeLuminousTextureLayer.beginFirstPersonBladeRender();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void kablade$endLuminousFirstPerson(PoseStack poseStack,
                                                MultiBufferSource buffers,
                                                int packedLight,
                                                CallbackInfo ci) {
        BladeLuminousTextureLayer.endFirstPersonBladeRender();
    }
}

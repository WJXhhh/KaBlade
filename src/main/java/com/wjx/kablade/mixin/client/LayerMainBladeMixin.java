package com.wjx.kablade.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.kablade.client.RaidenCycloneBladeTipTracker;
import com.wjx.kablade.client.RaizanCleaveClientState;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LayerMainBlade.class, remap = false)
public abstract class LayerMainBladeMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), remap = false, cancellable = true)
    private void kablade$beginTipCapture(PoseStack poseStack, MultiBufferSource buffers, int light,
                                         LivingEntity owner, float limbSwing, float limbSwingAmount,
                                         float partialTick, float ageInTicks, float netHeadYaw,
                                         float headPitch, CallbackInfo ci) {
        if (RaizanCleaveClientState.isActive(owner.getId())) {
            ci.cancel();
            return;
        }
        RaidenCycloneBladeTipTracker.beginBladeLayer(owner);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("RETURN"), remap = false)
    private void kablade$endTipCapture(PoseStack poseStack, MultiBufferSource buffers, int light,
                                       LivingEntity owner, float limbSwing, float limbSwingAmount,
                                       float partialTick, float ageInTicks, float netHeadYaw,
                                       float headPitch, CallbackInfo ci) {
        RaidenCycloneBladeTipTracker.endBladeLayer();
    }
}

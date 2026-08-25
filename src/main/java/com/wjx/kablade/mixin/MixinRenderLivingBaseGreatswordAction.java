package com.wjx.kablade.mixin;

import com.wjx.kablade.client.renderer.GreatswordVmdAnimation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 1.12 平面 ModelBiped 外补上 Player Animator 的 body 父节点。 */
@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBaseGreatswordAction {
    @Unique
    private boolean kablade$greatswordRootPushed;

    @Inject(method = "renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V",
            at = @At("HEAD"))
    private void kablade$pushGreatswordRoot(EntityLivingBase owner, float limbSwing,
                                             float limbSwingAmount, float ageInTicks,
                                             float netHeadYaw, float headPitch, float scaleFactor,
                                             CallbackInfo ci) {
        float partial = Math.max(0.0F, Math.min(1.0F, ageInTicks - owner.ticksExisted));
        GlStateManager.pushMatrix();
        kablade$greatswordRootPushed = true;
        GreatswordVmdAnimation.INSTANCE.applyPlayerRootPose(owner, partial);
    }

    @Inject(method = "renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V",
            at = @At("RETURN"))
    private void kablade$popGreatswordRoot(EntityLivingBase owner, float limbSwing,
                                            float limbSwingAmount, float ageInTicks,
                                            float netHeadYaw, float headPitch, float scaleFactor,
                                            CallbackInfo ci) {
        if (kablade$greatswordRootPushed) {
            GlStateManager.popMatrix();
            kablade$greatswordRootPushed = false;
        }
    }
}

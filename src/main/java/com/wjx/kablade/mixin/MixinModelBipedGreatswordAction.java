package com.wjx.kablade.mixin;

import com.wjx.kablade.client.renderer.GreatswordVmdAnimation;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 把 1.20 Player Animator 的 VMD 姿态写入 1.12 ModelBiped。 */
@Mixin(ModelBiped.class)
public abstract class MixinModelBipedGreatswordAction {
    @Unique
    private boolean kablade$greatswordPoseApplied;

    @Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V",
            at = @At("RETURN"))
    private void kablade$greatswordVmd(float limbSwing, float limbSwingAmount,
                                       float ageInTicks, float netHeadYaw, float headPitch,
                                       float scaleFactor, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof EntityLivingBase)) return;
        float partial = ageInTicks - entity.ticksExisted;
        ModelBiped model = (ModelBiped) (Object) this;
        boolean applied = GreatswordVmdAnimation.INSTANCE.applyPlayerPose(model,
                (EntityLivingBase) entity, Math.max(0.0F, Math.min(1.0F, partial)));
        if (!applied && kablade$greatswordPoseApplied) {
            GreatswordVmdAnimation.INSTANCE.resetPlayerPose(model);
        }
        kablade$greatswordPoseApplied = applied;
    }
}

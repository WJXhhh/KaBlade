package com.wjx.kablade.mixin;

import com.wjx.kablade.client.renderer.ElectricSkillFeedback;
import com.wjx.kablade.client.renderer.RaidenBladeTipTracker;
import mods.flammpfeil.slashblade.client.renderer.entity.layers.LayerSlashBlade;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 天殛隐藏原刀层；重磁暴为 Wavefront 刀尖采样建立持有者上下文。 */
@Mixin(value=LayerSlashBlade.class,remap=false)
public abstract class MixinLayerSlashBlade {
    @Inject(method="doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",at=@At("HEAD"),cancellable=true,remap=false)
    private void kablade$begin(EntityLivingBase owner,float limbSwing,float limbSwingAmount,float partial,float age,float headYaw,float headPitch,float scale,CallbackInfo ci){if(ElectricSkillFeedback.isRaizanActive(owner.getEntityId())){ci.cancel();return;}RaidenBladeTipTracker.begin(owner);}
    @Inject(method="doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",at=@At("RETURN"),remap=false)
    private void kablade$end(EntityLivingBase owner,float limbSwing,float limbSwingAmount,float partial,float age,float headYaw,float headPitch,float scale,CallbackInfo ci){RaidenBladeTipTracker.end();}
}

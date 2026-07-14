package com.wjx.kablade.mixin;

import com.meteor.extrabotany.common.entity.gaia.EntityGaiaIII;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 兼容 Gaia III 的装备限制配置。
 */
@Mixin(value = EntityGaiaIII.class, remap = false)
public abstract class MixinEntityGaiaIII {

    @Inject(method = "match(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private static void kablade$match(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.GeneralConf.ExtraBotanyGaiaDisableEquipmentRestrictions
                || ModConfig.GeneralConf.ExtraBotanyGaiaAllowSlashBlade
                && !stack.isEmpty() && stack.getItem() instanceof ItemSlashBlade) {
            cir.setReturnValue(true);
        }
    }
}

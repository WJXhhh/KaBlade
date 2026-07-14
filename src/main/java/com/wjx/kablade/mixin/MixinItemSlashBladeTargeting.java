package com.wjx.kablade.mixin;

import com.wjx.kablade.util.TargetingUtil;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.TagPropertyAccessor;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemSlashBlade.class, remap = false)
public abstract class MixinItemSlashBladeTargeting {
    private static final ThreadLocal<EntityPlayer> KABLADE_LOCK_PLAYER = new ThreadLocal<>();

    /**
     * @author KaBlade
     * @reason r32 把候选 AABB offset 到射程终点，无法覆盖完整射线路径。
     */
    @Overwrite
    public Entity getRayTrace(EntityLivingBase owner, double reachMax) {
        return TargetingUtil.rayTraceEntity(owner, reachMax, 0.0F);
    }

    /**
     * @author KaBlade
     * @reason 统一过滤规则，并使用经过方块裁剪的 swept AABB。
     */
    @Overwrite
    public Entity getRayTrace(EntityLivingBase owner, double reachMax, float expandBorder) {
        return TargetingUtil.rayTraceEntity(owner, reachMax, expandBorder);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"), remap = false)
    private void kablade$beginLockUpdate(ItemStack stack, World world, Entity entity, int slot,
                                         boolean isSelected, CallbackInfo ci) {
        if (!world.isRemote && ModConfig.GeneralConf.EnableShiftLockTargeting
                && isSelected && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            KABLADE_LOCK_PLAYER.set(player);
            TargetingUtil.observeMoveCommand(player);
            TargetingUtil.getValidLockedTarget(player, stack, TargetingUtil.DEFAULT_LOCK_DISTANCE);
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"), remap = false)
    private void kablade$endLockUpdate(ItemStack stack, World world, Entity entity, int slot,
                                       boolean isSelected, CallbackInfo ci) {
        KABLADE_LOCK_PLAYER.remove();
    }

    @Redirect(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/flammpfeil/slashblade/TagPropertyAccessor$TagPropertyInteger;get(Lnet/minecraft/nbt/NBTTagCompound;)Ljava/lang/Integer;",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private Integer kablade$suppressRepeatedLockSearch(TagPropertyAccessor.TagPropertyInteger accessor,
                                                        NBTTagCompound tag) {
        EntityPlayer player = KABLADE_LOCK_PLAYER.get();
        if (player != null && ModConfig.GeneralConf.EnableShiftLockTargeting
                && TargetingUtil.shouldSuppressRepeatedVanillaSearch(player)
                && ItemSlashBlade.TargetEntityId.get(tag) == 0) {
            // 只欺骗本次 onUpdate 的局部变量，绝不把哨兵写入 NBT。
            return -1;
        }
        return accessor.get(tag);
    }
}

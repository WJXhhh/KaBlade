package com.wjx.kablade.mixin;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.network.MessageMoveCommandState;
import mods.flammpfeil.slashblade.network.MessageMoveCommandStateHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MessageMoveCommandStateHandler.class, remap = false)
public abstract class MixinMessageMoveCommandStateHandler {
    @Inject(
            method = "onMessage(Lmods/flammpfeil/slashblade/network/MessageMoveCommandState;Lnet/minecraftforge/fml/common/network/simpleimpl/MessageContext;)Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void kablade$handleMoveCommandOnServerThread(MessageMoveCommandState message, MessageContext ctx,
                                                          CallbackInfoReturnable<IMessage> cir) {
        // 关闭时不取消原版 r32 处理器，完整回退其 Shift 锁敌输入链路。
        if (!ModConfig.GeneralConf.EnableShiftLockTargeting) {
            return;
        }
        if (ctx.getServerHandler() == null || ctx.getServerHandler().player == null) {
            cir.setReturnValue(null);
            return;
        }

        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> {
            ItemStack stack = player.getHeldItemMainhand();
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
                return;
            }

            byte oldCommand = player.getEntityData().getByte("SB.MCS");
            byte newCommand = message.command;
            player.getEntityData().setByte("SB.MCS", newCommand);

            boolean oldSneak = (oldCommand & MessageMoveCommandState.SNEAK) != 0;
            boolean newSneak = (newCommand & MessageMoveCommandState.SNEAK) != 0;
            if (!oldSneak && newSneak) {
                TargetingUtil.onSneakPressed(player);
                ItemSlashBlade.TargetEntityId.set(ItemSlashBlade.getItemTagCompound(stack), 0);
                Entity target = TargetingUtil.resolveTarget(player, stack, 20.0D, 10.0D, 5.0D);
                ItemSlashBlade.TargetEntityId.set(ItemSlashBlade.getItemTagCompound(stack),
                        target == null ? 0 : target.getEntityId());
            } else if (oldSneak && !newSneak) {
                TargetingUtil.onSneakReleased(player);
                ItemSlashBlade.TargetEntityId.set(ItemSlashBlade.getItemTagCompound(stack), 0);
            }

            if (ModConfig.GeneralConf.DebugTargeting && Main.logger != null) {
                Main.logger.info("[Targeting] packet player={} tick={} thread={} sneak={}->{} target={}",
                        player.getName(), player.world.getTotalWorldTime(), Thread.currentThread().getName(),
                        oldSneak, newSneak,
                        ItemSlashBlade.TargetEntityId.get(ItemSlashBlade.getItemTagCompound(stack)));
            }
        });
        cir.setReturnValue(null);
    }
}

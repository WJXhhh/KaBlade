package com.wjx.kablade.mixin;

import com.wjx.kablade.entity.SunfireImpactEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve authoritative collision movement without teleporting the predicting client each tick. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SunfireMovementMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Shadow private int aboveGroundTickCount;
    @Shadow public abstract void resetPosition();

    // This second serverLevel() call is after both the main-thread handoff and vanilla's
    // non-finite movement validation. Those checks must still run for a controlled player.
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;serverLevel()Lnet/minecraft/server/level/ServerLevel;",
            ordinal = 1), cancellable = true)
    private void kablade$keepSunfirePosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!SunfireImpactEntity.isCasting(player)) return;
        player.setYRot(Mth.wrapDegrees(packet.getYRot(player.getYRot())));
        player.setXRot(Mth.wrapDegrees(packet.getXRot(player.getXRot())));
        clientIsFloating = false;
        aboveGroundTickCount = 0;
        resetPosition();
        ci.cancel();
    }
}

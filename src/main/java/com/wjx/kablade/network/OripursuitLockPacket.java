package com.wjx.kablade.network;

import com.wjx.kablade.client.OripursuitClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record OripursuitLockPacket(int entityId) {

    public static void encode(OripursuitLockPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
    }

    public static OripursuitLockPacket decode(FriendlyByteBuf buf) {
        return new OripursuitLockPacket(buf.readVarInt());
    }

    public static void handle(OripursuitLockPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> OripursuitClientState.setLockedEntityId(packet.entityId));
        context.setPacketHandled(true);
    }
}

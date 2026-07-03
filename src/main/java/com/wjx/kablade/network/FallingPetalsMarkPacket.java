package com.wjx.kablade.network;

import com.wjx.kablade.client.FallingPetalsClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record FallingPetalsMarkPacket(int entityId, int duration) {

    public static void encode(FallingPetalsMarkPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.duration);
    }

    public static FallingPetalsMarkPacket decode(FriendlyByteBuf buf) {
        return new FallingPetalsMarkPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FallingPetalsMarkPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FallingPetalsClientState.mark(packet.entityId, packet.duration));
        context.setPacketHandled(true);
    }
}

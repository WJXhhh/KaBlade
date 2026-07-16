package com.wjx.kablade.network;

import com.wjx.kablade.client.RaidenCycloneRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record RaidenCycloneEndPacket(long castId, byte reason) {
    public static final byte COMPLETE = 0;
    public static final byte TARGET_LOST = 1;
    public static final byte OWNER_LOST = 2;

    public static void encode(RaidenCycloneEndPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.castId);
        buf.writeByte(packet.reason);
    }

    public static RaidenCycloneEndPacket decode(FriendlyByteBuf buf) {
        return new RaidenCycloneEndPacket(buf.readLong(), buf.readByte());
    }

    public static void handle(RaidenCycloneEndPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RaidenCycloneRenderer.stop(packet.castId, packet.reason));
        context.setPacketHandled(true);
    }
}

package com.wjx.kablade.network;

import com.wjx.kablade.client.RaidenCycloneRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record RaidenCycloneFxPacket(long castId, int casterId, int targetId,
                                    long serverStartGameTime, long seed,
                                    double originX, double originY, double originZ,
                                    double targetX, double targetY, double targetZ,
                                    float basisRotation) {

    public static void encode(RaidenCycloneFxPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.castId);
        buf.writeVarInt(packet.casterId);
        buf.writeInt(packet.targetId);
        buf.writeLong(packet.serverStartGameTime);
        buf.writeLong(packet.seed);
        buf.writeDouble(packet.originX);
        buf.writeDouble(packet.originY);
        buf.writeDouble(packet.originZ);
        buf.writeDouble(packet.targetX);
        buf.writeDouble(packet.targetY);
        buf.writeDouble(packet.targetZ);
        buf.writeFloat(packet.basisRotation);
    }

    public static RaidenCycloneFxPacket decode(FriendlyByteBuf buf) {
        return new RaidenCycloneFxPacket(buf.readLong(), buf.readVarInt(), buf.readInt(),
                buf.readLong(), buf.readLong(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
    }

    public static void handle(RaidenCycloneFxPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RaidenCycloneRenderer.start(packet));
        context.setPacketHandled(true);
    }
}

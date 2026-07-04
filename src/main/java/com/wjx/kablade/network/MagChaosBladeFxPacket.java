package com.wjx.kablade.network;

import com.wjx.kablade.client.MagChaosBladeFxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record MagChaosBladeFxPacket(int entityId, int duration,
                                    double x, double y, double z, float yaw) {

    public static void encode(MagChaosBladeFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.duration);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.yaw);
    }

    public static MagChaosBladeFxPacket decode(FriendlyByteBuf buf) {
        return new MagChaosBladeFxPacket(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat());
    }

    public static void handle(MagChaosBladeFxPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MagChaosBladeFxRenderer.start(packet.entityId, packet.duration,
                        packet.x, packet.y, packet.z, packet.yaw));
        context.setPacketHandled(true);
    }
}

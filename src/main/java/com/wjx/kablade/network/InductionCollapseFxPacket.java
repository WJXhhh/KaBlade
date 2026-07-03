package com.wjx.kablade.network;

import com.wjx.kablade.client.InductionCollapseLightningRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record InductionCollapseFxPacket(int entityId, int duration) {

    public static void encode(InductionCollapseFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.duration);
    }

    public static InductionCollapseFxPacket decode(FriendlyByteBuf buf) {
        return new InductionCollapseFxPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(InductionCollapseFxPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> InductionCollapseLightningRenderer.start(packet.entityId, packet.duration));
        context.setPacketHandled(true);
    }
}

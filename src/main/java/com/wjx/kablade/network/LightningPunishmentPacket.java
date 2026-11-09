package com.wjx.kablade.network;

import com.wjx.kablade.client.LightningPunishmentRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record LightningPunishmentPacket(double x, double y, double z) {
    public static void encode(LightningPunishmentPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x); buf.writeDouble(packet.y); buf.writeDouble(packet.z);
    }
    public static LightningPunishmentPacket decode(FriendlyByteBuf buf) {
        return new LightningPunishmentPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    public static void handle(LightningPunishmentPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LightningPunishmentRenderer.start(packet));
        context.setPacketHandled(true);
    }
}

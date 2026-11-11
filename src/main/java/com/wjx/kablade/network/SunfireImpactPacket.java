package com.wjx.kablade.network;

import com.wjx.kablade.client.SunfireImpactRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SunfireImpactPacket(double x, double y, double z) {
    public static void encode(SunfireImpactPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x); buf.writeDouble(packet.y); buf.writeDouble(packet.z);
    }
    public static SunfireImpactPacket decode(FriendlyByteBuf buf) {
        return new SunfireImpactPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    public static void handle(SunfireImpactPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SunfireImpactRenderer.start(packet));
        context.setPacketHandled(true);
    }
}

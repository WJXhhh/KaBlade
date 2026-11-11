package com.wjx.kablade.network;

import com.wjx.kablade.client.SunfireImpactRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.phys.Vec3;

public record SunfireImpactPacket(int castId, int ownerId, int impactAge, Vec3 center, int[] hitIds) {
    public static void encode(SunfireImpactPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.castId); buf.writeVarInt(packet.ownerId); buf.writeVarInt(packet.impactAge);
        buf.writeDouble(packet.center.x); buf.writeDouble(packet.center.y); buf.writeDouble(packet.center.z);
        buf.writeVarIntArray(packet.hitIds);
    }
    public static SunfireImpactPacket decode(FriendlyByteBuf buf) {
        return new SunfireImpactPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readVarIntArray(4096));
    }
    public static void handle(SunfireImpactPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SunfireImpactRenderer.start(packet));
        context.setPacketHandled(true);
    }
}

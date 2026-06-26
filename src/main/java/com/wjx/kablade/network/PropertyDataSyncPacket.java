package com.wjx.kablade.network;

import com.wjx.kablade.init.KabladeCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 将服务端玩家属性数据全量同步到客户端。
 * <p>
 * 由 {@link com.wjx.kablade.event.PlayerPropertyDataHandler} 在玩家 tick 时发送。
 */
public record PropertyDataSyncPacket(int playerId, CompoundTag data) {

    public static void encode(PropertyDataSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.playerId);
        buf.writeNbt(packet.data);
    }

    public static PropertyDataSyncPacket decode(FriendlyByteBuf buf) {
        return new PropertyDataSyncPacket(buf.readVarInt(), buf.readNbt());
    }

    public static void handle(PropertyDataSyncPacket packet, NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity e = level.getEntity(packet.playerId());
            if (!(e instanceof Player player)) return;
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.load(packet.data()));
        });
        context.setPacketHandled(true);
    }
}

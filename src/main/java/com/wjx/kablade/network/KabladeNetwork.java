package com.wjx.kablade.network;

import com.wjx.kablade.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class KabladeNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private KabladeNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(OripursuitLockPacket.class, 0)
                .encoder(OripursuitLockPacket::encode)
                .decoder(OripursuitLockPacket::decode)
                .consumerMainThread((packet, context) -> OripursuitLockPacket.handle(packet, context.get()))
                .add();

        CHANNEL.messageBuilder(PropertyDataSyncPacket.class, 1)
                .encoder(PropertyDataSyncPacket::encode)
                .decoder(PropertyDataSyncPacket::decode)
                .consumerMainThread((packet, context) -> PropertyDataSyncPacket.handle(packet, context.get()))
                .add();
    }
}

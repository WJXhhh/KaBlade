package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageTestFeatureAuth implements IMessage {
    public String token;

    public MessageTestFeatureAuth() {
    }

    public MessageTestFeatureAuth(String token) {
        this.token = token;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.token = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, token);
    }
}

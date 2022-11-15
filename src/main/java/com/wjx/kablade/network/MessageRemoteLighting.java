package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageRemoteLighting implements IMessage {
    public int x,y,z;

    public MessageRemoteLighting() {
    }

    public MessageRemoteLighting(int xIn, int yIn, int zIn){
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }
}

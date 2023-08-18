package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageDizuiKuo implements IMessage {

    public int id;

    public MessageDizuiKuo(){

    }

    public MessageDizuiKuo(int idIn){
        this.id = idIn;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        this.id = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
    }
}

package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageRePushPotion implements IMessage {
    public int entityId;
    public int id;
    public int duration;
    public byte amplifier;

    public MessageRePushPotion(){

    }

    public MessageRePushPotion(Entity e,int id,int duration,int amplifier){
        this.entityId = e.getEntityId();
        this.id = id;
        this.duration = duration;
        this.amplifier = (byte) amplifier;
    }


    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.id = buf.readInt();
        this.duration = buf.readInt();
        this.amplifier = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.id);
        buf.writeInt(this.duration);
        buf.writeByte(this.amplifier);
    }
}

package com.wjx.kablade.network;
import io.netty.buffer.ByteBuf;import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
public class MessageRaidenCycloneEnd implements IMessage{public static final byte COMPLETE=0,OWNER_LOST=1,TARGET_LOST=2;public long castId;public byte reason;public MessageRaidenCycloneEnd(){}public MessageRaidenCycloneEnd(long id,byte reason){castId=id;this.reason=reason;}public void fromBytes(ByteBuf b){castId=b.readLong();reason=b.readByte();}public void toBytes(ByteBuf b){b.writeLong(castId);b.writeByte(reason);}}

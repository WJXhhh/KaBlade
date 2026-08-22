package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageRaidenCycloneStart implements IMessage {
    public long castId,startGameTime,seed; public int casterId,targetId; public double ox,oy,oz,tx,ty,tz; public float rotation;
    public MessageRaidenCycloneStart(){}
    public MessageRaidenCycloneStart(long castId,int casterId,int targetId,long startGameTime,long seed,double ox,double oy,double oz,double tx,double ty,double tz,float rotation){this.castId=castId;this.casterId=casterId;this.targetId=targetId;this.startGameTime=startGameTime;this.seed=seed;this.ox=ox;this.oy=oy;this.oz=oz;this.tx=tx;this.ty=ty;this.tz=tz;this.rotation=rotation;}
    public void fromBytes(ByteBuf b){castId=b.readLong();casterId=b.readInt();targetId=b.readInt();startGameTime=b.readLong();seed=b.readLong();ox=b.readDouble();oy=b.readDouble();oz=b.readDouble();tx=b.readDouble();ty=b.readDouble();tz=b.readDouble();rotation=b.readFloat();}
    public void toBytes(ByteBuf b){b.writeLong(castId);b.writeInt(casterId);b.writeInt(targetId);b.writeLong(startGameTime);b.writeLong(seed);b.writeDouble(ox);b.writeDouble(oy);b.writeDouble(oz);b.writeDouble(tx);b.writeDouble(ty);b.writeDouble(tz);b.writeFloat(rotation);}
}

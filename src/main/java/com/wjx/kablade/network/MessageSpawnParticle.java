package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageSpawnParticle implements IMessage {
    public double x,y,z,sX,sY,sZ;
    public int particleID;

    public MessageSpawnParticle(){

    }

    public MessageSpawnParticle(EnumParticleTypes particleTypes,double xIn,double yIn,double zIn){
        this.x = xIn;
        this.y =yIn;
        this.z = zIn;
        this.sX = 0;
        this.sY = 0;
        this.sZ = 0;
        particleID = particleTypes.getParticleID();
    }

    public MessageSpawnParticle(EnumParticleTypes particleTypes,double xIn,double yIn,double zIn,double sXIn,double sYIn,double sZIn){
        this.x = xIn;
        this.y =yIn;
        this.z = zIn;
        this.sX = sXIn;
        this.sY = sYIn;
        this.sZ = sZIn;
        particleID = particleTypes.getParticleID();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        sX = buf.readDouble();
        sY = buf.readDouble();
        sZ = buf.readDouble();
        particleID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(sX);
        buf.writeDouble(sY);
        buf.writeDouble(sZ);
        buf.writeInt(particleID);
    }
}

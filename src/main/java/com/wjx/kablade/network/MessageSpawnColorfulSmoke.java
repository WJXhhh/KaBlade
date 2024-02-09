package com.wjx.kablade.network;

import com.wjx.kablade.util.Vec3f;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageSpawnColorfulSmoke implements IMessage {
    public double x,y,z;
    public float r,g,b;
    public int life;
    public MessageSpawnColorfulSmoke(){

    }

    public MessageSpawnColorfulSmoke(double xIn, double yIn, double zIn, Vec3f rgb, int life){
        this.x = xIn;
        this.y =yIn;
        this.z = zIn;
        this.r = rgb.x;
        this.g = rgb.y;
        this.b = rgb.z;
        this.life = life;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        r= buf.readFloat();
        g=buf.readFloat();
        b=buf.readFloat();
        life = buf.readInt();

    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeInt(life);
    }
}

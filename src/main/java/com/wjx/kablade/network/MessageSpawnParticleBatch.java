package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageSpawnParticleBatch implements IMessage {
    public List<ParticleEntry> particles = new ArrayList<>();

    public static class ParticleEntry {
        public int particleID;
        public double x, y, z, sX, sY, sZ;

        public ParticleEntry(int particleID, double x, double y, double z, double sX, double sY, double sZ) {
            this.particleID = particleID;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sX = sX;
            this.sY = sY;
            this.sZ = sZ;
        }
    }

    public MessageSpawnParticleBatch() {}

    public void add(int particleID, double x, double y, double z, double sX, double sY, double sZ) {
        particles.add(new ParticleEntry(particleID, x, y, z, sX, sY, sZ));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        particles.clear();
        for (int i = 0; i < count; i++) {
            int pid = buf.readInt();
            double px = buf.readDouble();
            double py = buf.readDouble();
            double pz = buf.readDouble();
            double psX = buf.readDouble();
            double psY = buf.readDouble();
            double psZ = buf.readDouble();
            particles.add(new ParticleEntry(pid, px, py, pz, psX, psY, psZ));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(particles.size());
        for (ParticleEntry e : particles) {
            buf.writeInt(e.particleID);
            buf.writeDouble(e.x);
            buf.writeDouble(e.y);
            buf.writeDouble(e.z);
            buf.writeDouble(e.sX);
            buf.writeDouble(e.sY);
            buf.writeDouble(e.sZ);
        }
    }
}

package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageSpawnParticleBurst implements IMessage {
    public double centerX, centerY, centerZ;
    public List<BurstGroup> groups = new ArrayList<>();

    public static class BurstGroup {
        public int particleID;
        public int count;
        public double rangeX, rangeY, rangeZ;
        public boolean colorful;
        public float r, g, b;
        public int life;

        public BurstGroup(int particleID, int count, double rangeX, double rangeY, double rangeZ) {
            this.particleID = particleID;
            this.count = count;
            this.rangeX = rangeX;
            this.rangeY = rangeY;
            this.rangeZ = rangeZ;
        }

        public BurstGroup(int count, double rangeX, double rangeY, double rangeZ, float r, float g, float b, int life) {
            this.particleID = -1;
            this.count = count;
            this.rangeX = rangeX;
            this.rangeY = rangeY;
            this.rangeZ = rangeZ;
            this.colorful = true;
            this.r = r;
            this.g = g;
            this.b = b;
            this.life = life;
        }
    }

    public MessageSpawnParticleBurst() {}

    public MessageSpawnParticleBurst(double cx, double cy, double cz) {
        this.centerX = cx;
        this.centerY = cy;
        this.centerZ = cz;
    }

    public void addGroup(int particleID, int count, double rangeX, double rangeY, double rangeZ) {
        groups.add(new BurstGroup(particleID, count, rangeX, rangeY, rangeZ));
    }

    public void addColorfulGroup(int count, double rangeX, double rangeY, double rangeZ, float r, float g, float b, int life) {
        groups.add(new BurstGroup(count, rangeX, rangeY, rangeZ, r, g, b, life));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        centerX = buf.readDouble();
        centerY = buf.readDouble();
        centerZ = buf.readDouble();
        int gCount = buf.readInt();
        groups.clear();
        for (int i = 0; i < gCount; i++) {
            int pid = buf.readInt();
            int cnt = buf.readInt();
            double rx = buf.readDouble();
            double ry = buf.readDouble();
            double rz = buf.readDouble();
            boolean cf = buf.readBoolean();
            BurstGroup g;
            if (cf) {
                float cr = buf.readFloat();
                float cg = buf.readFloat();
                float cb = buf.readFloat();
                int cl = buf.readInt();
                g = new BurstGroup(cnt, rx, ry, rz, cr, cg, cb, cl);
                g.particleID = pid;
            } else {
                g = new BurstGroup(pid, cnt, rx, ry, rz);
            }
            groups.add(g);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(centerX);
        buf.writeDouble(centerY);
        buf.writeDouble(centerZ);
        buf.writeInt(groups.size());
        for (BurstGroup g : groups) {
            buf.writeInt(g.particleID);
            buf.writeInt(g.count);
            buf.writeDouble(g.rangeX);
            buf.writeDouble(g.rangeY);
            buf.writeDouble(g.rangeZ);
            buf.writeBoolean(g.colorful);
            if (g.colorful) {
                buf.writeFloat(g.r);
                buf.writeFloat(g.g);
                buf.writeFloat(g.b);
                buf.writeInt(g.life);
            }
        }
    }
}

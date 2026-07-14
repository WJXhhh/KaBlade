package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageSpawnParticleRing implements IMessage {
    public double centerX, centerY, centerZ, startAngle;
    public List<RingGroup> groups = new ArrayList<>();

    public static class RingGroup {
        public int particleID;
        public double posRadius, speedRadius, yOffset, speedFactor, angleStep;
        public int count;

        public RingGroup(int particleID, double posRadius, double speedRadius, double yOffset, double speedFactor, int count, double angleStep) {
            this.particleID = particleID;
            this.posRadius = posRadius;
            this.speedRadius = speedRadius;
            this.yOffset = yOffset;
            this.speedFactor = speedFactor;
            this.count = count;
            this.angleStep = angleStep;
        }
    }

    public MessageSpawnParticleRing() {}

    public MessageSpawnParticleRing(double cx, double cy, double cz, double startAngle) {
        this.centerX = cx;
        this.centerY = cy;
        this.centerZ = cz;
        this.startAngle = startAngle;
    }

    public void addGroup(int particleID, double posRadius, double speedRadius, double yOffset, double speedFactor, int count, double angleStep) {
        groups.add(new RingGroup(particleID, posRadius, speedRadius, yOffset, speedFactor, count, angleStep));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        centerX = buf.readDouble();
        centerY = buf.readDouble();
        centerZ = buf.readDouble();
        startAngle = buf.readDouble();
        int gCount = buf.readInt();
        groups.clear();
        for (int i = 0; i < gCount; i++) {
            int pid = buf.readInt();
            double pr = buf.readDouble();
            double sr = buf.readDouble();
            double yo = buf.readDouble();
            double sf = buf.readDouble();
            int cnt = buf.readInt();
            double as = buf.readDouble();
            groups.add(new RingGroup(pid, pr, sr, yo, sf, cnt, as));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(centerX);
        buf.writeDouble(centerY);
        buf.writeDouble(centerZ);
        buf.writeDouble(startAngle);
        buf.writeInt(groups.size());
        for (RingGroup g : groups) {
            buf.writeInt(g.particleID);
            buf.writeDouble(g.posRadius);
            buf.writeDouble(g.speedRadius);
            buf.writeDouble(g.yOffset);
            buf.writeDouble(g.speedFactor);
            buf.writeInt(g.count);
            buf.writeDouble(g.angleStep);
        }
    }
}

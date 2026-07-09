package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageHandlerSpawnParticleRing implements IMessageHandler<MessageSpawnParticleRing, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageSpawnParticleRing message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            World world = Minecraft.getMinecraft().world;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                double a = message.startAngle;
                for (MessageSpawnParticleRing.RingGroup g : message.groups) {
                    for (int i = 0; i < g.count; i++) {
                        double rad = Math.toRadians(a);
                        double px = message.centerX + Math.cos(rad) * g.posRadius;
                        double py = message.centerY + g.yOffset;
                        double pz = message.centerZ + Math.sin(rad) * g.posRadius;
                        double sX = Math.cos(rad) * g.speedFactor;
                        double sZ = Math.sin(rad) * g.speedRadius * g.speedFactor;
                        world.spawnParticle(EnumParticleTypes.getParticleFromId(g.particleID), px, py, pz, sX, 0, sZ);
                        a += g.angleStep;
                    }
                }
            });
        }
        return null;
    }
}

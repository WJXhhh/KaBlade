package com.wjx.kablade.network;

import com.wjx.kablade.particle.ParticleColorfulSmoke;
import com.wjx.kablade.util.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class MessageHandlerSpawnParticleBurst implements IMessageHandler<MessageSpawnParticleBurst, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageSpawnParticleBurst message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            World world = Minecraft.getMinecraft().world;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Random rand = new Random();
                for (MessageSpawnParticleBurst.BurstGroup g : message.groups) {
                    for (int i = 0; i < g.count; i++) {
                        double px = message.centerX + (rand.nextDouble() - 0.5) * 2 * g.rangeX;
                        double py = message.centerY + rand.nextDouble() * g.rangeY;
                        double pz = message.centerZ + (rand.nextDouble() - 0.5) * 2 * g.rangeZ;
                        if (g.colorful) {
                            Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleColorfulSmoke(world, px, py, pz, new Vec3f(g.r, g.g, g.b), g.life));
                        } else {
                            world.spawnParticle(EnumParticleTypes.getParticleFromId(g.particleID), px, py, pz, 0, 0, 0);
                        }
                    }
                }
            });
        }
        return null;
    }
}

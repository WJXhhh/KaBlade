package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageHandlerSpawnParticleBatch implements IMessageHandler<MessageSpawnParticleBatch, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageSpawnParticleBatch message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            World world = Minecraft.getMinecraft().world;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                for (MessageSpawnParticleBatch.ParticleEntry e : message.particles) {
                    world.spawnParticle(EnumParticleTypes.getParticleFromId(e.particleID), e.x, e.y, e.z, e.sX, e.sY, e.sZ);
                }
            });
        }
        return null;
    }
}

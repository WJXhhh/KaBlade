package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MessageHandlerSpawnParticle implements IMessageHandler<MessageSpawnParticle, IMessage> {
    @Override
    public IMessage onMessage(MessageSpawnParticle message, MessageContext ctx) {
        if(ctx.side == Side.CLIENT)
        {
            World world = Minecraft.getMinecraft().world;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                world.spawnParticle(EnumParticleTypes.getParticleFromId(message.particleID), message.x, message.y, message.z, message.sX, message.sY, message.sZ);
            });
        }
        return null;
    }
}

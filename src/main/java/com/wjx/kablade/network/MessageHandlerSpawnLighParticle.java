package com.wjx.kablade.network;

import com.wjx.kablade.util.ParticleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageHandlerSpawnLighParticle implements IMessageHandler<MessageSpawnLighParticleOn, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageSpawnLighParticleOn message, MessageContext ctx) {
        if(ctx.side == Side.CLIENT)
        {
            World world = Minecraft.getMinecraft().world;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ParticleManager.spawnSmallLightParticle(message.x, message.y, message.z, message.sX, message.sY, message.sZ);
            });
        }
        return null;
    }
}

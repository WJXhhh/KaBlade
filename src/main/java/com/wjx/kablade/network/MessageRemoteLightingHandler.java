package com.wjx.kablade.network;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageRemoteLightingHandler implements IMessageHandler<MessageRemoteLighting,IMessage> {
    @Override
    public IMessage onMessage(MessageRemoteLighting message, MessageContext ctx) {
        int x = message.x;
        int y = message.y;
        int z = message.z;
        WorldServer world = ctx.getServerHandler().player.getServerWorld();
        world.spawnEntity(new EntityLightningBolt(world,x,y,z,true));
        return null;
    }
}

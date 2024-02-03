package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageRemoteLightingHandler implements IMessageHandler<MessageRemoteLighting,IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageRemoteLighting message, MessageContext ctx) {
        int x = message.x;
        int y = message.y;
        int z = message.z;
        World world = Minecraft.getMinecraft().world;
        world.addWeatherEffect(new EntityLightningBolt(world,x,y,z,true));
        return null;
    }
}

package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MessageHandlerUpdateKaBladePlayerProp implements IMessageHandler<MessageUpdateKaBladePlayerProp, IMessage> {
    @Override
    public IMessage onMessage(MessageUpdateKaBladePlayerProp message, MessageContext ctx) {
        if(ctx.side == Side.CLIENT)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                World world = Minecraft.getMinecraft().world;
                Entity e = world.getEntityByID(message.entityID);
                if (e != null && !e.isDead) {
                    e.getEntityData().setTag("kablade_player_property", message.compound);
                }
            });
        }
        return null;
    }
}

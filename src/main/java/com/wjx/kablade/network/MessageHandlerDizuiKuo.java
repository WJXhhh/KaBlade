package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.collection.parallel.ParIterableLike;

public class MessageHandlerDizuiKuo implements IMessageHandler<MessageDizuiKuo, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageDizuiKuo message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(()->{
            Entity e = Minecraft.getMinecraft().player.world.getEntityByID(message.id);
            if (e != null) {
                e.getEntityData().setInteger("dizuitime", 300);
                e.getEntityData().setBoolean("dizui",true);
            }
        });
        return null;
    }
}

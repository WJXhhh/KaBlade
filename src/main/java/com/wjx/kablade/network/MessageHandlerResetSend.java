package com.wjx.kablade.network;

import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MessageHandlerResetSend implements IMessageHandler<MessageResetSend, IMessage> {
    @Override
    public IMessage onMessage(MessageResetSend message, MessageContext ctx) {
        if(ctx.side == Side.CLIENT){
            Minecraft.getMinecraft().addScheduledTask(()->{
                Main.hasSendMessage = false;
            });
        }
        return null;
    }
}

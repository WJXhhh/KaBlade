package com.wjx.kablade.network;

import com.wjx.kablade.util.TestFeatureTokenAuth;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageHandlerTestFeatureAuth implements IMessageHandler<MessageTestFeatureAuth, IMessage> {
    @Override
    public IMessage onMessage(final MessageTestFeatureAuth message, final MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                TestFeatureTokenAuth.authorize(player.getUniqueID(), message.token);
            }
        });
        return null;
    }
}

package com.wjx.kablade.network;
import com.wjx.kablade.client.renderer.RaidenCycloneRenderer;import net.minecraft.client.Minecraft;import net.minecraftforge.fml.common.network.simpleimpl.*;import net.minecraftforge.fml.relauncher.*;
public class MessageRaidenCycloneEndHandler implements IMessageHandler<MessageRaidenCycloneEnd,IMessage>{@SideOnly(Side.CLIENT)public IMessage onMessage(final MessageRaidenCycloneEnd m,MessageContext c){Minecraft.getMinecraft().addScheduledTask(new Runnable(){public void run(){RaidenCycloneRenderer.stop(m.castId,m.reason);}});return null;}}

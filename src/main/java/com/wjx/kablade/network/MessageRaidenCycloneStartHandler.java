package com.wjx.kablade.network;
import com.wjx.kablade.client.renderer.RaidenCycloneRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.*;
public class MessageRaidenCycloneStartHandler implements IMessageHandler<MessageRaidenCycloneStart,IMessage>{@SideOnly(Side.CLIENT) public IMessage onMessage(final MessageRaidenCycloneStart m,MessageContext c){Minecraft.getMinecraft().addScheduledTask(new Runnable(){public void run(){RaidenCycloneRenderer.start(m);}});return null;}}

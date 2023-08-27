package com.wjx.kablade.network;

import com.wjx.kablade.util.special_render.MagChaosBladeEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageHandlerMagChaosBladeEffectUpdate implements IMessageHandler<MessageMagChaosBladeEffectUpdate, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageMagChaosBladeEffectUpdate message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(()->{
            MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.clear();
            for (String s : message.list.getKeySet()){
                NBTTagCompound compound = message.list.getCompoundTag(s);
                MagChaosBladeEffectRenderer renderer = new MagChaosBladeEffectRenderer(compound.getInteger("id"));
                renderer.exitTick = compound.getInteger("tick");
                MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.add(renderer);
            }
        });
        return null;
    }
}

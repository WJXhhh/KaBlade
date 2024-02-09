package com.wjx.kablade.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Objects;

public class MessageHandlerRePushPotion implements IMessageHandler<MessageRePushPotion, IMessage> {
    @Override
    public IMessage onMessage(MessageRePushPotion message, MessageContext ctx) {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(()->{
            World world = ctx.getServerHandler().player.world;
            Entity e = world.getEntityByID(message.entityId);
            if(e instanceof EntityLivingBase){
                ((EntityLivingBase) e).addPotionEffect(new PotionEffect(Objects.requireNonNull(Potion.getPotionById(message.id)), message.duration, message.amplifier));
            }
        });
        return null;
    }
}

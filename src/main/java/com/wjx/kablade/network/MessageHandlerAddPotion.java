package com.wjx.kablade.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MessageHandlerAddPotion implements IMessageHandler<MessageAddPotion, IMessage> {
    @Override
    public IMessage onMessage(MessageAddPotion message, MessageContext ctx) {
        if(ctx.side == Side.CLIENT){
            Minecraft.getMinecraft().addScheduledTask(() -> {
                World world = Minecraft.getMinecraft().world;
                EntityLivingBase entityLivingBase = (EntityLivingBase) world.getEntityByID(message.entityID);
                if (entityLivingBase != null)
                    entityLivingBase.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation(message.potionName),message.time,message.level));
            });
        }
        return null;
    }
}

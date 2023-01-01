package com.wjx.kablade.network;

import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.capability.CapabilityLoader;
import com.wjx.kablade.capability.inters.IPotionInSlash;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageHandlerSlashPotion implements IMessageHandler<MessageSlashPotion, IMessage> {
    @Override
    public IMessage onMessage(MessageSlashPotion message, MessageContext ctx) {
        NBTBase nbt = message.nbtTagCompound.getTag("slash_potion");
        Minecraft.getMinecraft().addScheduledTask(() -> {
            World world = Minecraft.getMinecraft().world;
            Entity e = world.getEntityByID(message.entityID);
            if (e.hasCapability(CapabilityLoader.SlashPotion,null)){
                IPotionInSlash p = e.getCapability(CapabilityLoader.SlashPotion,null);
                Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                storage.readNBT(CapabilityLoader.SlashPotion,p,null,nbt);
            }
        });
        return null;
    }
}

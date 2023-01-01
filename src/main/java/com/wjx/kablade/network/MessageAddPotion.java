package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageAddPotion implements IMessage {
    public int entityID;
    public int level;
    public int time;
    public String potionName;

    public MessageAddPotion() {
    }

    public MessageAddPotion(EntityLivingBase entityLivingBase, Potion potion, int levelIn, int timeIn){
        this.entityID = entityLivingBase.getEntityId();
        level = levelIn;
        time = timeIn;
        potionName = potion.getRegistryName().toString();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        level = buf.readInt();
        time = buf.readInt();
        potionName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeInt(level);
        buf.writeInt(time);
        ByteBufUtils.writeUTF8String(buf,potionName);
    }
}

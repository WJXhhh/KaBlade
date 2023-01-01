package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageSlashPotion implements IMessage {
    public NBTTagCompound nbtTagCompound;
    public int entityID = 0;

    public MessageSlashPotion(){

    }

    public MessageSlashPotion(NBTTagCompound compound){
        nbtTagCompound = compound;
    }

    public MessageSlashPotion(NBTTagCompound compound,Entity entity){
        nbtTagCompound = compound;
        entityID = entity.getEntityId();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        nbtTagCompound = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        ByteBufUtils.writeTag(buf,nbtTagCompound);
    }
}

package com.wjx.kablade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageUpdateKaBladeProp implements IMessage {
    public int entityID;
    public NBTTagCompound compound;

    public MessageUpdateKaBladeProp(){

    }

    public MessageUpdateKaBladeProp(int idIn,NBTTagCompound n2){
        entityID = idIn;
        compound = n2;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        compound = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        ByteBufUtils.writeTag(buf,compound);
    }
}

package com.wjx.kablade.network;

import com.wjx.kablade.util.special_render.MagChaosBladeEffectRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageMagChaosBladeEffectUpdate implements IMessage {
    public NBTTagCompound list = new NBTTagCompound();
    public MessageMagChaosBladeEffectUpdate(){
        int i = 0;
        for (MagChaosBladeEffectRenderer renderer : MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers){
            NBTTagCompound compound = new NBTTagCompound();
            compound.setInteger("id",renderer.playerID);
            compound.setInteger("tick",renderer.exitTick);
            list.setTag("c" + i,compound);
            i++;
        }
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        list = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf,list);
    }
}

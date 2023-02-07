package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageUpdateKaBladePlayerProp;
import com.wjx.kablade.network.MessageUpdateKaBladeProp;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class KaBladePlayerProp {
    public static String TURBULENCE = "turbulence";

    public static void initNBT(EntityPlayer e){
        if (!e.getEntityData().hasKey("kablade_player_property")){
            e.getEntityData().setTag("kablade_player_property",new NBTTagCompound());
        }
    }

    public static NBTTagCompound getPropCompound(EntityPlayer e){
        initNBT(e);
        return e.getEntityData().getCompoundTag("kablade_player_property");
    }

    public static void updateNBTForClient(EntityPlayer e){
        Main.PACKET_HANDLER.sendToAll(new MessageUpdateKaBladePlayerProp(e.getEntityId(),getPropCompound(e)));
    }


}

package com.wjx.kablade.util;

import com.google.common.collect.Lists;
import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageUpdateKaBladeProp;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;

public class KaBladeProperties {
    public static final String PROP_WINE_BIND = "wine_bind";
    public static final String PROP_WINE_BIND_ATTACKER = "wine_bind_attacker";

    public static void initNBT(Entity e){
        if (!e.getEntityData().hasKey("kablade_property")){
            e.getEntityData().setTag("kablade_property",new NBTTagCompound());
        }
    }

    public static boolean checkNBT(Entity e){
        return e.getEntityData().hasKey("kablade_property");
    }

    public static NBTTagCompound getPropCompound(Entity e){
        initNBT(e);
        return e.getEntityData().getCompoundTag("kablade_property");
    }

    public static void updateNBTForClient(Entity e){
        Main.PACKET_HANDLER.sendToAll(new MessageUpdateKaBladeProp(e.getEntityId(),getPropCompound(e)));
    }

}

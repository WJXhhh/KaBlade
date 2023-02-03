package com.wjx.kablade.util;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

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
}

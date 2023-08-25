package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageUpdateKaBladePlayerProp;
import com.wjx.kablade.network.MessageUpdateKaBladeProp;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Locale;

public class KaBladePlayerProp {
    public static final String TURBULENCE = "turbulence";
    public static final String LOCKING_ENTITY_UUID = "locking_entity_uuid";
    public static final String LOCKING_ENTITY_LEFT_TIME = "locking_entity_left_time";
    public static final String MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK = "mag_chaos_blade_extra_attack_tick";
    public static final String GLACIAL_BANE_EXTRA_TICK = "GLACIAL_BANE_EXTRA_TICK".toLowerCase(Locale.ROOT);

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

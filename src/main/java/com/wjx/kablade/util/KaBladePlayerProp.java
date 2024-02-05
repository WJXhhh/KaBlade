package com.wjx.kablade.util;

import com.google.common.collect.Lists;
import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageUpdateKaBladePlayerProp;
import javafx.util.Pair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import scala.Int;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class KaBladePlayerProp {
    public static final String TURBULENCE = "turbulence";
    public static final String LOCKING_ENTITY_UUID = "locking_entity_uuid";
    public static final String LOCKING_ENTITY_LEFT_TIME = "locking_entity_left_time";
    public static final String MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK = "mag_chaos_blade_extra_attack_tick";
    public static final String GLACIAL_BANE_EXTRA_TICK = "GLACIAL_BANE_EXTRA_TICK".toLowerCase(Locale.ROOT);
    public static final String KAMI_OF_WAR_COUNT = "KAMI_OF_WAR_COUNT".toLowerCase(Locale.ROOT);//
    public static final String KAMI_OF_WAR_TICK = "KAMI_OF_WAR_TICK".toLowerCase(Locale.ROOT);
    public static final String WIND_ENCHANTMENT_BOOST = "WIND_ENCHANTMENT_BOOST".toLowerCase(Locale.ROOT);//


    public static final String RAGING_IZUMO_COLD_DOWN = "RAGING_IZUMO_COLD_DOWN".toLowerCase(Locale.ROOT);

    public static final String FAIR_POW="FAIR_POW".toLowerCase(Locale.ROOT);//


    public static HashMap<String,Integer> Bufftimes = new HashMap<>();
    static {
        Bufftimes.put(KAMI_OF_WAR_COUNT,6);
        Bufftimes.put(WIND_ENCHANTMENT_BOOST,5);
        Bufftimes.put(FAIR_POW,1);
    }

    public static List<String> buffs = Arrays.asList(
            KAMI_OF_WAR_COUNT,
            WIND_ENCHANTMENT_BOOST,
            FAIR_POW);


    public static void initNBT(EntityPlayer e) {
        if (!e.getEntityData().hasKey("kablade_player_property")) {
            e.getEntityData().setTag("kablade_player_property", new NBTTagCompound());
        }
    }

    public static NBTTagCompound getPropCompound(EntityPlayer e) {
        initNBT(e);
        return e.getEntityData().getCompoundTag("kablade_player_property");
    }

    public static void updateNBTForClient(EntityPlayer e) {
        Main.PACKET_HANDLER.sendToAll(new MessageUpdateKaBladePlayerProp(e.getEntityId(), getPropCompound(e)));
    }

    public static String getTrans(String s){
        return "prop."+s;
    }


}

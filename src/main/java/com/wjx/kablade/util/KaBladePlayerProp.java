package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageUpdateKaBladePlayerProp;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.*;

public class KaBladePlayerProp {
    public static final String TURBULENCE = "turbulence";
    public static final String LOCKING_ENTITY_UUID = "locking_entity_uuid";
    public static final String LOCKING_ENTITY_LEFT_TIME = "locking_entity_left_time";
    public static final String LOCKING_ENTITY_LIST = "locking_entity_list";
    public static final String MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK = "mag_chaos_blade_extra_attack_tick";

    public static final int LOCK_MAX_TICK = 600;

    public static void addLockedTarget(NBTTagCompound prop, String uuid) {
        NBTTagList list = getLockingEntityList(prop);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (entry.getString("uuid").equals(uuid)) {
                entry.setInteger("leftTime", LOCK_MAX_TICK);
                return;
            }
        }
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("uuid", uuid);
        entry.setInteger("leftTime", LOCK_MAX_TICK);
        list.appendTag(entry);
    }

    public static Set<String> getLockedUUIDs(NBTTagCompound prop) {
        NBTTagList list = getLockingEntityList(prop);
        Set<String> set = new HashSet<>();
        for (int i = 0; i < list.tagCount(); i++) {
            set.add(list.getCompoundTagAt(i).getString("uuid"));
        }
        return set;
    }

    public static void tickAllLockedTimers(NBTTagCompound prop) {
        NBTTagList list = getLockingEntityList(prop);
        NBTTagList kept = new NBTTagList();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int t = entry.getInteger("leftTime") - 1;
            if (t > 0) {
                entry.setInteger("leftTime", t);
                kept.appendTag(entry);
            }
        }
        prop.setTag(LOCKING_ENTITY_LIST, kept);
    }

    public static boolean hasAnyLockedTarget(NBTTagCompound prop) {
        return getLockingEntityList(prop).tagCount() > 0;
    }

    public static void removeStaleTargets(NBTTagCompound prop, World world) {
        NBTTagList list = getLockingEntityList(prop);
        NBTTagList kept = new NBTTagList();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            String uuid = entry.getString("uuid");
            if (!EntityUUIDManager.getEntitiesFromUUID(uuid, world).isEmpty()) {
                kept.appendTag(entry);
            }
        }
        prop.setTag(LOCKING_ENTITY_LIST, kept);
    }

    public static NBTTagList getLockingEntityList(NBTTagCompound prop) {
        migrateIfNeeded(prop);
        if (!prop.hasKey(LOCKING_ENTITY_LIST)) {
            prop.setTag(LOCKING_ENTITY_LIST, new NBTTagList());
        }
        return prop.getTagList(LOCKING_ENTITY_LIST, 10);
    }

    private static void migrateIfNeeded(NBTTagCompound prop) {
        if (prop.hasKey(LOCKING_ENTITY_UUID)) {
            String oldUuid = prop.getString(LOCKING_ENTITY_UUID);
            int oldTime = prop.getInteger(LOCKING_ENTITY_LEFT_TIME);
            prop.removeTag(LOCKING_ENTITY_UUID);
            prop.removeTag(LOCKING_ENTITY_LEFT_TIME);
            NBTTagList list = new NBTTagList();
            if (!oldUuid.isEmpty() && oldTime > 0) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("uuid", oldUuid);
                entry.setInteger("leftTime", oldTime);
                list.appendTag(entry);
            }
            prop.setTag(LOCKING_ENTITY_LIST, list);
        }
    }

    

    public static final String MAG_CHAOS_BLADE_EXTRA_ATTACK_EX_DAMAGE = "MAG_CHAOS_BLADE_EXTRA_ATTACK_EX_DAMAGE".toLowerCase(Locale.ROOT);
    public static final String GLACIAL_BANE_EXTRA_TICK = "GLACIAL_BANE_EXTRA_TICK".toLowerCase(Locale.ROOT);
    public static final String KAMI_OF_WAR_COUNT = "KAMI_OF_WAR_COUNT".toLowerCase(Locale.ROOT);//
    public static final String KAMI_OF_WAR_TICK = "KAMI_OF_WAR_TICK".toLowerCase(Locale.ROOT);
    public static final String KAMI_OF_WAR_EX_DAMAGE = "KAMI_OF_WAR_EX_DAMAGE".toLowerCase(Locale.ROOT);
    public static final String WIND_ENCHANTMENT_BOOST = "WIND_ENCHANTMENT_BOOST".toLowerCase(Locale.ROOT);//
    public static final String FORESIGHT = "FORESIGHT".toLowerCase(Locale.ROOT);


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

package com.wjx.kablade.util;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.wjx.kablade.util.KaBladeEntityProperties.KABLADE_UUID;

public class EntityUUIDManager {
    public static void addRandomUUIDTOEntity(Entity e){
        KaBladeEntityProperties.initNBT(e);
        KaBladeEntityProperties.getPropCompound(e).setString(KABLADE_UUID, UUID.randomUUID().toString());
    }

    public static boolean hasUUID(Entity e){
        return KaBladeEntityProperties.getPropCompound(e).hasKey(KABLADE_UUID);
    }

    public static List<Entity> getEntitiesFromUUID(String uuid, World world){
        ArrayList<Entity> l = Lists.newArrayList();
        for (Entity e : world.loadedEntityList){
            if (KaBladeEntityProperties.getPropCompound(e).hasKey(KABLADE_UUID)){
                if (KaBladeEntityProperties.getPropCompound(e).getString(KABLADE_UUID).equals(uuid)){
                    l.add(e);
                }
            }
        }
        return l;
    }

    public static String getEntityUUID(Entity e){
        return KaBladeEntityProperties.getPropCompound(e).getString(KABLADE_UUID);
    }
}

package com.wjx.kablade.particle.manager;

import net.minecraft.util.EnumParticleTypes;

import java.util.HashSet;
import java.util.Set;

public class ParticleIDManager {

    private static final Set<Integer> usedIDs = new HashSet<>();
    private static int nextAvailableID = 1000;

    static {
        // 收集已使用的ID
        for (EnumParticleTypes type : EnumParticleTypes.values()) {
            usedIDs.add(type.getParticleID());
        }
    }

    public static int getNextAvailableID() {
        while (usedIDs.contains(nextAvailableID)) {
            nextAvailableID++;
        }
        usedIDs.add(nextAvailableID);
        return nextAvailableID++;
    }

    public static boolean isIDAvailable(int id) {
        return !usedIDs.contains(id);
    }
}

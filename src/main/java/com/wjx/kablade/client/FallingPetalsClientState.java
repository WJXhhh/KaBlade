package com.wjx.kablade.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class FallingPetalsClientState {

    private static final Map<Integer, Long> MARKS = new ConcurrentHashMap<>();

    private FallingPetalsClientState() {
    }

    public static void mark(int entityId, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MARKS.put(entityId, mc.level.getGameTime() + duration);
    }

    public static boolean isMarked(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MARKS.clear();
            return false;
        }
        long now = mc.level.getGameTime();
        MARKS.entrySet().removeIf(entry -> entry.getValue() <= now);
        return MARKS.containsKey(entityId);
    }
}

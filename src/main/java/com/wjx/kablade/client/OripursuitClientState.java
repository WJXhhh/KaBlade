package com.wjx.kablade.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class OripursuitClientState {

    private static int lockedEntityId = -1;

    private OripursuitClientState() {
    }

    public static int getLockedEntityId() {
        return lockedEntityId;
    }

    public static void setLockedEntityId(int entityId) {
        lockedEntityId = entityId;
    }
}

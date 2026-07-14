package com.wjx.kablade.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class SATool {
    public static Entity getEntityToWatch(EntityPlayer player) {
        // 兼容旧 SA 调用点：不再把同一个 AABB 重复查询 9 次。
        return TargetingUtil.resolveTarget(player, player.getHeldItemMainhand(), 30.0D, 8.0D, 8.0D);
    }
}

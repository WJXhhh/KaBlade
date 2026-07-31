package com.wjx.kablade.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;

/**
 * SA 辅助工具类 —— 从 1.12.2 {@code SATool} 移植而来。
 */
public final class SATool {

    private SATool() {
    }

    /**
     * 获取玩家视线方向上最近的可攻击实体（与 1.12.2 逻辑一致）。
     */
    public static Entity getEntityToWatch(LivingEntity watcher) {
        return getTargetToWatch(watcher, 20.0D).map(SaTarget::hitEntity).orElse(null);
    }

    public static Optional<SaTarget> getTargetToWatch(LivingEntity watcher, double distance) {
        return SaTargeting.findInSight(watcher, distance, 0.35D);
    }

    /**
     * Returns the closest harmful living entity intersected by the look ray.
     * This is the precise ray test used by Plasma Kagehide's SA.
     */
    public static LivingEntity getEntityInSight(LivingEntity watcher, double distance) {
        return getTargetInSight(watcher, distance).map(SaTarget::root).orElse(null);
    }

    public static Optional<SaTarget> getTargetInSight(LivingEntity watcher, double distance) {
        return SaTargeting.findInSight(watcher, distance, 0.0D);
    }
}

package com.wjx.kablade.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
        Level level = watcher.level();
        Entity target = null;
        for (int dist = 2; dist < 20; dist += 2) {
            Vec3 look = watcher.getLookAngle();
            double dx = look.x * 3.0D;
            double dy = watcher.getEyeHeight() + look.y * 3.0D;
            double dz = look.z * 3.0D;

            AABB box = watcher.getBoundingBox()
                    .inflate(8.0D, 8.0D, 8.0D)
                    .move(dx, dy, dz);
            List<Entity> list = level.getEntities(watcher, box,
                    entity -> isAttackable(watcher, entity));

            float closest = 30.0f;
            for (Entity cur : list) {
                float d = cur.distanceTo(watcher);
                if (d < closest) {
                    target = cur;
                    closest = d;
                }
            }
            if (target != null) {
                break;
            }
        }
        return target;
    }

    /**
     * Returns the closest harmful living entity intersected by the look ray.
     * This is the precise ray test used by Plasma Kagehide's SA.
     */
    public static LivingEntity getEntityInSight(LivingEntity watcher, double distance) {
        Level level = watcher.level();
        Vec3 eye = watcher.getEyePosition();
        Vec3 end = eye.add(watcher.getLookAngle().scale(distance));
        AABB scanBox = watcher.getBoundingBox()
                .expandTowards(watcher.getLookAngle().scale(distance))
                .inflate(1.0D, 1.0D, 1.0D);

        LivingEntity closest = null;
        double closestDistance = distance;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, scanBox, entity -> isAttackable(watcher, entity))) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hit = box.clip(eye, end);
            double hitDistance;
            if (box.contains(eye)) {
                hitDistance = 0.0D;
            } else if (hit.isPresent()) {
                hitDistance = eye.distanceTo(hit.get());
            } else {
                continue;
            }

            if (hitDistance < closestDistance) {
                closest = candidate;
                closestDistance = hitDistance;
            }
        }
        return closest;
    }

    private static boolean isAttackable(LivingEntity watcher, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (!entity.isPickable() || !SaTargeting.canDamage(watcher, living)) {
            return false;
        }
        return entity instanceof Mob || entity instanceof Player;
    }
}

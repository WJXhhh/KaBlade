package com.wjx.kablade.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A harmful-SA target split into its physical hit box and logical living owner.
 * Multipart bosses need both: permissions belong to the owner, while damage must
 * be delivered to the selected part so the boss can apply its own weak-point rules.
 */
public record SaTarget(Entity hitEntity, LivingEntity root, Vec3 anchor) {

    public SaTarget {
        if (hitEntity == null || root == null || anchor == null) {
            throw new IllegalArgumentException("SA target fields must not be null");
        }
    }

    public static Optional<SaTarget> of(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        Entity current = entity;
        Set<Entity> visited = new HashSet<>();
        while (current instanceof PartEntity<?> part && visited.add(current)) {
            current = part.getParent();
        }
        if (!(current instanceof LivingEntity living)) {
            return Optional.empty();
        }
        return Optional.of(new SaTarget(entity, living, center(entity.getBoundingBox())));
    }

    public UUID damageGroup() {
        return root.getUUID();
    }

    public boolean isAlive() {
        return hitEntity.isAlive() && root.isAlive();
    }

    public boolean isMultipartPart() {
        return hitEntity != root;
    }

    public SaTarget withAnchor(Vec3 value) {
        return new SaTarget(hitEntity, root, value);
    }

    public double distanceToSqr(Vec3 point) {
        return nearestPoint(hitEntity.getBoundingBox(), point).distanceToSqr(point);
    }

    public static Vec3 center(AABB box) {
        return new Vec3((box.minX + box.maxX) * 0.5D,
                (box.minY + box.maxY) * 0.5D,
                (box.minZ + box.maxZ) * 0.5D);
    }

    public static Vec3 nearestPoint(AABB box, Vec3 point) {
        return new Vec3(
                Math.max(box.minX, Math.min(point.x, box.maxX)),
                Math.max(box.minY, Math.min(point.y, box.maxY)),
                Math.max(box.minZ, Math.min(point.z, box.maxZ)));
    }
}

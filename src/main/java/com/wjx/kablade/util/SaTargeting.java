package com.wjx.kablade.util;

import com.wjx.kablade.config.KabladeConfig;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Shared harmful-SA target rules.
 */
public final class SaTargeting {

    private static final TargetSelector.AttackablePredicate SLASHBLADE_ATTACKABLE =
            new TargetSelector.AttackablePredicate();

    private SaTargeting() {
    }

    public static boolean canDamage(Entity owner, LivingEntity target) {
        if(!isAllUseTargetSelector()){
            if (!testBasicAttackable(owner, target))
                return false;
            if (target instanceof Player && filtersPlayers()) {
                return false;
            }
            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return false;
            }
            if (protectsTamedPets() && isProtectedTamedPet(owner, target)) {
                return false;
            }
            return scoreboardAllowsDamage(owner, target);
        }
        else {
            if(testBasicAttackable(owner, target)){
                return SLASHBLADE_ATTACKABLE.test(target);
            }
            return false;
        }
    }

    /** Multipart-aware harmful-target check. */
    public static boolean canDamage(Entity owner, Entity target) {
        return SaTarget.of(target).map(value -> canDamage(owner, value.root())).orElse(false);
    }

    private static boolean testBasicAttackable(Entity owner,LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        return owner == null || target != owner;
    }

    public static boolean canDamageAttackable(Entity owner, LivingEntity target) {
        if (!canDamage(owner, target)) {
            return false;
        }
        return target instanceof Mob || target instanceof Player || SLASHBLADE_ATTACKABLE.test(target);
    }

    /** Multipart-aware scan predicate. */
    public static boolean canDamageAttackable(Entity owner, Entity target) {
        return SaTarget.of(target).map(value -> canDamageAttackable(owner, value.root())).orElse(false);
    }

    /**
     * Resolve an existing SlashBlade lock, then fall back to a direct ray hit and a
     * conservative soft cone. A multipart root lock is refined to the part closest
     * to the user's crosshair.
     */
    public static Optional<SaTarget> findTarget(LivingEntity owner, Entity locked, double range) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = normalizedLook(owner);
        if (locked != null) {
            Optional<SaTarget> resolved = bestFromLocked(owner, locked, eye, look, range);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return findInSight(owner, range, 0.35D);
    }

    /** Find the best attackable physical hit box under or near the crosshair. */
    public static Optional<SaTarget> findInSight(LivingEntity owner, double range, double softCone) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = normalizedLook(owner);
        Vec3 end = eye.add(look.scale(range));
        AABB scan = owner.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);

        SaTarget bestDirect = null;
        double bestDirectDistance = Double.MAX_VALUE;
        SaTarget bestSoft = null;
        double bestSoftScore = Double.MAX_VALUE;

        for (Entity candidate : physicalCandidates(owner.level(), owner, scan)) {
            Optional<SaTarget> resolved = validPhysicalTarget(owner, candidate, range, eye);
            if (resolved.isEmpty()) {
                continue;
            }
            SaTarget target = resolved.get();
            AABB hitBox = candidate.getBoundingBox().inflate(Math.max(0.1D, candidate.getPickRadius()));
            Optional<Vec3> clip = hitBox.clip(eye, end);
            if (hitBox.contains(eye)) {
                return Optional.of(target.withAnchor(eye));
            }
            if (clip.isPresent()) {
                double distance = eye.distanceToSqr(clip.get());
                if (distance < bestDirectDistance) {
                    bestDirectDistance = distance;
                    bestDirect = target.withAnchor(clip.get());
                }
                continue;
            }

            Vec3 anchor = SaTarget.nearestPoint(hitBox, eye);
            Vec3 delta = anchor.subtract(eye);
            double distance = delta.length();
            if (distance <= 1.0E-6D) {
                continue;
            }
            double angularMiss = 1.0D - look.dot(delta.scale(1.0D / distance));
            if (angularMiss <= softCone * softCone * 0.5D) {
                double score = angularMiss * 256.0D + distance;
                if (score < bestSoftScore) {
                    bestSoftScore = score;
                    bestSoft = target.withAnchor(anchor);
                }
            }
        }
        return Optional.ofNullable(bestDirect != null ? bestDirect : bestSoft);
    }

    /**
     * Multipart-aware area scan. Only one physical target is returned per living
     * root, preventing a boss with many parts from taking multiplied pulse damage.
     */
    public static List<SaTarget> uniqueTargets(Level level, Entity owner, AABB area,
                                                boolean requireAttackable) {
        Vec3 center = SaTarget.center(area);
        Map<java.util.UUID, SaTarget> unique = new LinkedHashMap<>();
        for (Entity entity : physicalCandidates(level, owner, area)) {
            Optional<SaTarget> value = SaTarget.of(entity);
            if (value.isEmpty()) {
                continue;
            }
            SaTarget target = value.get();
            if (!target.isAlive() || (!canDamage(owner, target.root()))
                    || (requireAttackable && !canDamageAttackable(owner, target.root()))) {
                continue;
            }
            SaTarget previous = unique.get(target.damageGroup());
            if (previous == null || target.distanceToSqr(center) < previous.distanceToSqr(center)) {
                unique.put(target.damageGroup(), target);
            }
        }
        // Match Level#getEntities* semantics: callers in several timeline skills
        // append a forced primary target after the scan.
        return new ArrayList<>(unique.values());
    }

    public static List<SaTarget> uniqueTargets(Level level, Entity owner, AABB area) {
        return uniqueTargets(level, owner, area, true);
    }

    /** Multipart-preserving area scan with a predicate over the physical target. */
    public static List<SaTarget> targets(Level level, Entity owner, AABB area,
                                         Predicate<SaTarget> predicate) {
        Vec3 center = SaTarget.center(area);
        Map<java.util.UUID, SaTarget> unique = new LinkedHashMap<>();
        for (Entity entity : physicalCandidates(level, owner, area)) {
            Optional<SaTarget> value = SaTarget.of(entity);
            if (value.isEmpty()) {
                continue;
            }
            SaTarget target = value.get();
            // Apply the shape predicate before multipart de-duplication. Otherwise a
            // body/neck outside a narrow slash can hide a head that actually intersects it.
            if (!target.isAlive() || !canDamage(owner, target.root()) || !predicate.test(target)) {
                continue;
            }
            SaTarget previous = unique.get(target.damageGroup());
            if (previous == null || target.distanceToSqr(center) < previous.distanceToSqr(center)) {
                unique.put(target.damageGroup(), target);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /** Choose the physical part nearest a damage origin, retaining normal entities unchanged. */
    public static Entity damageEntity(Entity target, Vec3 origin) {
        if (target == null || !target.isMultipartEntity() || target.getParts() == null
                || target.getParts().length == 0) {
            return target;
        }
        return java.util.Arrays.stream(target.getParts())
                .filter(Entity::isAlive)
                .filter(Entity::isPickable)
                .min(Comparator.comparingDouble(part ->
                        SaTarget.nearestPoint(part.getBoundingBox(), origin).distanceToSqr(origin)))
                .<Entity>map(part -> part)
                .orElse(target);
    }

    private static Optional<SaTarget> bestFromLocked(LivingEntity owner, Entity locked,
                                                      Vec3 eye, Vec3 look, double range) {
        if (locked.isMultipartEntity() && locked.getParts() != null && locked.getParts().length > 0) {
            Vec3 end = eye.add(look.scale(range));
            return java.util.Arrays.stream(locked.getParts())
                    .map(part -> validPhysicalTarget(owner, part, range, eye).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .min(Comparator.comparingDouble(target -> lockedPartScore(target, eye, end)));
        }
        return validPhysicalTarget(owner, locked, range, eye);
    }

    private static double lockedPartScore(SaTarget target, Vec3 eye, Vec3 end) {
        Optional<Vec3> hit = target.hitEntity().getBoundingBox()
                .inflate(Math.max(0.1D, target.hitEntity().getPickRadius())).clip(eye, end);
        return hit.map(vec3 -> eye.distanceToSqr(vec3))
                .orElse(1_000_000.0D + target.distanceToSqr(eye));
    }

    private static Optional<SaTarget> validPhysicalTarget(LivingEntity owner, Entity candidate,
                                                           double range, Vec3 eye) {
        Optional<SaTarget> value = SaTarget.of(candidate);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        SaTarget target = value.get();
        if (!candidate.isPickable() || !target.isAlive()
                || !canDamageAttackable(owner, target.root())
                || target.distanceToSqr(eye) > range * range) {
            return Optional.empty();
        }
        return value;
    }

    private static Collection<Entity> physicalCandidates(Level level, Entity except, AABB area) {
        Map<Entity, Boolean> unique = new IdentityHashMap<>();
        for (Entity entity : level.getEntities(except, area, Entity::isPickable)) {
            unique.put(entity, Boolean.TRUE);
        }
        for (PartEntity<?> part : level.getPartEntities()) {
            if (part != except && part.isPickable() && part.getBoundingBox().intersects(area)) {
                unique.put(part, Boolean.TRUE);
            }
        }
        return new ArrayList<>(unique.keySet());
    }

    private static Vec3 normalizedLook(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        return look.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
    }

    private static boolean filtersPlayers() {
        return !KabladeConfig.SPEC.isLoaded() || KabladeConfig.FILTER_PLAYERS_IN_SA_TARGETING.get();
    }

    private static boolean protectsTamedPets() {
        return !KabladeConfig.SPEC.isLoaded() || KabladeConfig.PROTECT_TAMED_PETS_IN_SA_TARGETING.get();
    }

    private static boolean isAllUseTargetSelector(){
        return !KabladeConfig.SPEC.isLoaded() || KabladeConfig.SA_ALL_USE_TARGET_SELECTOR.get();
    }

    private static boolean isProtectedTamedPet(Entity owner, LivingEntity target) {
        if (owner == null || !(target instanceof TamableAnimal pet) || !pet.isTame()) {
            return false;
        }

        LivingEntity petOwner = pet.getOwner();
        if (petOwner == null) {
            return false;
        }
        if (petOwner == owner) {
            return true;
        }

        if (owner instanceof Player attacker && petOwner instanceof Player playerOwner
                && !attacker.canHarmPlayer(playerOwner)) {
            return true;
        }

        Team ownerTeam = owner.getTeam();
        Team petOwnerTeam = petOwner.getTeam();
        return ownerTeam != null
                && petOwnerTeam != null
                && ownerTeam.isAlliedTo(petOwnerTeam)
                && !ownerTeam.isAllowFriendlyFire();
    }

    private static boolean scoreboardAllowsDamage(Entity owner, LivingEntity target) {
        if (owner == null) {
            return true;
        }
        if (owner instanceof Player attacker && target instanceof Player playerTarget
                && !attacker.canHarmPlayer(playerTarget)) {
            return false;
        }

        Team ownerTeam = owner.getTeam();
        Team targetTeam = target.getTeam();
        return ownerTeam == null
                || targetTeam == null
                || !ownerTeam.isAlliedTo(targetTeam)
                || ownerTeam.isAllowFriendlyFire();
    }
}

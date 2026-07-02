package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 高频坍缩 -- 「高周波切割刀」专属 SA。
 * <p>
 * 释放后向目标水平突进；命中目标会使其陷入 4 秒的高频电弧坍缩：
 * 缓慢 III、蓝色电弧缠身，并每 0.4 秒受到一次伤害。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InductionCollapseArts extends SlashArts {

    private static final Map<UUID, CollapseState> ACTIVE_COLLAPSES = new ConcurrentHashMap<>();
    private static final Map<UUID, LungeState> ACTIVE_LUNGES = new ConcurrentHashMap<>();
    private static final int EFFECT_DURATION = 80;
    private static final int DAMAGE_INTERVAL = 8;
    private static final int SLOW_AMPLIFIER = 2;
    private static final int CONTACT_WINDOW = 40;
    private static final int LUNGE_EXTRA_TICKS = 5;
    private static final int SEARCH_RANGE = 24;
    private static final float PULSE_DAMAGE = 4.0F;
    private static final double MIN_LUNGE_SPEED = 1.1;
    private static final double MAX_LUNGE_SPEED = 2.2;
    private static final Vector3f ELECTRIC_BLUE = new Vector3f(0.05F, 0.55F, 1.0F);

    public InductionCollapseArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        LivingEntity target = findTarget(level, user, blade);
        lockSlashBladeTarget(blade, target);

        Vec3 direction = target != null ? flatDirectionTo(user, target) : SaFx.flatLook(user);
        startLunge(level, user, target, direction);
        spawnReleaseFx(level, user, direction);

        return super.doArts(type, user);
    }

    private static LivingEntity findTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        LivingEntity raycast = raycastTarget(level, user);
        if (raycast != null) {
            return raycast;
        }

        if (SATool.getEntityToWatch(user) instanceof LivingEntity watched
                && isValidTarget(user, watched) && watched.distanceTo(user) <= SEARCH_RANGE) {
            return watched;
        }

        LivingEntity forward = forwardNearestTarget(level, user);
        if (forward != null) {
            return forward;
        }

        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity living && isValidTarget(user, living)
                && living.distanceTo(user) <= SEARCH_RANGE) {
            return living;
        }

        return null;
    }

    private static LivingEntity forwardNearestTarget(ServerLevel level, LivingEntity user) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(SEARCH_RANGE, 4.0, SEARCH_RANGE),
                candidate -> isForwardTarget(user, candidate));
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (!isValidTarget(user, candidate)) {
                continue;
            }
            double dist = candidate.distanceToSqr(user);
            if (dist < closestDist) {
                closest = candidate;
                closestDist = dist;
            }
        }
        return closest;
    }

    private static LivingEntity raycastTarget(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(SEARCH_RANGE));

        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(SEARCH_RANGE))
                .inflate(2.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox,
                candidate -> candidate != user && candidate.isAlive() && candidate.isPickable());

        LivingEntity closest = null;
        double closestDist = SEARCH_RANGE;
        for (LivingEntity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(candidate.getPickRadius() + 0.3);
            var hit = bb.clip(eye, end);
            if (bb.contains(eye)) {
                return candidate;
            } else if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < closestDist) {
                    closest = candidate;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    private static boolean isForwardTarget(LivingEntity user, LivingEntity target) {
        if (!isValidTarget(user, target)) {
            return false;
        }

        Vec3 flatLook = SaFx.flatLook(user);
        double dx = target.getX() - user.getX();
        double dz = target.getZ() - user.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance > SEARCH_RANGE) {
            return false;
        }
        if (horizontalDistance < 3.0) {
            return true;
        }
        return (dx * flatLook.x + dz * flatLook.z) / horizontalDistance >= 0.25;
    }

    private static void lockSlashBladeTarget(ItemStack blade, LivingEntity target) {
        if (target == null) {
            return;
        }
        blade.getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(state -> state.setTargetEntityId(target));
    }

    private static boolean isValidTarget(LivingEntity user, LivingEntity target) {
        if (user == null || target == null || target == user || !target.isAlive()) {
            return false;
        }
        try {
            if (target.isAlliedTo(user)) {
                return false;
            }
        } catch (NullPointerException ignored) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        try {
            return new TargetSelector.AttackablePredicate().test(target);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    private static LivingEntity findContactTarget(ServerLevel level, LivingEntity user, LivingEntity intended,
                                                  AABB box, Set<UUID> alreadyHit) {
        if (intended != null && !alreadyHit.contains(intended.getUUID())
                && isValidTarget(user, intended) && intended.getBoundingBox().intersects(box)) {
            return intended;
        }

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> isValidTarget(user, candidate));
        for (LivingEntity candidate : candidates) {
            if (alreadyHit.contains(candidate.getUUID())) {
                continue;
            }
            double dist = candidate.distanceToSqr(user);
            if (dist < closestDist) {
                closest = candidate;
                closestDist = dist;
            }
        }
        return closest;
    }

    private static Vec3 flatDirectionTo(LivingEntity user, LivingEntity target) {
        Vec3 diff = target.position().subtract(user.position());
        Vec3 flat = new Vec3(diff.x, 0.0, diff.z);
        return flat.lengthSqr() < 1.0e-6 ? SaFx.flatLook(user) : flat.normalize();
    }

    private static void startLunge(ServerLevel level, LivingEntity user, LivingEntity target, Vec3 direction) {
        double speed = lungeSpeed(user, target);
        int boostTicks = lungeBoostTicks(user, target, speed);
        applyLungeVelocity(user, direction, speed);

        long now = level.getServer().getTickCount();
        ACTIVE_LUNGES.put(user.getUUID(), new LungeState(
                level.dimension(),
                user.getUUID(),
                target != null ? target.getUUID() : null,
                direction,
                speed,
                now + CONTACT_WINDOW,
                now + boostTicks,
                user.getBoundingBox()));
    }

    private static double lungeSpeed(LivingEntity user, LivingEntity target) {
        double speed = MIN_LUNGE_SPEED;
        if (target != null) {
            double dist = Math.sqrt(user.distanceToSqr(target));
            speed = Math.min(MAX_LUNGE_SPEED, Math.max(MIN_LUNGE_SPEED, dist / 10.0));
        }
        return speed;
    }

    private static int lungeBoostTicks(LivingEntity user, LivingEntity target, double speed) {
        if (target == null) {
            return 7;
        }
        double dist = Math.sqrt(user.distanceToSqr(target));
        int travelTicks = (int) Math.ceil(dist / Math.max(0.1, speed));
        return Math.min(CONTACT_WINDOW, Math.max(7, travelTicks + LUNGE_EXTRA_TICKS));
    }

    private static void applyLungeVelocity(LivingEntity user, Vec3 direction, double speed) {
        user.setDeltaMovement(direction.x * speed, 0.0, direction.z * speed);
        user.fallDistance = 0.0F;
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    private static void applyCollapse(ServerLevel level, LivingEntity user, LivingEntity target) {
        if (!isValidTarget(user, target) || target.level() != level) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                EFFECT_DURATION, SLOW_AMPLIFIER, false, true));
        long now = level.getServer().getTickCount();
        ACTIVE_COLLAPSES.put(target.getUUID(), new CollapseState(
                level.dimension(), target.getUUID(), user.getUUID(), now + EFFECT_DURATION, now));

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.55F);
        level.sendParticles(ParticleTypes.FLASH,
                target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        spawnElectricArc(level, target, 0);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        tickActiveLunges(event);

        if (event.phase != TickEvent.Phase.END || ACTIVE_COLLAPSES.isEmpty()) {
            return;
        }

        long now = event.getServer().getTickCount();
        ACTIVE_COLLAPSES.entrySet().removeIf(entry -> {
            CollapseState state = entry.getValue();
            ServerLevel level = event.getServer().getLevel(state.dimension);
            if (level == null || now >= state.expiresAt) {
                return true;
            }

            Entity targetEntity = level.getEntity(state.targetUUID);
            if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
                return true;
            }
            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return true;
            }

            LivingEntity owner = null;
            Entity ownerEntity = level.getEntity(state.ownerUUID);
            if (ownerEntity instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                owner = livingOwner;
                if (target.isAlliedTo(owner)) {
                    return true;
                }
            }

            int age = (int) (EFFECT_DURATION - (state.expiresAt - now));
            spawnElectricArc(level, target, age);
            if (now >= state.nextDamageAt) {
                target.hurt(damageSource(level, owner), PULSE_DAMAGE);
                state.nextDamageAt = now + DAMAGE_INTERVAL;
            }
            if (age % 6 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        Math.min(EFFECT_DURATION, (int) (state.expiresAt - now)),
                        SLOW_AMPLIFIER, false, true));
            }
            return false;
        });
    }

    private static void tickActiveLunges(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE_LUNGES.isEmpty()) {
            return;
        }

        long now = event.getServer().getTickCount();
        ACTIVE_LUNGES.entrySet().removeIf(entry -> {
            LungeState state = entry.getValue();
            ServerLevel level = event.getServer().getLevel(state.dimension);
            if (level == null || now >= state.expiresAt) {
                return true;
            }

            Entity ownerEntity = level.getEntity(state.ownerUUID);
            if (!(ownerEntity instanceof LivingEntity user) || !user.isAlive()) {
                return true;
            }

            if (now <= state.boostUntil) {
                applyLungeVelocity(user, state.direction, state.speed);
            } else if (state.triggered) {
                return true;
            }

            AABB current = user.getBoundingBox();
            AABB projected = current.move(state.direction.scale(state.speed * 1.25));
            AABB sweep = state.lastBox.minmax(current).minmax(projected).inflate(2.1, 1.05, 2.1);
            state.lastBox = current;

            LivingEntity intended = null;
            if (state.targetUUID != null) {
                Entity targetEntity = level.getEntity(state.targetUUID);
                if (targetEntity instanceof LivingEntity livingTarget) {
                    intended = livingTarget;
                }
            }

            LivingEntity hit;
            while ((hit = findContactTarget(level, user, intended, sweep, state.hitTargets)) != null) {
                state.hitTargets.add(hit.getUUID());
                applyCollapse(level, user, hit);
                state.triggered = true;
            }

            return false;
        });
    }

    private static DamageSource damageSource(ServerLevel level, LivingEntity owner) {
        if (owner instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        if (owner != null) {
            return level.damageSources().mobAttack(owner);
        }
        return level.damageSources().magic();
    }

    private static final class CollapseState {
        private final ResourceKey<Level> dimension;
        private final UUID targetUUID;
        private final UUID ownerUUID;
        private final long expiresAt;
        private long nextDamageAt;

        private CollapseState(ResourceKey<Level> dimension, UUID targetUUID, UUID ownerUUID,
                              long expiresAt, long nextDamageAt) {
            this.dimension = dimension;
            this.targetUUID = targetUUID;
            this.ownerUUID = ownerUUID;
            this.expiresAt = expiresAt;
            this.nextDamageAt = nextDamageAt;
        }
    }

    private static final class LungeState {
        private final ResourceKey<Level> dimension;
        private final UUID ownerUUID;
        private final UUID targetUUID;
        private final Vec3 direction;
        private final double speed;
        private final long expiresAt;
        private final long boostUntil;
        private AABB lastBox;
        private boolean triggered;
        private final Set<UUID> hitTargets = new HashSet<>();

        private LungeState(ResourceKey<Level> dimension, UUID ownerUUID, UUID targetUUID,
                           Vec3 direction, double speed, long expiresAt, long boostUntil,
                           AABB lastBox) {
            this.dimension = dimension;
            this.ownerUUID = ownerUUID;
            this.targetUUID = targetUUID;
            this.direction = direction;
            this.speed = speed;
            this.expiresAt = expiresAt;
            this.boostUntil = boostUntil;
            this.lastBox = lastBox;
        }
    }

    private static void spawnElectricArc(ServerLevel level, LivingEntity target, int age) {
        double height = target.getBbHeight();
        double radius = Math.max(0.55, target.getBbWidth() * 1.05);
        double cy = target.getY() + height * 0.52;

        for (int arc = 0; arc < 5; arc++) {
            double phase = age * 0.72 + arc * Math.PI * 2.0 / 5.0 + level.random.nextDouble() * 0.55;
            double y = target.getY() + 0.15 + height * (0.12 + 0.76 * ((age + arc * 5) % 19) / 18.0);
            double x0 = target.getX() + Math.cos(phase) * radius;
            double z0 = target.getZ() + Math.sin(phase) * radius;
            double x1 = target.getX() + Math.cos(phase + 1.55) * radius;
            double z1 = target.getZ() + Math.sin(phase + 1.55) * radius;

            for (int step = 0; step <= 6; step++) {
                double t = step / 6.0;
                double bend = Math.sin(t * Math.PI) * (0.20 + level.random.nextDouble() * 0.10);
                double px = x0 + (x1 - x0) * t + Math.cos(phase + Math.PI * 0.5) * bend;
                double pz = z0 + (z1 - z0) * t + Math.sin(phase + Math.PI * 0.5) * bend;
                level.sendParticles(new DustParticleOptions(ELECTRIC_BLUE, 1.15F),
                        px, y + (level.random.nextDouble() - 0.5) * 0.22, pz,
                        2, 0.015, 0.015, 0.015, 0.0);
            }
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), cy, target.getZ(),
                8, radius, height * 0.42, radius, 0.06);
        if (age % 8 == 0) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.55F, 1.85F);
        }
    }

    private static void spawnReleaseFx(ServerLevel level, LivingEntity user, Vec3 direction) {
        Vec3 origin = user.position().add(0.0, user.getBbHeight() * 0.55, 0.0);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.9F, 1.45F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.75F);

        for (int i = 0; i < 14; i++) {
            double step = i * 0.18;
            level.sendParticles(new DustParticleOptions(ELECTRIC_BLUE, 0.9F),
                    origin.x + direction.x * step,
                    origin.y + (level.random.nextDouble() - 0.5) * 0.45,
                    origin.z + direction.z * step,
                    1, 0.04, 0.04, 0.04, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                origin.x + direction.x * 0.9, origin.y, origin.z + direction.z * 0.9,
                8, 0.35, 0.35, 0.35, 0.08);
    }

}

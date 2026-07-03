package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.InductionCollapseFxPacket;
import com.wjx.kablade.network.KabladeNetwork;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
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
    private static final int DAMAGE_INTERVAL = 4;
    private static final int SLOW_AMPLIFIER = 2;
    private static final float IMPACT_DAMAGE = 40.0F;
    private static final float PULSE_DAMAGE = 8.5F;
    private static final double SWEEP_RANGE_XZ = 4.5D;
    private static final double SWEEP_RANGE_Y = 1.5D;
    private static final Vector3f ELECTRIC_BLUE = new Vector3f(0.05F, 0.55F, 1.0F);

    public InductionCollapseArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }
        if (type == ArtsType.Fail) {
            SaFx.cancelPiercingCharge(user);
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ResourceLocation combo = SaFx.startPiercingCombo(user, type);
        Vec3 direction = SaFx.flatLook(user);
        startLunge(level, user);
        spawnReleaseFx(level, user, direction);

        return combo;
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

    private static LivingEntity findContactTarget(ServerLevel level, LivingEntity user,
                                                  AABB box, Set<UUID> alreadyHit) {
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

    private static void startLunge(ServerLevel level, LivingEntity user) {
        if (!SaFx.isPiercingLungeWindow(user)) {
            return;
        }
        long now = level.getServer().getTickCount();
        LungeState state = new LungeState(
                level.dimension(),
                user.getUUID(),
                now + SaFx.PIERCING_LUNGE_TICKS,
                now + 1L,
                user.getBoundingBox());
        ACTIVE_LUNGES.put(user.getUUID(), state);
        SaFx.applyPiercingBoost(user);
        applyCollapseSweep(level, user, state);
    }

    private static void applyCollapse(ServerLevel level, LivingEntity user, LivingEntity target) {
        if (!isValidTarget(user, target) || target.level() != level) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                EFFECT_DURATION, SLOW_AMPLIFIER, false, true));
        long now = level.getServer().getTickCount();
        target.hurt(damageSource(level, user), IMPACT_DAMAGE);
        ACTIVE_COLLAPSES.put(target.getUUID(), new CollapseState(
                level.dimension(), target.getUUID(), user.getUUID(), now + EFFECT_DURATION, now + DAMAGE_INTERVAL));

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.55F);
        level.sendParticles(ParticleTypes.FLASH,
                target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        KabladeNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new InductionCollapseFxPacket(target.getId(), EFFECT_DURATION));
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
            if (!SaFx.isPiercingLungeWindow(user)) {
                return true;
            }

            if (now < state.nextTickAt) {
                return false;
            }

            state.nextTickAt = now + 1L;
            SaFx.applyPiercingBoost(user);
            applyCollapseSweep(level, user, state);
            return false;
        });
    }

    private static void applyCollapseSweep(ServerLevel level, LivingEntity user, LungeState state) {
        AABB current = user.getBoundingBox();
        AABB projected = current.move(SaFx.flatLook(user).scale(SaFx.piercingProjection(user)));
        AABB sweep = state.lastBox.minmax(current).minmax(projected)
                .inflate(SWEEP_RANGE_XZ, SWEEP_RANGE_Y, SWEEP_RANGE_XZ);
        state.lastBox = current;

        LivingEntity hit;
        while ((hit = findContactTarget(level, user, sweep, state.hitTargets)) != null) {
            state.hitTargets.add(hit.getUUID());
            applyCollapse(level, user, hit);
        }
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
        private final long expiresAt;
        private long nextTickAt;
        private AABB lastBox;
        private final Set<UUID> hitTargets = new HashSet<>();

        private LungeState(ResourceKey<Level> dimension, UUID ownerUUID,
                           long expiresAt, long nextTickAt, AABB lastBox) {
            this.dimension = dimension;
            this.ownerUUID = ownerUUID;
            this.expiresAt = expiresAt;
            this.nextTickAt = nextTickAt;
            this.lastBox = lastBox;
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

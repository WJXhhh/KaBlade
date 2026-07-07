package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.entity.IShootable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Red-black singularity for Vorpal Hole.
 * <p>
 * It is both the gameplay anchor and the client render anchor: server ticks pull
 * enemies, apply the 80% opening cut, then fire six 20% attack energy pulses.
 */
public class VorpalBlackHoleEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(VorpalBlackHoleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_OPENING_DAMAGE =
            SynchedEntityData.defineId(VorpalBlackHoleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PULSE_DAMAGE =
            SynchedEntityData.defineId(VorpalBlackHoleEntity.class, EntityDataSerializers.FLOAT);

    private static final int OPENING_TICK = 16;
    private static final int FIRST_PULSE_TICK = 24;
    private static final int PULSE_INTERVAL = 4;
    private static final int PULSE_COUNT = 6;
    private static final int VISUAL_AFTER_TICKS = 26;

    private static final double PULL_RADIUS = 14.5;
    private static final double PULL_VERTICAL_RADIUS = 8.0;
    private static final double DAMAGE_RADIUS = 6.25;
    private static final double DAMAGE_VERTICAL_RADIUS = 3.4;

    private LivingEntity owner;
    private UUID ownerUUID;
    private final Set<UUID> openingHit = new HashSet<>();

    public VorpalBlackHoleEntity(EntityType<? extends VorpalBlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public VorpalBlackHoleEntity(Level level, LivingEntity owner, Vec3 pos,
                                 int lifetime, float openingDamage, float pulseDamage) {
        this(ModEntities.VORPAL_BLACK_HOLE.get(), level);
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(owner.getYRot());
        this.setLifetime(lifetime);
        this.setOpeningDamage(openingDamage);
        this.setPulseDamage(pulseDamage);
    }

    public static VorpalBlackHoleEntity spawn(Level level, LivingEntity owner, Vec3 pos,
                                              int lifetime, float openingDamage, float pulseDamage) {
        VorpalBlackHoleEntity hole = new VorpalBlackHoleEntity(level, owner, pos,
                lifetime, openingDamage, pulseDamage);
        level.addFreshEntity(hole);
        return hole;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 56);
        this.entityData.define(DATA_OPENING_DAMAGE, 0.0F);
        this.entityData.define(DATA_PULSE_DAMAGE, 0.0F);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public int getVisualLifetime() {
        return this.getLifetime() + VISUAL_AFTER_TICKS;
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
    }

    public float getOpeningDamage() {
        return this.entityData.get(DATA_OPENING_DAMAGE);
    }

    public void setOpeningDamage(float damage) {
        this.entityData.set(DATA_OPENING_DAMAGE, Math.max(0.0F, damage));
    }

    public float getPulseDamage() {
        return this.entityData.get(DATA_PULSE_DAMAGE);
    }

    public void setPulseDamage(float damage) {
        this.entityData.set(DATA_PULSE_DAMAGE, Math.max(0.0F, damage));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) this.level();
        resolveOwner(level);
        if (this.tickCount < this.getLifetime()) {
            pullNearbyEntities(level);
        }

        if (this.tickCount == OPENING_TICK) {
            openingCut(level);
        }

        int pulseIndex = (this.tickCount - FIRST_PULSE_TICK) / PULSE_INTERVAL;
        if (pulseIndex >= 0
                && pulseIndex < PULSE_COUNT
                && (this.tickCount - FIRST_PULSE_TICK) % PULSE_INTERVAL == 0) {
            energyPulse(level, pulseIndex);
        }

        if (this.tickCount == this.getLifetime()) {
            collapseFx(level);
        }

        if (this.tickCount >= this.getVisualLifetime()) {
            this.discard();
        }
    }

    private void resolveOwner(ServerLevel level) {
        if (this.owner == null && this.ownerUUID != null) {
            Entity entity = level.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
    }

    private void pullNearbyEntities(ServerLevel level) {
        AABB bounds = AABB.ofSize(this.position(),
                PULL_RADIUS * 2.0, PULL_VERTICAL_RADIUS * 2.0, PULL_RADIUS * 2.0);
        for (Entity target : level.getEntities(this, bounds, this::canPull)) {
            Vec3 targetCenter = target instanceof LivingEntity living
                    ? target.position().add(0.0, living.getBbHeight() * 0.52, 0.0)
                    : target.position();
            Vec3 toCenter = this.position().subtract(targetCenter);
            double distance = Math.max(0.35, toCenter.length());
            if (distance > PULL_RADIUS) {
                continue;
            }

            double ease = 1.0 - Mth.clamp(distance / PULL_RADIUS, 0.0, 1.0);
            double strength = 0.16 + ease * 0.46;
            Vec3 pull = toCenter.normalize().scale(strength);
            if (target instanceof LivingEntity) {
                pull = pull.add(0.0, 0.025 + ease * 0.035, 0.0);
            }
            target.setDeltaMovement(target.getDeltaMovement().scale(0.28).add(pull));
            target.hurtMarked = true;
        }
    }

    private void openingCut(ServerLevel level) {
        DamageSource source = damageSource(level);
        for (LivingEntity target : targets(level, DAMAGE_RADIUS)) {
            if (!this.openingHit.add(target.getUUID())) {
                continue;
            }
            target.invulnerableTime = 0;
            if (com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, source, this.getOpeningDamage())) {
                Vec3 pull = this.position().subtract(target.position())
                        .multiply(0.08, 0.0, 0.08);
                target.setDeltaMovement(pull.x, 0.28, pull.z);
                target.hurtMarked = true;
            }
        }
        pulseFx(level, 0, 1.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.35F, 0.58F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.52F, 0.82F);
    }

    private void energyPulse(ServerLevel level, int pulseIndex) {
        DamageSource source = damageSource(level);
        for (LivingEntity target : targets(level, DAMAGE_RADIUS * (0.88 + pulseIndex * 0.025))) {
            target.invulnerableTime = 0;
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, source, this.getPulseDamage());
            Vec3 toCenter = this.position().subtract(target.position());
            if (toCenter.lengthSqr() > 1.0E-5) {
                Vec3 pull = toCenter.normalize().scale(0.16 + pulseIndex * 0.018);
                target.setDeltaMovement(target.getDeltaMovement().scale(0.18).add(pull));
                target.hurtMarked = true;
            }
        }
        pulseFx(level, pulseIndex, 0.72F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                0.85F, 0.72F + pulseIndex * 0.08F);
    }

    private Iterable<LivingEntity> targets(ServerLevel level, double radius) {
        AABB bounds = AABB.ofSize(this.position(),
                radius * 2.0, DAMAGE_VERTICAL_RADIUS * 2.0, radius * 2.0);
        double radiusSq = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, bounds, target -> {
            if (!canDamage(target)) {
                return false;
            }
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            return dx * dx + dz * dz <= radiusSq;
        });
    }

    private boolean canPull(Entity entity) {
        if (entity == this || entity == this.owner) {
            return false;
        }
        if (entity instanceof IShootable shootable && shootable.getShooter() == this.owner) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        if (entity instanceof LivingEntity living) {
            return SaTargeting.canDamage(this.owner, living);
        }
        return entity instanceof IShootable;
    }

    private boolean canDamage(LivingEntity target) {
        return SaTargeting.canDamage(this.owner, target);
    }

    private DamageSource damageSource(ServerLevel level) {
        if (this.owner instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        if (this.owner != null) {
            return level.damageSources().mobAttack(this.owner);
        }
        return level.damageSources().magic();
    }

    private void pulseFx(ServerLevel level, int pulseIndex, float intensity) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(),
                (int) (4 * intensity), 0.18, 0.18, 0.18, 0.03);
    }

    private void collapseFx(ServerLevel level) {
        level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.85F, 1.38F);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(8.0, 5.0, 8.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("OpeningDamage")) {
            this.setOpeningDamage(tag.getFloat("OpeningDamage"));
        }
        if (tag.contains("PulseDamage")) {
            this.setPulseDamage(tag.getFloat("PulseDamage"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putInt("Lifetime", this.getLifetime());
        tag.putFloat("OpeningDamage", this.getOpeningDamage());
        tag.putFloat("PulseDamage", this.getPulseDamage());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

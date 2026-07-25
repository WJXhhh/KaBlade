package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.slasharts.NarukamiDivinityTimeline;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative owner and damage timeline for 鸣雷神.
 *
 * <p>Only the cast root, initial facing, stored target and deterministic seed are
 * synchronized. The 41-frame mesh is reconstructed locally by the renderer.
 */
public final class NarukamiDivinityEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START_TIME =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_SEED =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_FORWARD_X =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FORWARD_Z =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(NarukamiDivinityEntity.class, EntityDataSerializers.FLOAT);

    private static final Set<UUID> ACTIVE_CASTERS = ConcurrentHashMap.newKeySet();
    private static final Vector3f ELECTRIC_PURPLE = new Vector3f(0.72F, 0.28F, 1.0F);
    private static final double OPENING_RADIUS = 3.25D;
    private static final double CROSS_IMPACT_RADIUS = 4.55D;
    private static final double CROSS_RADIUS = 6.0D;
    private static final double CAGE_RADIUS = 4.35D;

    private UUID ownerUuid;
    private LivingEntity owner;
    private LivingEntity target;
    private float totalDamage;
    private int nextHit;
    private boolean releasedActiveCaster;

    public NarukamiDivinityEntity(EntityType<? extends NarukamiDivinityEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static boolean isCasting(LivingEntity owner) {
        return owner != null && ACTIVE_CASTERS.contains(owner.getUUID());
    }

    public static NarukamiDivinityEntity spawn(ServerLevel level, LivingEntity owner,
                                                LivingEntity target, Vec3 targetAnchor,
                                                Vec3 initialForward, float totalDamage) {
        if (!ACTIVE_CASTERS.add(owner.getUUID())) {
            return null;
        }
        NarukamiDivinityEntity entity =
                new NarukamiDivinityEntity(ModEntities.NARUKAMI_DIVINITY.get(), level);
        entity.owner = owner;
        entity.ownerUuid = owner.getUUID();
        entity.target = target;
        entity.totalDamage = totalDamage;
        entity.setPos(owner.position());
        entity.setOwnerId(owner.getId());
        entity.setTargetId(target == null ? -1 : target.getId());
        entity.setStartGameTime(level.getGameTime());
        entity.setSeed(level.random.nextLong() ^ owner.getUUID().getMostSignificantBits());
        entity.setInitialForward(initialForward);
        entity.setStoredTargetAnchor(targetAnchor);
        if (!level.addFreshEntity(entity)) {
            ACTIVE_CASTERS.remove(owner.getUUID());
            return null;
        }
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_ID, -1);
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_START_TIME, 0L);
        this.entityData.define(DATA_SEED, 0L);
        this.entityData.define(DATA_FORWARD_X, 0.0F);
        this.entityData.define(DATA_FORWARD_Z, 1.0F);
        this.entityData.define(DATA_TARGET_X, 0.0F);
        this.entityData.define(DATA_TARGET_Y, 1.25F);
        this.entityData.define(DATA_TARGET_Z, 5.5F);
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    private void setOwnerId(int id) {
        this.entityData.set(DATA_OWNER_ID, id);
    }

    public int getTargetId() {
        return this.entityData.get(DATA_TARGET_ID);
    }

    private void setTargetId(int id) {
        this.entityData.set(DATA_TARGET_ID, id);
    }

    public long getStartGameTime() {
        return this.entityData.get(DATA_START_TIME);
    }

    private void setStartGameTime(long time) {
        this.entityData.set(DATA_START_TIME, time);
    }

    public long getSeed() {
        return this.entityData.get(DATA_SEED);
    }

    private void setSeed(long seed) {
        this.entityData.set(DATA_SEED, seed);
    }

    public Vec3 getInitialForward() {
        Vec3 forward = new Vec3(this.entityData.get(DATA_FORWARD_X), 0.0D,
                this.entityData.get(DATA_FORWARD_Z));
        return forward.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
    }

    private void setInitialForward(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        Vec3 normalized = horizontal.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
        this.entityData.set(DATA_FORWARD_X, (float) normalized.x);
        this.entityData.set(DATA_FORWARD_Z, (float) normalized.z);
        this.setYRot((float) (Mth.atan2(-normalized.x, normalized.z) * Mth.RAD_TO_DEG));
    }

    private void setStoredTargetAnchor(Vec3 anchor) {
        Vec3 offset = anchor.subtract(this.position());
        this.entityData.set(DATA_TARGET_X, (float) offset.x);
        this.entityData.set(DATA_TARGET_Y, (float) offset.y);
        this.entityData.set(DATA_TARGET_Z, (float) offset.z);
    }

    public Vec3 getStoredTargetAnchor() {
        return this.position().add(this.entityData.get(DATA_TARGET_X),
                this.entityData.get(DATA_TARGET_Y), this.entityData.get(DATA_TARGET_Z));
    }

    public Vec3 getTargetAnchor(float partialTick) {
        Entity entity = this.level().getEntity(getTargetId());
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living.getPosition(partialTick).add(0.0D, living.getBbHeight() * 0.55D, 0.0D);
        }
        return getStoredTargetAnchor();
    }

    public Vec3 getOwnerAnchor(float partialTick) {
        Entity entity = this.level().getEntity(getOwnerId());
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living.getPosition(partialTick);
        }
        return this.position();
    }

    public float getRenderAge(float partialTick) {
        if (this.level() == null) {
            return this.tickCount + partialTick;
        }
        return Mth.clamp((float) (this.level().getGameTime() + partialTick - getStartGameTime()),
                0.0F, NarukamiDivinityTimeline.DURATION_TICKS);
    }

    public float getReferenceFrame(float partialTick) {
        return NarukamiDivinityTimeline.referenceFrame(getRenderAge(partialTick));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        LivingEntity source = resolveOwner();
        if (source == null || !source.isAlive()) {
            this.discard();
            return;
        }

        ServerLevel level = (ServerLevel) this.level();
        playTimelineSounds(level);
        while (this.nextHit < NarukamiDivinityTimeline.HIT_TICKS.length
                && this.tickCount >= NarukamiDivinityTimeline.HIT_TICKS[this.nextHit]) {
            resolveHit(level, source, this.nextHit);
            this.nextHit++;
        }
        if (this.tickCount >= NarukamiDivinityTimeline.DURATION_TICKS) {
            this.discard();
        }
    }

    private LivingEntity resolveOwner() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }
        Entity entity = this.level().getEntity(getOwnerId());
        if (entity instanceof LivingEntity living
                && (this.ownerUuid == null || this.ownerUuid.equals(living.getUUID()))) {
            this.owner = living;
            this.ownerUuid = living.getUUID();
            return living;
        }
        return null;
    }

    private LivingEntity resolveTarget() {
        if (this.target != null && this.target.isAlive()) {
            return this.target;
        }
        Entity entity = this.level().getEntity(getTargetId());
        if (entity instanceof LivingEntity living && living.isAlive()) {
            this.target = living;
            return living;
        }
        return null;
    }

    private void resolveHit(ServerLevel level, LivingEntity source, int hitIndex) {
        float damage = this.totalDamage * NarukamiDivinityTimeline.DAMAGE_WEIGHTS[hitIndex];
        Vec3 center;
        double radius;
        List<LivingEntity> victims;
        if (hitIndex == 0) {
            center = getTargetAnchor(1.0F);
            radius = OPENING_RADIUS;
            victims = targetsAround(level, source, center, radius);
        } else if (hitIndex < 3) {
            center = source.position().add(0.0D, 1.15D, 0.0D);
            radius = CROSS_RADIUS;
            victims = crossAndRingTargets(level, source, center);
        } else {
            center = source.position().add(0.0D, 1.15D, 0.0D);
            radius = CAGE_RADIUS;
            victims = targetsAround(level, source, center, radius);
        }
        LivingEntity primary = resolveTarget();
        if (primary != null && SaTargeting.canDamageAttackable(source, primary)
                && !victims.contains(primary)
                && primary.getBoundingBox().getCenter().distanceToSqr(center)
                <= (radius + 1.25D) * (radius + 1.25D)) {
            victims.add(primary);
        }

        for (LivingEntity victim : victims) {
            if (!SaTargeting.canDamage(source, victim)
                    || !SaDamage.hurtSlashArtNoIFrame(victim, level, this, source, damage)) {
                continue;
            }
            Vec3 push = victim.position().subtract(source.position());
            if (push.lengthSqr() > 1.0E-6D) {
                double strength = hitIndex == NarukamiDivinityTimeline.HIT_TICKS.length - 1
                        ? 0.72D : 0.18D;
                victim.setDeltaMovement(victim.getDeltaMovement().scale(0.50D)
                        .add(push.normalize().scale(strength)).add(0.0D, 0.10D, 0.0D));
            }
            victim.hurtMarked = true;
            if (source instanceof Player player) {
                player.crit(victim);
            }
        }
        playHitFx(level, center, hitIndex, !victims.isEmpty());
    }

    /**
     * The two X hits retain their forward impact volume while the accompanying
     * character-centered ring supplies the enlarged circular AOE.
     */
    private List<LivingEntity> crossAndRingTargets(ServerLevel level, LivingEntity source,
                                                   Vec3 ringCenter) {
        List<LivingEntity> victims = targetsAround(level, source, ringCenter, CROSS_RADIUS);
        Vec3 impactCenter = getTargetAnchor(1.0F);
        for (LivingEntity candidate : targetsAround(
                level, source, impactCenter, CROSS_IMPACT_RADIUS)) {
            if (!victims.contains(candidate)) {
                victims.add(candidate);
            }
        }
        return victims;
    }

    private List<LivingEntity> targetsAround(ServerLevel level, LivingEntity source,
                                              Vec3 center, double radius) {
        AABB box = AABB.ofSize(center, radius * 2.0D, radius * 1.55D, radius * 2.0D);
        return level.getEntitiesOfClass(LivingEntity.class, box, candidate ->
                candidate.isPickable()
                        && SaTargeting.canDamageAttackable(source, candidate)
                        && candidate.getBoundingBox().getCenter().distanceToSqr(center)
                        <= radius * radius);
    }

    private void playTimelineSounds(ServerLevel level) {
        Vec3 targetAnchor = getTargetAnchor(1.0F);
        Vec3 ownerAnchor = resolveOwner() == null ? this.position() : resolveOwner().position();
        if (this.tickCount == 4) {
            level.playSound(null, targetAnchor.x, targetAnchor.y, targetAnchor.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.08F, 1.28F);
        } else if (this.tickCount == 8 || this.tickCount == 10) {
            level.playSound(null, targetAnchor.x, targetAnchor.y, targetAnchor.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.94F,
                    this.tickCount == 8 ? 0.62F : 0.78F);
        } else if (this.tickCount == 20) {
            level.playSound(null, ownerAnchor.x, ownerAnchor.y + 1.1D, ownerAnchor.z,
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.82F, 1.52F);
        } else if (this.tickCount == 24 || this.tickCount == 28) {
            level.playSound(null, ownerAnchor.x, ownerAnchor.y + 1.1D, ownerAnchor.z,
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.92F,
                    this.tickCount == 24 ? 1.54F : 1.82F);
        } else if (this.tickCount == 31) {
            level.playSound(null, ownerAnchor.x, ownerAnchor.y + 0.5D, ownerAnchor.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.88F, 1.64F);
        }
    }

    private void playHitFx(ServerLevel level, Vec3 center, int hitIndex, boolean hit) {
        int count = hit ? 10 + hitIndex * 2 : 4;
        level.sendParticles(new DustParticleOptions(ELECTRIC_PURPLE, 1.1F),
                center.x, center.y, center.z, count, 0.8D, 0.6D, 0.8D, 0.035D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, Math.max(4, count / 2),
                0.75D, 0.55D, 0.75D, 0.055D);
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseActiveCaster();
        super.remove(reason);
    }

    private void releaseActiveCaster() {
        if (!this.releasedActiveCaster && this.ownerUuid != null) {
            ACTIVE_CASTERS.remove(this.ownerUuid);
            this.releasedActiveCaster = true;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.totalDamage = tag.getFloat("TotalDamage");
        this.nextHit = tag.getInt("NextHit");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("TotalDamage", this.totalDamage);
        tag.putInt("NextHit", this.nextHit);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 a = this.position();
        Vec3 b = getStoredTargetAnchor();
        return new AABB(a, b).inflate(7.0D, 5.0D, 7.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

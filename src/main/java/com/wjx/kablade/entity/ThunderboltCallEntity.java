package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.slasharts.ThunderboltCallTimeline;
import com.wjx.kablade.specialeffect.ThunderBlitz;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import com.wjx.kablade.util.SaTarget;
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
import net.minecraft.world.effect.MobEffectInstance;
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

/** Server-authoritative owner, target, direction and damage timeline for Thunderbolt Call. */
public final class ThunderboltCallEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START_TIME =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_SEED =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(ThunderboltCallEntity.class, EntityDataSerializers.FLOAT);

    private static final Set<UUID> ACTIVE_CASTERS = ConcurrentHashMap.newKeySet();
    private static final Vector3f ELECTRIC_PURPLE = new Vector3f(0.66F, 0.24F, 1.0F);
    private static final int PARALYSIS_DURATION = 72;
    private static final int PARALYSIS_AMPLIFIER = 8;
    private static final int NARUKAMI_DURATION = 300;
    private static final double IMPACT_RADIUS = 3.6D;
    private static final double X_REAR_REACH = 1.1D;
    private static final double X_FORWARD_REACH = 7.2D;
    private static final double X_HALF_WIDTH = 4.8D;
    private static final double X_HALF_HEIGHT = 3.2D;

    private UUID ownerUuid;
    private LivingEntity owner;
    private Entity target;
    private float totalDamage;
    private int nextHit;
    private boolean releasedActiveCaster;

    public ThunderboltCallEntity(EntityType<? extends ThunderboltCallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static boolean isCasting(LivingEntity owner) {
        return owner != null && ACTIVE_CASTERS.contains(owner.getUUID());
    }

    public static ThunderboltCallEntity spawn(ServerLevel level, LivingEntity owner,
                                               Entity target, Vec3 targetAnchor,
                                               Vec3 launchDirection, float totalDamage) {
        if (!ACTIVE_CASTERS.add(owner.getUUID())) {
            return null;
        }
        ThunderboltCallEntity entity = new ThunderboltCallEntity(ModEntities.THUNDERBOLT_CALL.get(), level);
        entity.owner = owner;
        entity.ownerUuid = owner.getUUID();
        entity.target = target;
        entity.totalDamage = totalDamage;
        entity.setPos(owner.position());
        entity.setOwnerId(owner.getId());
        entity.setTargetId(target == null ? -1 : target.getId());
        entity.setStartGameTime(level.getGameTime());
        entity.setSeed(level.random.nextLong() ^ owner.getUUID().getMostSignificantBits());
        entity.setLaunchDirection(launchDirection);
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
        this.entityData.define(DATA_DIRECTION_X, 0.0F);
        this.entityData.define(DATA_DIRECTION_Y, 0.0F);
        this.entityData.define(DATA_DIRECTION_Z, 1.0F);
        this.entityData.define(DATA_TARGET_X, 0.0F);
        this.entityData.define(DATA_TARGET_Y, 1.2F);
        this.entityData.define(DATA_TARGET_Z, 4.0F);
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

    public Vec3 getLaunchDirection() {
        Vec3 direction = new Vec3(this.entityData.get(DATA_DIRECTION_X),
                this.entityData.get(DATA_DIRECTION_Y), this.entityData.get(DATA_DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private void setLaunchDirection(Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        this.entityData.set(DATA_DIRECTION_X, (float) normalized.x);
        this.entityData.set(DATA_DIRECTION_Y, (float) normalized.y);
        this.entityData.set(DATA_DIRECTION_Z, (float) normalized.z);
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
        if (entity != null && entity.isAlive()) {
            return SaTarget.center(entity.getBoundingBox());
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
                0.0F, ThunderboltCallTimeline.DURATION_TICKS);
    }

    public float getReferenceFrame(float partialTick) {
        return ThunderboltCallTimeline.referenceFrame(getRenderAge(partialTick));
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
        while (this.nextHit < ThunderboltCallTimeline.HIT_TICKS.length
                && this.tickCount >= ThunderboltCallTimeline.HIT_TICKS[this.nextHit]) {
            resolveHit(level, source, this.nextHit);
            this.nextHit++;
        }
        if (this.tickCount >= ThunderboltCallTimeline.DURATION_TICKS) {
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

    private Entity resolveTarget() {
        if (this.target != null && this.target.isAlive()) {
            return this.target;
        }
        Entity entity = this.level().getEntity(getTargetId());
        if (entity != null && entity.isAlive() && SaTarget.of(entity).isPresent()) {
            this.target = entity;
            return entity;
        }
        return null;
    }

    private void resolveHit(ServerLevel level, LivingEntity source, int hitIndex) {
        float damage = this.totalDamage * ThunderboltCallTimeline.DAMAGE_WEIGHTS[hitIndex];
        List<SaTarget> targets = hitIndex == 0
                ? impactTargets(level, source, IMPACT_RADIUS)
                : crossTargets(level, source);
        Vec3 anchor = getTargetAnchor(1.0F);
        for (SaTarget selected : targets) {
            LivingEntity victim = selected.root();
            if (!SaTargeting.canDamage(source, selected.hitEntity())
                    || !SaDamage.hurtSlashArtNoIFrame(
                    selected.hitEntity(), level, this, source, damage)) {
                continue;
            }
            victim.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                    PARALYSIS_DURATION, PARALYSIS_AMPLIFIER, false, true, true));
            victim.getPersistentData().putInt(ThunderBlitz.NARUKAMI_TAG, NARUKAMI_DURATION);
            if (hitIndex == 0) {
                Vec3 inward = anchor.subtract(victim.position());
                Vec3 pull = inward.horizontalDistanceSqr() > 1.0E-6D
                        ? new Vec3(inward.x, 0.0D, inward.z).normalize().scale(0.12D) : Vec3.ZERO;
                victim.setDeltaMovement(victim.getDeltaMovement().scale(0.42D)
                        .add(pull).add(0.0D, 0.46D, 0.0D));
            } else if (hitIndex == ThunderboltCallTimeline.HIT_TICKS.length - 1) {
                victim.setDeltaMovement(victim.getDeltaMovement().scale(0.35D)
                        .add(getLaunchDirection().scale(0.72D)).add(0.0D, 0.16D, 0.0D));
            }
            victim.hurtMarked = true;
            if (source instanceof Player player) {
                player.crit(victim);
            }
        }
        playHitFx(level, anchor, hitIndex, !targets.isEmpty());
    }

    private List<SaTarget> impactTargets(ServerLevel level, LivingEntity source, double radius) {
        Vec3 center = getTargetAnchor(1.0F);
        AABB box = AABB.ofSize(center, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        SaTarget primary = SaTarget.of(resolveTarget()).orElse(null);
        List<SaTarget> targets = SaTargeting.targets(level, source, box, candidate ->
                SaTargeting.canDamageAttackable(source, candidate.root())
                        && candidate.distanceToSqr(center) <= radius * radius);
        if (primary != null && SaTargeting.canDamageAttackable(source, primary.hitEntity())
                && primary.distanceToSqr(center) <= radius * radius) {
            targets.removeIf(candidate -> candidate.damageGroup().equals(primary.damageGroup()));
            targets.add(primary);
        }
        return targets;
    }

    private List<SaTarget> crossTargets(ServerLevel level, LivingEntity source) {
        Vec3 forward = getLaunchDirection();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        Vec3 anchor = getStoredTargetAnchor();
        Vec3 origin = anchor.subtract(forward.scale(X_REAR_REACH));
        double totalReach = X_REAR_REACH + X_FORWARD_REACH;
        Vec3 end = origin.add(forward.scale(totalReach));
        AABB box = new AABB(origin, end).inflate(
                X_HALF_WIDTH + 1.5D, X_HALF_HEIGHT + 1.5D, X_HALF_WIDTH + 1.5D);
        Vec3 finalRight = right;
        Vec3 finalUp = up;
        return SaTargeting.targets(level, source, box, candidate -> {
            if (!SaTargeting.canDamageAttackable(source, candidate.root())) {
                return false;
            }
            Entity physical = candidate.hitEntity();
            Vec3 offset = candidate.anchor().subtract(origin);
            double ahead = offset.dot(forward);
            double lateral = Math.abs(offset.dot(finalRight));
            double vertical = Math.abs(offset.dot(finalUp));
            double horizontalMargin = physical.getBbWidth() * 0.5D;
            double verticalMargin = physical.getBbHeight() * 0.5D;
            return ahead >= -horizontalMargin && ahead <= totalReach + horizontalMargin
                    && lateral <= X_HALF_WIDTH + horizontalMargin
                    && vertical <= X_HALF_HEIGHT + verticalMargin;
        });
    }

    private void playTimelineSounds(ServerLevel level) {
        Vec3 anchor = getTargetAnchor(1.0F);
        if (this.tickCount == 3) {
            level.playSound(null, this.getX(), this.getY() + 1.1D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.70F, 1.72F);
        } else if (this.tickCount == 7) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.46F);
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.92F, 0.78F);
        } else if (this.tickCount == 16) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.18F, 1.34F);
        } else if (this.tickCount == 23) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.78F, 1.62F);
        } else if (this.tickCount == 31) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.92F, 1.52F);
        }
    }

    private void playHitFx(ServerLevel level, Vec3 anchor, int hitIndex, boolean hit) {
        int count = hit ? 12 + hitIndex * 4 : 5;
        level.sendParticles(new DustParticleOptions(ELECTRIC_PURPLE, 1.15F),
                anchor.x, anchor.y, anchor.z, count, 0.9D, 0.75D, 0.9D, 0.04D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                anchor.x, anchor.y, anchor.z, Math.max(4, count / 2),
                0.85D, 0.65D, 0.85D, 0.06D);
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
        Vec3 b = getStoredTargetAnchor().add(getLaunchDirection().scale(7.5D));
        return new AABB(a, b).inflate(6.0D, 5.0D, 6.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

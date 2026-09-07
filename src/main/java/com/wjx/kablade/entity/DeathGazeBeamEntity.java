package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 灼热重斩 (Death Gaze) 高能穿透激光与等离子光束实体。
 * <p>
 * 锁定施法者前方朝向，持续喷涌贯穿性的高能等离子热线与紫色崩坏粒子风暴。
 */
public class DeathGazeBeamEntity extends Entity {

    public static final int MAX_LIFETIME = 26; // ~1.3s
    public static final double MAX_RANGE = 26.0D;
    public static final double BEAM_RADIUS = 1.5D;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(DeathGazeBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(DeathGazeBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BEAM_LENGTH =
            SynchedEntityData.defineId(DeathGazeBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final Vector3f PURPLE_PARTICLE_COLOR = new Vector3f(0.85F, 0.15F, 1.0F);
    private static final Vector3f DEEP_VIOLET_COLOR = new Vector3f(0.60F, 0.05F, 0.90F);

    private UUID ownerUuid;
    private LivingEntity owner;
    private float totalDamage;

    public DeathGazeBeamEntity(EntityType<? extends DeathGazeBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static DeathGazeBeamEntity spawn(ServerLevel level, LivingEntity owner, float totalDamage) {
        DeathGazeBeamEntity entity = new DeathGazeBeamEntity(ModEntities.DEATH_GAZE_BEAM.get(), level);
        entity.owner = owner;
        entity.ownerUuid = owner.getUUID();
        entity.totalDamage = totalDamage;
        entity.setOwnerId(owner.getId());

        Vec3 look = owner.getLookAngle();
        Vec3 spawnPos = owner.getEyePosition().add(look.scale(0.85D)).subtract(0, 0.22D, 0);
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
        entity.yRotO = owner.getYRot();
        entity.xRotO = owner.getXRot();

        // Initial raycast
        float initialLength = computeBeamLength(level, spawnPos, look, entity);
        entity.setBeamLength(initialLength);

        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_ID, -1);
        this.entityData.define(DATA_AGE, 0);
        this.entityData.define(DATA_BEAM_LENGTH, (float) MAX_RANGE);
    }

    @Override
    public void tick() {
        super.tick();
        int age = this.entityData.get(DATA_AGE) + 1;
        this.entityData.set(DATA_AGE, age);

        if (age >= MAX_LIFETIME) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        LivingEntity currentOwner = getOwnerLiving();
        if (currentOwner != null && currentOwner.isAlive()) {
            Vec3 look = currentOwner.getLookAngle();
            Vec3 newPos = currentOwner.getEyePosition().add(look.scale(0.85D)).subtract(0, 0.22D, 0);
            this.setPos(newPos.x, newPos.y, newPos.z);
            this.setYRot(currentOwner.getYRot());
            this.setXRot(currentOwner.getXRot());
        }

        // Raycast collision for length
        Vec3 origin = this.position();
        Vec3 look = this.getViewVector(1.0F);
        float currentLength = computeBeamLength(this.level(), origin, look, this);
        this.setBeamLength(currentLength);

        // Server-side damage and particles
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Damage pulses every 3 ticks
            if (age >= 3 && age <= 24 && age % 3 == 0) {
                applyBeamDamage(serverLevel, currentOwner, origin, look, currentLength);
            }

            // Continuous ambient beam sound
            if (age % 5 == 0) {
                serverLevel.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.9F);
                serverLevel.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.4F);
            }

            // Spawn particles along beam and at impact point
            spawnBeamParticles(serverLevel, origin, look, currentLength);
        }
    }

    private static float computeBeamLength(Level level, Vec3 origin, Vec3 look, Entity entity) {
        Vec3 end = origin.add(look.scale(MAX_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(
                origin, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
        if (hit.getType() != HitResult.Type.MISS) {
            return (float) hit.getLocation().distanceTo(origin);
        }
        return (float) MAX_RANGE;
    }

    private void applyBeamDamage(ServerLevel level, @Nullable LivingEntity ownerEntity,
                                 Vec3 origin, Vec3 look, float length) {
        if (ownerEntity == null) {
            // Respect AGENTS.md rule: cancel delayed damage if owner cannot be resolved
            return;
        }

        Vec3 end = origin.add(look.scale(length));
        AABB searchBox = new AABB(origin, end).inflate(BEAM_RADIUS + 0.5D);
        List<SaTarget> candidates = SaTargeting.uniqueTargets(level, ownerEntity, searchBox);

        float hitDamage = this.totalDamage / 6.5F;
        double radiusSq = BEAM_RADIUS * BEAM_RADIUS;

        for (SaTarget target : candidates) {
            LivingEntity victim = target.root();
            Vec3 targetCenter = victim.getBoundingBox().getCenter();
            double distSq = distanceSqToSegment(targetCenter, origin, end);

            if (distSq <= radiusSq) {
                SaDamage.hurtSlashArtNoIFrame(target.hitEntity(), level, this, ownerEntity, hitDamage);
                victim.setSecondsOnFire(3);
                // Slight knockback along laser beam vector
                Vec3 impulse = look.scale(0.22D).add(0.0D, 0.08D, 0.0D);
                victim.push(impulse.x, impulse.y, impulse.z);
                victim.hurtMarked = true;
            }
        }
    }

    private void spawnBeamParticles(ServerLevel level, Vec3 origin, Vec3 look, float length) {
        DustParticleOptions purpleDust = new DustParticleOptions(PURPLE_PARTICLE_COLOR, 1.7F);
        DustParticleOptions deepViolet = new DustParticleOptions(DEEP_VIOLET_COLOR, 2.2F);

        int sampleCount = Math.max(3, (int) (length / 2.0F));
        for (int i = 1; i <= sampleCount; i++) {
            double progress = (double) i / sampleCount;
            Vec3 point = origin.add(look.scale(length * progress));
            double spread = 0.28D;

            level.sendParticles(purpleDust, point.x, point.y, point.z, 2, spread, spread, spread, 0.02D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH, point.x, point.y, point.z, 1,
                        spread * 0.4D, spread * 0.4D, spread * 0.4D, 0.01D);
                level.sendParticles(ParticleTypes.WITCH, point.x, point.y, point.z, 1,
                        spread, spread, spread, 0.02D);
            }
        }

        // Blast particles at impact end
        Vec3 impactEnd = origin.add(look.scale(length));
        level.sendParticles(purpleDust, impactEnd.x, impactEnd.y, impactEnd.z, 8, 0.45D, 0.45D, 0.45D, 0.08D);
        level.sendParticles(deepViolet, impactEnd.x, impactEnd.y, impactEnd.z, 6, 0.40D, 0.40D, 0.40D, 0.06D);
        level.sendParticles(ParticleTypes.LAVA, impactEnd.x, impactEnd.y, impactEnd.z, 2, 0.25D, 0.25D, 0.25D, 0.04D);
        level.sendParticles(ParticleTypes.SMOKE, impactEnd.x, impactEnd.y, impactEnd.z, 4, 0.35D, 0.35D, 0.35D, 0.02D);
    }

    public static double distanceSqToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        Vec3 ap = point.subtract(a);
        double abLenSq = ab.lengthSqr();
        if (abLenSq < 1e-6) {
            return point.distanceToSqr(a);
        }
        double t = ap.dot(ab) / abLenSq;
        t = Mth.clamp(t, 0.0, 1.0);
        Vec3 closest = a.add(ab.scale(t));
        return point.distanceToSqr(closest);
    }

    @Nullable
    public LivingEntity getOwnerLiving() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }
        int id = this.entityData.get(DATA_OWNER_ID);
        if (id >= 0) {
            Entity e = this.level().getEntity(id);
            if (e instanceof LivingEntity le) {
                this.owner = le;
                return le;
            }
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(this.ownerUuid);
            if (e instanceof LivingEntity le) {
                this.owner = le;
                return le;
            }
        }
        return null;
    }

    public int getAge() {
        return this.entityData.get(DATA_AGE);
    }

    public float getBeamLength() {
        return this.entityData.get(DATA_BEAM_LENGTH);
    }

    public void setBeamLength(float length) {
        this.entityData.set(DATA_BEAM_LENGTH, length);
    }

    public void setOwnerId(int id) {
        this.entityData.set(DATA_OWNER_ID, id);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.contains("Damage")) {
            this.totalDamage = tag.getFloat("Damage");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Damage", this.totalDamage);
    }
}

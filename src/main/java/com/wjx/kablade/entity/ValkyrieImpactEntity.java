package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
import com.wjx.kablade.util.SaTargeting;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Timeline, hitbox calculation and damage entity for the Valkyrie Impact (女武神冲击) Slash Art.
 * Handles:
 * 1. Frame 0 ~ 24: Windup & Downward Slam
 * 2. Frame 24 (Tick 24): Instant ground impact burst (R=1.8m) with heavy blast feedback
 * 3. Frame 24 ~ 42 (Tick 24~42): Forward ground fissure sweep (1.25m -> 5.5m) launching victims upward
 * 4. Frame 42 ~ 68 (Tick 42~68): Hold pose and recovery dissipation
 */
public class ValkyrieImpactEntity extends Entity {

    public static final int MAX_LIFETIME = 68;      // 2.24s (30fps)
    public static final int HIT_TICK = 17;          // 0.56s (Frame 17) downward slam motion overlap
    public static final int FISSURE_END_TICK = 42;  // 1.40s (Frame 42) fissure propagation end frame
    public static final double FISSURE_START_Z = 1.25D;
    public static final double FISSURE_END_Z = 8.5D; // Expanded forward range (up to 8.5m)

    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(ValkyrieImpactEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> YAW_ROT =
            SynchedEntityData.defineId(ValkyrieImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(ValkyrieImpactEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUUID;
    private final Set<UUID> hitByFissure = new HashSet<>();

    public ValkyrieImpactEntity(EntityType<? extends ValkyrieImpactEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static ValkyrieImpactEntity spawn(ServerLevel level, LivingEntity owner, Vec3 impactPos, float yaw, float damage) {
        ValkyrieImpactEntity entity = new ValkyrieImpactEntity(ModEntities.VALKYRIE_IMPACT.get(), level);
        entity.setPos(impactPos.x, impactPos.y, impactPos.z);
        entity.ownerUUID = owner.getUUID();
        entity.setYawRot(yaw);
        entity.setDamage(damage);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFETIME, 0);
        this.entityData.define(YAW_ROT, 0.0F);
        this.entityData.define(DAMAGE, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        int age = this.entityData.get(LIFETIME) + 1;
        this.entityData.set(LIFETIME, age);

        if (age >= MAX_LIFETIME) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            LivingEntity owner = getOwnerLiving();
            if (owner == null) {
                this.discard();
                return;
            }

            // 1. Instant Ground Impact Burst at Frame 24
            if (age == HIT_TICK) {
                triggerImpactBurst(serverLevel, owner);
            }

            // 2. Linear Ground Fissure Propagation from Frame 24 to 42
            if (age >= HIT_TICK && age <= FISSURE_END_TICK) {
                triggerFissureStep(serverLevel, owner, age);
            }
        }
    }

    /** Phase 1: Instant burst around the sword impact point */
    private void triggerImpactBurst(ServerLevel level, LivingEntity owner) {
        float damage = getDamage();

        // Audio cues for powerful earth slam
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.4F, 0.95F);
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.75F);
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.6F);

        // Radial burst AABB (Radius expanded to 3.2m, Height 2.5m)
        AABB box = this.getBoundingBox().inflate(3.2D, 1.25D, 3.2D);
        List<SaTarget> targets = SaTargeting.uniqueTargets(level, owner, box);
        for (SaTarget target : targets) {
            LivingEntity victim = target.root();
            SaDamage.hurtSlashArtNoIFrame(target.hitEntity(), level, this, owner, damage * 1.25F);

            // Ground slam shock impulse
            Vec3 delta = victim.position().subtract(this.position());
            Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
            Vec3 knockback = (horizontal.lengthSqr() > 1e-4 ? horizontal.normalize() : getForwardVector()).scale(1.1D);
            victim.setDeltaMovement(knockback.x, 0.65D, knockback.z);
            victim.hurtMarked = true;
            victim.hasImpulse = true;
        }
    }

    /** Phase 2: Forward multi-row ground fissure sweep and knockup */
    private void triggerFissureStep(ServerLevel level, LivingEntity owner, int age) {
        float progress = (float) (age - HIT_TICK) / (float) (FISSURE_END_TICK - HIT_TICK);
        double distFromStart = progress * (FISSURE_END_Z - FISSURE_START_Z);

        Vec3 forward = getForwardVector();
        Vec3 stepCenter = this.position().add(forward.scale(distFromStart));

        // Expanding fan AABB: width scales from 2.4m up to 6.8m total width
        double halfWidth = 1.2D + progress * 2.2D;
        AABB stepBox = new AABB(
                stepCenter.x - halfWidth, stepCenter.y - 0.6D, stepCenter.z - halfWidth,
                stepCenter.x + halfWidth, stepCenter.y + 2.8D, stepCenter.z + halfWidth
        );

        List<SaTarget> targets = SaTargeting.uniqueTargets(level, owner, stepBox);
        for (SaTarget target : targets) {
            LivingEntity victim = target.root();
            if (hitByFissure.add(victim.getUUID())) {
                SaDamage.hurtSlashArtNoIFrame(target.hitEntity(), level, this, owner, getDamage() * 0.95F);

                // High knockup velocity: launches enemy 4~5 blocks into the air
                victim.setDeltaMovement(forward.x * 0.45D, 1.15D, forward.z * 0.45D);
                victim.hurtMarked = true;
                victim.hasImpulse = true;
            }
        }
    }

    public Vec3 getForwardVector() {
        float yawRad = -getYawRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.sin(yawRad), 0.0, Mth.cos(yawRad)).normalize();
    }

    @Nullable
    public LivingEntity getOwnerLiving() {
        if (this.ownerUUID == null || !(this.level() instanceof ServerLevel sl)) {
            return null;
        }
        Entity e = sl.getEntity(this.ownerUUID);
        return e instanceof LivingEntity le ? le : null;
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public float getYawRot() {
        return this.entityData.get(YAW_ROT);
    }

    public void setYawRot(float yaw) {
        this.entityData.set(YAW_ROT, yaw);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("Damage")) {
            setDamage(tag.getFloat("Damage"));
        }
        if (tag.contains("YawRot")) {
            setYawRot(tag.getFloat("YawRot"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putFloat("Damage", getDamage());
        tag.putFloat("YawRot", getYawRot());
    }
}

package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Fengxuan's 1.12.2-style modified slash dimension.
 */
public final class FengxuanDimensionEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(FengxuanDimensionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(FengxuanDimensionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(FengxuanDimensionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(FengxuanDimensionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(FengxuanDimensionEntity.class, EntityDataSerializers.INT);

    private static final double PULL_RADIUS_XZ = 6.0D;
    private static final double PULL_RADIUS_Y = 6.0D;
    private static final double DAMAGE_RADIUS_XZ = 4.0D;
    private static final double DAMAGE_RADIUS_Y = 5.0D;
    private static final int DAMAGE_INTERVAL = 4;
    private static final int PROJECTILE_SCAN_INTERVAL = 8;
    private static final int MAX_PULL_TARGETS = 12;
    private static final int MAX_DAMAGE_TARGETS = 8;
    private static final int MAX_PROJECTILES_PER_SCAN = 8;

    private LivingEntity owner;

    public FengxuanDimensionEntity(EntityType<? extends FengxuanDimensionEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FengxuanDimensionEntity spawn(ServerLevel level, LivingEntity owner, Vec3 pos,
                                                float damage, int color, int lifetime) {
        FengxuanDimensionEntity entity = new FengxuanDimensionEntity(ModEntities.FENGXUAN_DIMENSION.get(), level);
        entity.owner = owner;
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setYRot(owner.getYRot());
        entity.setOwnerId(owner.getId());
        entity.setDamage(damage);
        entity.setColor(color);
        entity.setLifetime(lifetime);
        entity.setSeed(level.random.nextInt(50));
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 50);
        this.entityData.define(DATA_COLOR, 0xFFFFFF);
        this.entityData.define(DATA_OWNER_ID, 0);
        this.entityData.define(DATA_DAMAGE, 1.0F);
        this.entityData.define(DATA_SEED, 0);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getSeed() {
        return this.entityData.get(DATA_SEED);
    }

    public void setSeed(int seed) {
        this.entityData.set(DATA_SEED, seed);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    public void setOwnerId(int ownerId) {
        this.entityData.set(DATA_OWNER_ID, ownerId);
    }

    public LivingEntity getOwner() {
        if (this.owner == null && this.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(this.getOwnerId());
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
        return this.owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        if (this.tickCount < 8 && this.tickCount % 2 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.2F,
                    0.5F + 0.25F * this.random.nextFloat());
        }

        LivingEntity source = getOwner();
        if (source != null && source.isAlive()) {
            if (this.tickCount % PROJECTILE_SCAN_INTERVAL == 0) {
                destroyProjectiles(source);
            }
            pullTargets(source);
            if (this.tickCount <= 1 || this.tickCount % DAMAGE_INTERVAL == 0) {
                damageTargets(source);
            }
        }

        if (this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    private void destroyProjectiles(LivingEntity source) {
        AABB box = pullBox();
        int removed = 0;
        for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class, box,
                projectile -> projectile.isAlive() && projectile.getOwner() != source)) {
            projectile.discard();
            if (++removed >= MAX_PROJECTILES_PER_SCAN) {
                break;
            }
        }
    }

    private void pullTargets(LivingEntity source) {
        AABB box = pullBox();
        int processed = 0;
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box,
                target -> isValidTarget(source, target) && isInsidePullRange(target))) {
            pullTowardCenter(target);
            if (++processed >= MAX_PULL_TARGETS) {
                break;
            }
        }
    }

    private void damageTargets(LivingEntity source) {
        AABB box = damageBox();
        DamageSource damageSource = this.level().damageSources().indirectMagic(this, source);
        float damage = Math.max(1.0F, this.getDamage());
        int processed = 0;
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box,
                target -> isValidTarget(source, target) && isInsideDamageRange(target))) {
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, damageSource, damage);
            if (++processed >= MAX_DAMAGE_TARGETS) {
                break;
            }
        }
    }

    private AABB pullBox() {
        return this.getBoundingBox().inflate(PULL_RADIUS_XZ, PULL_RADIUS_Y, PULL_RADIUS_XZ);
    }

    private AABB damageBox() {
        return this.getBoundingBox().inflate(DAMAGE_RADIUS_XZ, DAMAGE_RADIUS_Y, DAMAGE_RADIUS_XZ);
    }

    private boolean isValidTarget(LivingEntity source, LivingEntity target) {
        return SaTargeting.canDamage(source, target);
    }

    private boolean isInsidePullRange(LivingEntity target) {
        return isInsideRange(target, PULL_RADIUS_XZ, PULL_RADIUS_Y);
    }

    private boolean isInsideDamageRange(LivingEntity target) {
        return isInsideRange(target, DAMAGE_RADIUS_XZ, DAMAGE_RADIUS_Y);
    }

    private boolean isInsideRange(LivingEntity target, double radiusXZ, double radiusY) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        return dx * dx + dz * dz <= radiusXZ * radiusXZ
                && Math.abs(target.getY() + target.getBbHeight() * 0.52D - this.getY()) <= radiusY;
    }

    private void pullTowardCenter(LivingEntity target) {
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
        Vec3 toCenter = this.position().subtract(targetCenter);
        double distance = Math.max(0.35D, toCenter.length());
        double ease = 1.0D - Mth.clamp(distance / PULL_RADIUS_XZ, 0.0D, 1.0D);
        double strength = 0.16D + ease * 0.46D;
        Vec3 pull = toCenter.normalize()
                .scale(strength)
                .add(0.0D, 0.025D + ease * 0.035D, 0.0D);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.28D).add(pull));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(3.25D, 3.0D, 3.25D);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

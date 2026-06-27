package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Visual anchor for Shock Impact.
 * <p>
 * The trail follows its owner for its whole lifetime (using the forward/up
 * offset captured at spawn and the yaw locked at spawn). The entity itself only
 * snaps to the owner once per tick to keep it near the owner for culling; the
 * smooth, per-frame placement is done in the renderer with partialTick so the
 * trail doesn't stutter behind the interpolated player.
 */
public class ShockImpactEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FORWARD =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.FLOAT);

    public ShockImpactEntity(EntityType<? extends ShockImpactEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static ShockImpactEntity spawn(Level level, LivingEntity owner, double forwardOffset,
                                          double upOffset, int lifetime, float scale) {
        ShockImpactEntity entity = new ShockImpactEntity(ModEntities.SHOCK_IMPACT.get(), level);
        entity.setYRot(owner.getYRot());
        entity.setOwnerId(owner.getId());
        entity.setForwardOffset((float) forwardOffset);
        entity.setUpOffset((float) upOffset);
        entity.setLifetime(lifetime);
        entity.setScale(scale);
        entity.followOwner();
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 34);
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_OWNER_ID, -1);
        this.entityData.define(DATA_FORWARD, 0.0F);
        this.entityData.define(DATA_UP, 0.0F);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, Math.max(0.1F, scale));
    }

    public void setOwnerId(int id) {
        this.entityData.set(DATA_OWNER_ID, id);
    }

    public void setForwardOffset(float forward) {
        this.entityData.set(DATA_FORWARD, forward);
    }

    public void setUpOffset(float up) {
        this.entityData.set(DATA_UP, up);
    }

    public float getForwardOffset() {
        return this.entityData.get(DATA_FORWARD);
    }

    public float getUpOffset() {
        return this.entityData.get(DATA_UP);
    }

    public Entity getOwner() {
        int id = this.entityData.get(DATA_OWNER_ID);
        return id >= 0 ? this.level().getEntity(id) : null;
    }

    /** Snap onto the owner using the locked yaw and the captured offsets (tick granularity). */
    private void followOwner() {
        Entity owner = getOwner();
        if (owner == null) {
            return;
        }
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yawRad);
        double fz = Mth.cos(yawRad);
        double forward = getForwardOffset();
        double up = getUpOffset();
        this.setPos(owner.getX() + fx * forward, owner.getY() + up, owner.getZ() + fz * forward);
    }

    @Override
    public void tick() {
        super.tick();
        followOwner();
        if (!this.level().isClientSide() && this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(8.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("Scale")) {
            this.setScale(tag.getFloat("Scale"));
        }
        if (tag.contains("OwnerId")) {
            this.setOwnerId(tag.getInt("OwnerId"));
        }
        if (tag.contains("Forward")) {
            this.setForwardOffset(tag.getFloat("Forward"));
        }
        if (tag.contains("Up")) {
            this.setUpOffset(tag.getFloat("Up"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.getLifetime());
        tag.putFloat("Scale", this.getScale());
        tag.putInt("OwnerId", this.entityData.get(DATA_OWNER_ID));
        tag.putFloat("Forward", getForwardOffset());
        tag.putFloat("Up", getUpOffset());
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

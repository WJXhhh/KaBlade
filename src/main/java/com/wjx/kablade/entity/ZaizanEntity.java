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
import net.minecraftforge.network.NetworkHooks;

/** Synced visual anchor for the Zaizan slash art. */
public final class ZaizanEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(ZaizanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(ZaizanEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(ZaizanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FORWARD =
            SynchedEntityData.defineId(ZaizanEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP =
            SynchedEntityData.defineId(ZaizanEntity.class, EntityDataSerializers.FLOAT);

    public ZaizanEntity(EntityType<? extends ZaizanEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static ZaizanEntity spawn(Level level, LivingEntity owner, double forwardOffset,
                                     double upOffset, int lifetime, float scale) {
        ZaizanEntity entity = new ZaizanEntity(ModEntities.ZAIZAN.get(), level);
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
        this.entityData.define(DATA_LIFETIME, 30);
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

    public Entity getOwner() {
        int id = this.entityData.get(DATA_OWNER_ID);
        return id >= 0 ? this.level().getEntity(id) : null;
    }

    public float getForwardOffset() {
        return this.entityData.get(DATA_FORWARD);
    }

    public void setForwardOffset(float forward) {
        this.entityData.set(DATA_FORWARD, forward);
    }

    public float getUpOffset() {
        return this.entityData.get(DATA_UP);
    }

    public void setUpOffset(float up) {
        this.entityData.set(DATA_UP, up);
    }

    private void followOwner() {
        Entity owner = getOwner();
        if (owner == null) {
            return;
        }
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yawRad);
        double fz = Mth.cos(yawRad);
        this.setPos(owner.getX() + fx * getForwardOffset(),
                owner.getY() + getUpOffset(),
                owner.getZ() + fz * getForwardOffset());
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
        return super.getBoundingBoxForCulling().inflate(9.0, 4.0, 9.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Lifetime")) {
            setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("Scale")) {
            setScale(tag.getFloat("Scale"));
        }
        if (tag.contains("OwnerId")) {
            setOwnerId(tag.getInt("OwnerId"));
        }
        if (tag.contains("Forward")) {
            setForwardOffset(tag.getFloat("Forward"));
        }
        if (tag.contains("Up")) {
            setUpOffset(tag.getFloat("Up"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", getLifetime());
        tag.putFloat("Scale", getScale());
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

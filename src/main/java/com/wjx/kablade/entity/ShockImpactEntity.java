package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Visual anchor for Shock Impact.
 */
public class ShockImpactEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(ShockImpactEntity.class, EntityDataSerializers.FLOAT);

    public ShockImpactEntity(EntityType<? extends ShockImpactEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static ShockImpactEntity spawn(Level level, LivingEntity owner, Vec3 pos,
                                          int lifetime, float scale) {
        ShockImpactEntity entity = new ShockImpactEntity(ModEntities.SHOCK_IMPACT.get(), level);
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setYRot(owner.getYRot());
        entity.setLifetime(lifetime);
        entity.setScale(scale);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 34);
        this.entityData.define(DATA_SCALE, 1.0F);
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

    @Override
    public void tick() {
        super.tick();
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.getLifetime());
        tag.putFloat("Scale", this.getScale());
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

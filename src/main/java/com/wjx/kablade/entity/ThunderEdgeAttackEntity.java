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
import net.minecraftforge.network.NetworkHooks;

/** Short-lived visual anchor for Thunder Edge, the Key of Castigation slash art. */
public class ThunderEdgeAttackEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(ThunderEdgeAttackEntity.class, EntityDataSerializers.INT);

    public ThunderEdgeAttackEntity(EntityType<? extends ThunderEdgeAttackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        // The rendered crescent is much larger than the registry hitbox.
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static ThunderEdgeAttackEntity spawn(Level level, LivingEntity owner, int lifetime) {
        ThunderEdgeAttackEntity entity = new ThunderEdgeAttackEntity(ModEntities.THUNDER_EDGE_ATTACK.get(), level);
        entity.setPos(owner.getX(), owner.getY(), owner.getZ());
        entity.setYRot(owner.getYRot());
        entity.setLifetime(lifetime);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 24);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.getLifetime());
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

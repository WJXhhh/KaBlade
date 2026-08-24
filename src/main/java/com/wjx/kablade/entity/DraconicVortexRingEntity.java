package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 「魔龙旋斩」的环形刀光表现实体。
 * 只负责同步位置/朝向/寿命；几何由客户端渲染器程序化绘制。
 */
public class DraconicVortexRingEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(DraconicVortexRingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(DraconicVortexRingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(DraconicVortexRingEntity.class, EntityDataSerializers.INT);

    public DraconicVortexRingEntity(EntityType<? extends DraconicVortexRingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static DraconicVortexRingEntity spawn(Level level, double x, double y, double z,
                                                 float yaw, float size, int lifetime, int color) {
        DraconicVortexRingEntity ring = new DraconicVortexRingEntity(ModEntities.DRACONIC_VORTEX_RING.get(), level);
        ring.setPos(x, y, z);
        ring.setYRot(yaw);
        ring.setSize(size);
        ring.setLifetime(lifetime);
        ring.setColor(color);
        level.addFreshEntity(ring);
        return ring;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 16);
        this.entityData.define(DATA_SIZE, 1.0F);
        this.entityData.define(DATA_COLOR, 0xF7FBFF);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int v) {
        this.entityData.set(DATA_LIFETIME, v);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSize(float v) {
        this.entityData.set(DATA_SIZE, v);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int v) {
        this.entityData.set(DATA_COLOR, v);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= this.getLifetime()) {
            this.discard();
        }
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

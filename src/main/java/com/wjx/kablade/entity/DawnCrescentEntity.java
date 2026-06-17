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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 破晓弧月 —— 「弧光破晓」专属表现实体。一弯实心炽亮的金色新月（弧光），向前飞掠并张大。
 * 纯表现（不碰撞/不重力/不存盘）；几何在 {@code DawnCrescentRenderer} 里用 {@code RenderType.lightning()}
 * 程序化绘制成一弯渐尖的月牙。伤害由 SA 的前方扫描负责。
 */
public class DawnCrescentEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(DawnCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(DawnCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(DawnCrescentEntity.class, EntityDataSerializers.INT);

    public DawnCrescentEntity(EntityType<? extends DawnCrescentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static DawnCrescentEntity spawn(Level level, double x, double y, double z,
                                           float yaw, Vec3 velocity, float size, int lifetime, int color) {
        DawnCrescentEntity c = new DawnCrescentEntity(ModEntities.DAWN_CRESCENT.get(), level);
        c.setPos(x, y, z);
        c.setYRot(yaw);
        c.setLifetime(lifetime);
        c.setSize(size);
        c.setColor(color);
        c.setDeltaMovement(velocity);
        level.addFreshEntity(c);
        return c;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 16);
        this.entityData.define(DATA_SIZE, 1.0F);
        this.entityData.define(DATA_COLOR, 0xFFC83C);
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
            return;
        }
        Vec3 m = this.getDeltaMovement();
        if (m.lengthSqr() > 1.0e-9) {
            this.setPos(this.getX() + m.x, this.getY() + m.y, this.getZ() + m.z);
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

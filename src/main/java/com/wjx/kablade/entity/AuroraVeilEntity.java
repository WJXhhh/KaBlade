package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 极光帷幕 —— 「极光映天」专属表现实体。一道随时间起伏、颜色流转的半透明极光帷幕，向前飘扫。
 * 纯表现（不碰撞/不重力/不存盘）；几何与波动全在 {@code AuroraVeilRenderer} 里用 {@code RenderType.lightning()}
 * 程序化绘制。伤害由 SA 的前方扫描负责。
 */
public class AuroraVeilEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(AuroraVeilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(AuroraVeilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(AuroraVeilEntity.class, EntityDataSerializers.INT);

    public AuroraVeilEntity(EntityType<? extends AuroraVeilEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static AuroraVeilEntity spawn(Level level, double x, double y, double z,
                                         float yaw, Vec3 velocity, float size, int lifetime, int seed) {
        AuroraVeilEntity veil = new AuroraVeilEntity(ModEntities.AURORA_VEIL.get(), level);
        veil.setPos(x, y, z);
        veil.setYRot(yaw);
        veil.setLifetime(lifetime);
        veil.setSize(size);
        veil.setSeed(seed);
        veil.setDeltaMovement(velocity);
        level.addFreshEntity(veil);
        return veil;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 24);
        this.entityData.define(DATA_SIZE, 1.0F);
        this.entityData.define(DATA_SEED, 0);
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

    public int getSeed() {
        return this.entityData.get(DATA_SEED);
    }

    public void setSeed(int v) {
        this.entityData.set(DATA_SEED, v);
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

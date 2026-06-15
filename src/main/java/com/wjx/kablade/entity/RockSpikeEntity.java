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
 * 岩刺实体 —— 「岩石撼击」从地面顶出来的那根石矛，纯表现实体（不碰撞、不受重力、不存盘）。
 * <p>
 * 几何与动画都在客户端 {@code RockSpikeModel}/{@code RockSpikeRenderer} 里手搓：它会在出生头几 tick
 * 从地下顶出、停留、再缩回。伤害与击飞由 {@code RockStrikeArts} 的 AABB 扫描负责，本实体不参与战斗逻辑，
 * 因此「先结算伤害再击飞」的顺序不受影响。
 * <p>
 * 同步字段：{@link #DATA_LIFETIME} 总存活 tick、{@link #DATA_SCALE} 体型、{@link #DATA_VARIANT} 外形种子
 * （决定倾斜角等随机细节，保证两端一致）。
 */
public class RockSpikeEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(RockSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(RockSpikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(RockSpikeEntity.class, EntityDataSerializers.INT);

    private static final int DEFAULT_LIFETIME = 18;

    public RockSpikeEntity(EntityType<? extends RockSpikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** SlashArts 侧用：在指定地面位置、朝向、体型生成一根岩刺。 */
    public static RockSpikeEntity spawn(Level level, double x, double y, double z,
                                        float yaw, float scale, int lifetime, int variant) {
        RockSpikeEntity spike = new RockSpikeEntity(ModEntities.ROCK_SPIKE.get(), level);
        spike.setPos(x, y, z);
        spike.setYRot(yaw);
        spike.setScaleFactor(scale);
        spike.setLifetime(lifetime);
        spike.setVariant(variant);
        level.addFreshEntity(spike);
        return spike;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, DEFAULT_LIFETIME);
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_VARIANT, 0);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    public float getScaleFactor() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setScaleFactor(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, variant);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    // 纯表现实体：不存盘（EntityType 也设了 noSave），故读写存档留空。
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        // 非生物自定义实体必须自带生成包，否则客户端不会出现。
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

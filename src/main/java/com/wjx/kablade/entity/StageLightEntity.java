package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.KabladeCapabilities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

/**
 * 「聚光舞台」的同步锚点。伤害由 Slash Art 结算；该实体只同步固定落点、朝向和演出时长，
 * 所有光环、追光与星芒都由客户端着色器程序化绘制。
 */
public final class StageLightEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(StageLightEntity.class, EntityDataSerializers.INT);

    /* package-private */ static final double BUFF_RANGE = 6.25;
    private static final double BUFF_VERTICAL_RANGE = 3.0;
    private static final int BUFF_TICKS = 10; // 0.5 秒，持续刷新所以离开后半秒消退

    public StageLightEntity(EntityType<? extends StageLightEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static StageLightEntity spawn(Level level, double x, double y, double z,
                                         float yaw, int lifetime) {
        StageLightEntity stage = new StageLightEntity(ModEntities.STAGE_LIGHT.get(), level);
        stage.setPos(x, y, z);
        stage.setYRot(yaw);
        stage.setLifetime(lifetime);
        level.addFreshEntity(stage);
        return stage;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 80);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            // 对范围内玩家写入聚光舞台 property，handler 据此施加属性增益
            AABB bounds = AABB.ofSize(this.position(), BUFF_RANGE * 2.0,
                    BUFF_VERTICAL_RANGE * 2.0, BUFF_RANGE * 2.0);
            for (Player player : this.level().getEntitiesOfClass(Player.class, bounds,
                    p -> p.isAlive() && p.distanceToSqr(this) <= BUFF_RANGE * BUFF_RANGE)) {
                player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                        .ifPresent(data -> data.set("stage_light", BUFF_TICKS));
            }

            if (this.tickCount >= this.getLifetime()) {
                this.discard();
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(7.0, 3.0, 7.0);
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

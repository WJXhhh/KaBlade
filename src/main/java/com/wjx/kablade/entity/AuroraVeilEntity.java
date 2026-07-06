package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 极光帷幕 —— 「极光映天」专属实体。一道随时间起伏、颜色流转的半透明极光帷幕，向前飘扫。
 * 不碰撞/不重力/不存盘；几何与波动全在 {@code AuroraVeilRenderer} 里用 {@code RenderType.lightning()} 程序化绘制。
 * <p>
 * 推进途中会「扫过即伤」：覆盖范围内每个敌人被同一道帷幕只结算一次无视护甲伤害（{@link #alreadyHit} 去重），
 * 伤害值与归属者在 {@link #spawn} 时由 SA 传入。
 */
public class AuroraVeilEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(AuroraVeilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(AuroraVeilEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(AuroraVeilEntity.class, EntityDataSerializers.INT);

    // 仅服务端：伤害值、归属者、已命中去重（帷幕扫过每个敌人只结算一次）。
    private float damage = 0.0F;
    private LivingEntity owner = null;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AuroraVeilEntity(EntityType<? extends AuroraVeilEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static AuroraVeilEntity spawn(Level level, double x, double y, double z,
                                         float yaw, Vec3 velocity, float size, int lifetime, int seed,
                                         LivingEntity owner, float damage) {
        AuroraVeilEntity veil = new AuroraVeilEntity(ModEntities.AURORA_VEIL.get(), level);
        veil.setPos(x, y, z);
        veil.setYRot(yaw);
        veil.setLifetime(lifetime);
        veil.setSize(size);
        veil.setSeed(seed);
        veil.setDeltaMovement(velocity);
        veil.owner = owner;
        veil.damage = damage;
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
        if (!this.level().isClientSide()) {
            if (this.tickCount >= this.getLifetime()) {
                this.discard();
                return;
            }
            sweepDamage();
        }
        Vec3 m = this.getDeltaMovement();
        if (m.lengthSqr() > 1.0e-9) {
            this.setPos(this.getX() + m.x, this.getY() + m.y, this.getZ() + m.z);
        }
    }

    /** 帷幕推进时，对其覆盖范围内、尚未被本帷幕命中过的敌人各结算一次无视护甲伤害。 */
    private void sweepDamage() {
        if (this.damage <= 0.0F || this.owner == null) {
            return;
        }
        double hw = 3.0 * this.getSize();   // 半宽（与渲染帷幕宽度同量级）
        double h = 4.0 * this.getSize();    // 高
        AABB box = new AABB(this.getX() - hw, this.getY() - 0.5, this.getZ() - hw,
                this.getX() + hw, this.getY() + h, this.getZ() + hw);
        DamageSource src = this.level().damageSources().indirectMagic(this, this.owner);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> SaTargeting.canDamage(this.owner, e))) {
            if (this.alreadyHit.add(target.getUUID())) {
                target.hurt(src, this.damage);
            }
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

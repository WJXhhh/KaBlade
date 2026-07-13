package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
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
 * 破晓弧月 —— 「弧光破晓」专属实体。一弯实心炽亮的金色新月（弧光），向前飞掠并张大。
 * 不碰撞/不重力/不存盘；几何在 {@code DawnCrescentRenderer} 里用 {@code RenderType.lightning()} 程序化绘制。
 * <p>
 * 飞掠途中「扫过即伤」：覆盖范围内每个敌人被同一弯弧月只结算一次无视护甲伤害（{@link #alreadyHit} 去重），
 * 伤害值与归属者在 {@link #spawn} 时由 SA 传入。
 */
public class DawnCrescentEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(DawnCrescentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(DawnCrescentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(DawnCrescentEntity.class, EntityDataSerializers.INT);

    // 仅服务端：伤害值、归属者、已命中去重。
    private float damage = 0.0F;
    private LivingEntity owner = null;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public DawnCrescentEntity(EntityType<? extends DawnCrescentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static DawnCrescentEntity spawn(Level level, double x, double y, double z,
                                           float yaw, Vec3 velocity, float size, int lifetime, int color,
                                           LivingEntity owner, float damage) {
        DawnCrescentEntity c = new DawnCrescentEntity(ModEntities.DAWN_CRESCENT.get(), level);
        c.setPos(x, y, z);
        c.setYRot(yaw);
        c.setLifetime(lifetime);
        c.setSize(size);
        c.setColor(color);
        c.setDeltaMovement(velocity);
        c.owner = owner;
        c.damage = damage;
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

    /** 弧月飞掠时，对其覆盖范围内、尚未被本弧月命中过的敌人各结算一次无视护甲伤害。 */
    private void sweepDamage() {
        if (this.damage <= 0.0F || this.owner == null) {
            return;
        }
        double rad = 2.6 * this.getSize();   // 与渲染外半径同量级的水平覆盖
        AABB box = new AABB(this.getX() - rad, this.getY() - 1.0, this.getZ() - rad,
                this.getX() + rad, this.getY() + 2.0, this.getZ() + rad);
        DamageSource src = this.level().damageSources().indirectMagic(this, this.owner);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> SaTargeting.canDamage(this.owner, e))) {
            if (this.alreadyHit.add(target.getUUID())) {
                com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, (ServerLevel) this.level(), this, this.owner, this.damage);
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

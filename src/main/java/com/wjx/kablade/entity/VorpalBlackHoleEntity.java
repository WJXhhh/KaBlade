package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import mods.flammpfeil.slashblade.entity.IShootable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 反力场黑洞 —— 「反力场打刀11式」SA 召唤的牵引黑洞。
 * <p>
 * 从 1.12.2 {@code EntityVorpalBlackHole} 移植而来：
 * 在玩家位置生成，存在约 1 秒，持续将周围生物拉向中心，
 * 贴近中心的实体受到伤害并被定身。
 */
public class VorpalBlackHoleEntity extends Entity {

    private static final int LIFETIME = 20;
    private static final double PULL_RADIUS_H = 10.0;
    private static final double PULL_RADIUS_V = 5.0;
    private static final double CATCH_SPEED = 2.2;
    private static final double PULL_MIN_DISTANCE = 0.75;
    private static final double DAMAGE_RADIUS = 0.5;
    private static final float DAMAGE = 3.0F;

    private LivingEntity thrower;

    public VorpalBlackHoleEntity(EntityType<? extends VorpalBlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public VorpalBlackHoleEntity(Level level, LivingEntity thrower, double x, double y, double z) {
        this(ModEntities.VORPAL_BLACK_HOLE.get(), level);
        this.thrower = thrower;
        this.setPos(x, y, z);
    }

    public static VorpalBlackHoleEntity spawn(Level level, LivingEntity thrower, double x, double y, double z) {
        VorpalBlackHoleEntity hole = new VorpalBlackHoleEntity(level, thrower, x, y, z);
        level.addFreshEntity(hole);
        return hole;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        Level level = this.level();

        // 牵引：范围内的生物/投射物被拉向中心（向量算法照搬 1.12.2：按最大轴分量归一化 × catchSpeed / 距离）。
        AABB pullBox = this.getBoundingBox()
                .inflate(PULL_RADIUS_H, PULL_RADIUS_V, PULL_RADIUS_H)
                .move(this.getDeltaMovement());
        for (Entity e : level.getEntities(this, pullBox, this::canAffect)) {
            double dist = this.distanceTo(e);
            if (dist >= PULL_MIN_DISTANCE) {
                double dx = this.getX() - e.getX();
                double dy = this.getY() - e.getY();
                double dz = this.getZ() - e.getZ();
                double per1 = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                double disCount = 1.0 / dist;
                e.setDeltaMovement(dx / per1 * CATCH_SPEED * disCount,
                        dy / per1 * CATCH_SPEED * disCount,
                        dz / per1 * CATCH_SPEED * disCount);
                e.hurtMarked = true;
            }
        }

        // 切割：贴近中心（0.5 格内）的目标被定身并受伤。
        AABB damageBox = this.getBoundingBox()
                .inflate(DAMAGE_RADIUS)
                .move(this.getDeltaMovement());
        DamageSource src = this.thrower != null
                ? level.damageSources().mobAttack(this.thrower)
                : level.damageSources().generic();
        for (Entity e : level.getEntities(this, damageBox, this::canAffect)) {
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
            e.hurt(src, DAMAGE);
        }

        // 寿命 20 tick（与 1.12.2 一致：结算完本 tick 效果后再判定消失）。
        if (this.tickCount > LIFETIME) {
            this.discard();
        }
    }

    /**
     * 影响判定，一比一复刻 1.12.2 的谓词 {@code p2}：
     * 排除自身、施法者、以及施法者自己发出的投射物（召唤剑等）；创造/旁观玩家不受影响；
     * 只作用于生物或可发射投射物，掉落物/经验球等不受牵引。
     */
    private boolean canAffect(Entity e) {
        if (e == this || e == this.thrower) {
            return false;
        }
        if (e instanceof IShootable shootable && shootable.getShooter() == this.thrower) {
            return false;
        }
        if (e instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return e instanceof LivingEntity || e instanceof IShootable;
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

package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 幻影剑Ex —— 1.12.2 {@code EntityPhantomSwordEx} 的移植。
 * <p>
 * 在延迟（interval）后追踪目标，命中时"骑乘"目标并持续造成伤害。
 */
public class PhantomSwordExEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_INTERVAL =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_THROWER_ID =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MOUNTED_ID =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_INI_YAW =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_INI_PITCH =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(PhantomSwordExEntity.class, EntityDataSerializers.FLOAT);

    protected LivingEntity thrower;
    protected ItemStack blade = ItemStack.EMPTY;
    protected float attackDamage;
    protected final List<UUID> alreadyHit = new ArrayList<>();

    /** 当前骑乘的目标（1.12.2 ridingEntity2） */
    private Entity mountedTarget;
    /** 骑乘偏移 */
    private double hitX, hitY, hitZ;
    private float hitYaw, hitPitch;
    protected boolean initIniYP = false;
    protected float iniYaw, iniPitch;

    // ── 调试/视觉效果辅助 ──
    private static final double HIT_AMBIT = 1.0;

    public PhantomSwordExEntity(EntityType<? extends PhantomSwordExEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** 工厂方法。 */
    public static PhantomSwordExEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                             Vec3 direction, float damage, int color,
                                             int lifetime, int interval, float iniYaw, float iniPitch) {
        PhantomSwordExEntity e = new PhantomSwordExEntity(ModEntities.PHANTOM_SWORD_EX.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.setYRot(iniYaw);
        e.setXRot(iniPitch);
        e.setDeltaMovement(direction);
        e.setThrower(thrower);
        e.attackDamage = damage;
        e.setColor(color);
        e.setLifetime(lifetime);
        e.setInterval(interval);
        e.setIniYaw(iniYaw);
        e.setIniPitch(iniPitch);
        e.setRoll(90.0F);
        e.initializeTrajectory(iniYaw, iniPitch, direction);
        e.blade = thrower.getMainHandItem();
        if (thrower != null) e.alreadyHit.add(thrower.getUUID());
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_LIFETIME, 30);
        entityData.define(DATA_INTERVAL, 7);
        entityData.define(DATA_COLOR, 0x3355647);
        entityData.define(DATA_TARGET_ID, 0);
        entityData.define(DATA_THROWER_ID, 0);
        entityData.define(DATA_MOUNTED_ID, 0);
        entityData.define(DATA_INI_YAW, 0.0F);
        entityData.define(DATA_INI_PITCH, -720.0F);
        entityData.define(DATA_ROLL, 90.0F);
    }

    public int getLifetime()              { return entityData.get(DATA_LIFETIME); }
    public void setLifetime(int v)        { entityData.set(DATA_LIFETIME, v); }
    public int getInterval()              { return entityData.get(DATA_INTERVAL); }
    public void setInterval(int v)        { entityData.set(DATA_INTERVAL, v); }
    public int getColor()                 { return entityData.get(DATA_COLOR); }
    public void setColor(int v)           { entityData.set(DATA_COLOR, v); }
    public int getTargetEntityId()        { return entityData.get(DATA_TARGET_ID); }
    public void setTargetEntityId(int v)  { entityData.set(DATA_TARGET_ID, v); }
    public int getThrowerEntityId()       { return entityData.get(DATA_THROWER_ID); }
    public int getMountedEntityId()       { return entityData.get(DATA_MOUNTED_ID); }
    public float getIniYaw()              { return entityData.get(DATA_INI_YAW); }
    public void setIniYaw(float v)        { entityData.set(DATA_INI_YAW, v); }
    public float getIniPitch()            { return entityData.get(DATA_INI_PITCH); }
    public void setIniPitch(float v)      { entityData.set(DATA_INI_PITCH, v); }
    public float getRoll()                { return entityData.get(DATA_ROLL); }
    public void setRoll(float v)           { entityData.set(DATA_ROLL, v); }

    protected void setThrower(LivingEntity value) {
        thrower = value;
        entityData.set(DATA_THROWER_ID, value == null ? 0 : value.getId());
    }

    public void setTarget(LivingEntity target) {
        setTargetEntityId(target.getId());
    }

    @Override
    public void tick() {
        super.tick();

        resolveSyncedEntities();

        // The owner is assigned directly on the server and reconstructed from synced data on
        // the client. Never discard a client copy merely because its owner packet arrived later.
        if (!level().isClientSide() && (thrower == null || !thrower.isAlive())) {
            discard();
            return;
        }

        if (tickCount >= getLifetime()) {
            if (mountedTarget != null) {
                // 骑乘结束时对目标造成终结伤害
                if (mountedTarget.isAlive()) {
                    doFinalHit((LivingEntity) mountedTarget);
                }
            }
            discard();
            return;
        }

        if (mountedTarget != null) {
            tickRidden();
        } else {
            tickFlying();
        }
    }

    /** 飞行中的 tick 逻辑。 */
    protected void tickFlying() {
        // 目标追逐
        if (getInterval() > 0 && tickCount <= getInterval()) {
            // 延迟期：只做旋转追踪，不位移
            doTargeting();
        } else {
            // 移动期：直接应用当前 motion（doTargeting 已设好速度 1.75）
            Vec3 motion = getDeltaMovement();
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        }

        // 碰撞方块检测
        BlockPos blockPos = blockPosition();
        if (!level().getBlockState(blockPos).getCollisionShape(level(), blockPos).isEmpty()) {
            discard();
            return;
        }

        // 命中检测（服务端）
        if (!level().isClientSide()) {
            findTarget().ifPresent(this::onHitEntity);
        }
    }

    /**
     * 命中目标时的回调 —— 子类可重写以改变行为（例如闪电剑生成闪电）。
     * 默认行为：附加推力 + 骑乘目标。
     */
    protected void onHitEntity(LivingEntity target) {
        target.invulnerableTime = 0;
        com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, damageSource(), attackDamage);
        target.setDeltaMovement(0.0, 0.1, 0.0);
        target.hurtMarked = true;
        hitBlade(target);
        mountEntity(target);
    }

    /** 在延迟期旋转对准目标。 */
    protected void doTargeting() {
        int targetId = getTargetEntityId();
        if (targetId == 0) return;
        Entity target = level().getEntity(targetId);
        if (target == null) return;

        if (!initIniYP) {
            initIniYP = true;
            if (getIniPitch() < -700.0F) {
                if (thrower == null) return;
                iniYaw = thrower.getYRot();
                iniPitch = thrower.getXRot();
            } else {
                iniYaw = getIniYaw();
                iniPitch = getIniPitch();
            }
        }

        faceEntity(this, target, tickCount, tickCount);
        setDriveVectorFromIni(1.75F);
    }

    protected void faceEntity(Entity viewer, Entity target, float yawStep, float pitchStep) {
        double dx = target.getX() - viewer.getX();
        double dz = target.getZ() - viewer.getZ();
        double dy = (target instanceof LivingEntity lt
                ? lt.getEyeY()
                : (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0)
                - viewer.getEyeY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) (-Mth.atan2(dy, dist) * Mth.RAD_TO_DEG);
        iniPitch = updateRotation(iniPitch, targetPitch, pitchStep);
        iniYaw = updateRotation(iniYaw, targetYaw, yawStep);
    }

    protected void setDriveVectorFromIni(float speed) {
        float yawRad = iniYaw * ((float) Math.PI / 180.0F);
        float pitchRad = iniPitch * ((float) Math.PI / 180.0F);
        double mx = -Mth.sin(yawRad) * Mth.cos(pitchRad) * speed;
        double my = -Mth.sin(pitchRad) * speed;
        double mz = Mth.cos(yawRad) * Mth.cos(pitchRad) * speed;
        setDeltaMovement(mx, my, mz);
        float horizontal = Mth.sqrt((float) (mx * mx + mz * mz));
        setYRot((float) (Mth.atan2(mx, mz) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(my, horizontal) * Mth.RAD_TO_DEG));
        hasImpulse = true;
    }

    protected void initializeTrajectory(float yaw, float pitch, Vec3 direction) {
        iniYaw = yaw;
        iniPitch = pitch;
        initIniYP = true;
        if (direction.lengthSqr() > 1.0E-8D) {
            setDeltaMovement(direction);
            double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            setYRot((float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
            setXRot((float) (Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG));
            hasImpulse = true;
        } else {
            setDriveVectorFromIni(1.75F);
        }
        yRotO = getYRot();
        xRotO = getXRot();
    }

    /** 寻找可命中的目标。 */
    protected Optional<LivingEntity> findTarget() {
        double ambit = HIT_AMBIT;
        AABB bb = getBoundingBox().inflate(ambit);
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, bb,
                e -> SaTargeting.canDamage(thrower, e) && !alreadyHit.contains(e.getUUID()));
        // 优先命中锁定的目标
        int targetId = getTargetEntityId();
        if (targetId != 0) {
            Entity locked = level().getEntity(targetId);
            if (locked instanceof LivingEntity lt && list.contains(lt)) {
                return Optional.of(lt);
            }
        }
        // 否则最近优先
        double closest = Double.MAX_VALUE;
        LivingEntity result = null;
        for (LivingEntity e : list) {
            double d = distanceToSqr(e);
            if (d < closest) {
                closest = d;
                result = e;
            }
        }
        return Optional.ofNullable(result);
    }

    /** 骑乘目标。 */
    protected void mountEntity(Entity target) {
        if (target == null) return;
        hitYaw = getYRot() - target.getYRot();
        hitPitch = getXRot() - target.getXRot();
        hitX = getX() - target.getX();
        hitY = getY() - target.getY();
        hitZ = getZ() - target.getZ();
        mountedTarget = target;
        entityData.set(DATA_MOUNTED_ID, target.getId());
        // 骑乘时剩余寿命缩短到 20 tick
        int remaining = getLifetime() - tickCount;
        if (remaining > 20) {
            setLifetime(tickCount + 20);
        }
    }

    /** 骑乘中的 tick 逻辑。 */
    protected void tickRidden() {
        if (mountedTarget == null || !mountedTarget.isAlive()) {
            discard();
            return;
        }

        // 跟随骑乘目标
        double nx = mountedTarget.getX() + hitX * Mth.cos(mountedTarget.getYRot() * ((float) Math.PI / 180.0F))
                - hitZ * Mth.sin(mountedTarget.getYRot() * ((float) Math.PI / 180.0F));
        double ny = mountedTarget.getY() + hitY;
        double nz = mountedTarget.getZ() + hitX * Mth.sin(mountedTarget.getYRot() * ((float) Math.PI / 180.0F))
                + hitZ * Mth.cos(mountedTarget.getYRot() * ((float) Math.PI / 180.0F));
        setPos(nx, ny, nz);
        setYRot(mountedTarget.getYRot() + hitYaw);
        setXRot(mountedTarget.getXRot() + hitPitch);
    }

    private void resolveSyncedEntities() {
        if (thrower == null) {
            Entity owner = level().getEntity(getThrowerEntityId());
            if (owner instanceof LivingEntity living) {
                thrower = living;
            }
        }

        if (mountedTarget == null && getMountedEntityId() != 0) {
            Entity target = level().getEntity(getMountedEntityId());
            if (target != null) {
                hitYaw = getYRot() - target.getYRot();
                hitPitch = getXRot() - target.getXRot();
                hitX = getX() - target.getX();
                hitY = getY() - target.getY();
                hitZ = getZ() - target.getZ();
                mountedTarget = target;
            }
        }
    }

    /** 骑乘结束时对目标造成终结伤害（默认每20 tick触发一次额外伤害）。 */
    protected void doFinalHit(LivingEntity target) {
        if (!SaTargeting.canDamage(thrower, target)) {
            return;
        }
        target.invulnerableTime = 0;
        com.wjx.kablade.util.SaDamage.hurtNoIFrame(target,
                damageSource(), Math.max(attackDamage / 2.0F, 1.0F));
        hitBlade(target);
    }

    protected DamageSource damageSource() {
        return level().damageSources().indirectMagic(this, thrower);
    }

    protected void hitBlade(LivingEntity target) {
        // 触发 blade hitEntity 效果（由 mixin 或 capability 处理）
    }

    // ── 工具 ──────────────────────────────────────────────────
    private static float updateRotation(float current, float target, float step) {
        float diff = Mth.wrapDegrees(target - current);
        if (diff > step) diff = step;
        if (diff < -step) diff = -step;
        return current + diff;
    }

    // ── NBT ──────────────────────────────────────────────────
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

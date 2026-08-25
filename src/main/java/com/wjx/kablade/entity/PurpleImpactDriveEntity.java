package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 深紫冲击（Purple Impact）专用驱动实体。
 * <p>
 * 基于 {@link LacerateDriveEntity}，将刀光颜色替换为深紫色（0x8A2BE2）。
 * 特性：AABB 范围命中、可破坏物销毁、EXPLOSION_NORMAL 粒子、速度曲线、防具穿透魔法伤害。
 */
public class PurpleImpactDriveEntity extends ExSlashDriveEntity {

    public PurpleImpactDriveEntity(EntityType<? extends PurpleImpactDriveEntity> type, Level level) {
        super(type, level);
    }

    /**
     * 工厂方法 —— 创建深紫冲击驱动实体并加入世界。
     *
     * @param level      世界
     * @param thrower    施术者
     * @param pos        生成位置
     * @param direction  飞行方向（用于朝向 + deltaMovement）
     * @param damage     伤害值
     * @param roll       旋转角
     */
    public static PurpleImpactDriveEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                               Vec3 direction, float damage, float roll) {
        PurpleImpactDriveEntity e = new PurpleImpactDriveEntity(ModEntities.PURPLE_IMPACT_DRIVE.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(direction);
        e.applyHeading(direction);
        e.thrower = thrower;
        e.attackDamage = damage;
        e.blade = thrower.getMainHandItem();
        if (thrower != null) {
            e.alreadyHit.add(thrower.getUUID());
        }

        // 深紫色刀光
        e.setColor(0x8A2BE2);
        // roll（随机视觉翻滚角）
        e.setRoll(roll);
        // SCALE_Y = 2.0（放大两倍）
        e.setScaleY(2.0F);
        // 90 tick 寿命
        e.setLifetime(90);
        // 初始速度 = 0.101f（慢速飘移）
        e.setInitialSpeed(0.101F);
        // changeTime = 1 → 第 1 tick 切换到 nextSpeed
        e.setChangeTime(1);
        // nextSpeed = 1.3f → 切换后加速
        e.setNextSpeed(1.3F);
        // 启用可破坏物销毁
        e.setDestroyDestructible(true);
        // 关闭多段命中
        e.setMultiHit(false);
        // 粒子：EXPLOSION_NORMAL
        e.setParticleEnabled(true);
        e.setParticleStyle("EXPLOSION_NORMAL");

        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected DamageSource damageSource() {
        return level().damageSources().indirectMagic(this, thrower);
    }

    @Override
    protected void onImpact(Entity target) {
        SaDamage.hurtNoIFrame(target, damageSource(), Math.max(attackDamage, 1.0F));
        SaTarget.of(target).map(SaTarget::root).ifPresent(this::hitBlade);
    }
}

package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 撕裂灵刃（Lacerate Blade）专用驱动实体 —— 1.12.2 {@code EntityDriveAdd} 的移植。
 * <p>
 * 特性：AABB 范围命中、可破坏物销毁（方块/箭矢/火球）、EXPLOSION_NORMAL 粒子、
 * 速度曲线（慢速飘移 + 变速）、防具穿透（直接魔法伤害）。
 */
public class LacerateDriveEntity extends ExSlashDriveEntity {

    public LacerateDriveEntity(EntityType<? extends LacerateDriveEntity> type, Level level) {
        super(type, level);
    }

    /**
     * 工厂方法 —— 按 1.12.2 撕裂灵刃参数创建并加入世界。
     *
     * @param level      世界
     * @param thrower    施术者
     * @param pos        生成位置
     * @param direction  飞行方向（用于朝向 + deltaMovement）
     * @param damage     伤害值
     * @param roll       旋转角
     */
    public static LacerateDriveEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                            Vec3 direction, float damage, float roll) {
        LacerateDriveEntity e = new LacerateDriveEntity(ModEntities.LACERATE_DRIVE.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(direction);
        e.applyHeading(direction);
        e.thrower = thrower;
        e.attackDamage = damage;
        e.blade = thrower.getMainHandItem();
        if (thrower != null) {
            e.alreadyHit.add(thrower.getUUID());
        }

        // ── 1.12.2 参数映射 ──────────────────────────────────────
        // color: 0xCC3344 → R=0.8f, G=0.2f, B=0.267f (0x44/255≈0.267)
        e.setColor(0xCC3344);
        // roll（随机视觉翻滚角，1.12.2 传 a - swingDirection）
        e.setRoll(roll);
        // SCALE_Y = 2.0（放大两倍）
        e.setScaleY(2.0F);
        // 90 tick 寿命
        e.setLifetime(90);
        // 初始速度 = 0.101f（慢速飘移；initSpd < 1.05 → ExSlashDriveEntity 走 0.1 额外慢移分支）
        e.setInitialSpeed(0.101F);
        // changeTime = 1 → 第 1 tick 切换到 nextSpeed
        e.setChangeTime(1);
        // nextSpeed = 1.3f → 切换后加速
        e.setNextSpeed(1.3F);
        // 启用可破坏物销毁
        e.setDestroyDestructible(true);
        // 关闭多段命中（1.12.2 setIsMultihit(false)）
        e.setMultiHit(false);
        // 粒子：EXPLOSION_NORMAL
        e.setParticleEnabled(true);
        e.setParticleStyle("EXPLOSION_NORMAL");

        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected DamageSource damageSource() {
        // 直接魔法伤害 + 防具穿透，匹配 1.12.2 EntityDriveAdd 的伤害类型
        return level().damageSources().indirectMagic(this, thrower);
    }

    @Override
    protected void onImpact(LivingEntity target) {
        target.invulnerableTime = 0;
        // indirectMagic 在 1.20.1 已是魔法伤害（不计算护甲），等价 1.12.2 防具穿透
        com.wjx.kablade.util.SaDamage.hurtNoIFrame(target,
                damageSource(), Math.max(attackDamage, 1.0F));
        hitBlade(target);
    }
}

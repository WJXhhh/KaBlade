package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 苍流刃实体 —— 1.12.2 {@code EntityAquaEdge} 的移植。
 * <p>
 * 命中时造成溺水中等伤害，扑灭目标火焰，附带爆炸粒子（渲染器处理）。
 */
public class AquaEdgeEntity extends ExSlashDriveEntity {

    public AquaEdgeEntity(EntityType<? extends AquaEdgeEntity> type, Level level) {
        super(type, level);
    }

    /** 工厂方法。 */
    public static AquaEdgeEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                       Vec3 direction, float damage,
                                       int color, int lifetime, float roll, boolean multiHit) {
        AquaEdgeEntity e = new AquaEdgeEntity(ModEntities.AQUA_EDGE.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(direction);
        e.thrower = thrower;
        e.attackDamage = damage;
        e.setColor(color);
        e.setLifetime(lifetime);
        e.setRoll(roll);
        e.setMultiHit(multiHit);
        e.blade = thrower.getMainHandItem();
        if (thrower != null) e.alreadyHit.add(thrower.getUUID());
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void onImpact(LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurt(damageSource(), attackDamage);
        if (target.isOnFire()) {
            target.clearFire();
        }
        hitBlade(target);
    }

    @Override
    protected DamageSource damageSource() {
        // "drown" 类型伤害对应窒息/溺水伤害
        return level().damageSources().drown();
    }
}

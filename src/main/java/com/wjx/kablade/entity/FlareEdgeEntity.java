package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 熔岩驱动实体 —— 1.12.2 {@code EntityFlareEdge} 的移植。
 * <p>
 * 命中时附加火焰伤害（5秒灼烧），并阻止末影人传送。
 */
public class FlareEdgeEntity extends ExSlashDriveEntity {

    public FlareEdgeEntity(EntityType<? extends FlareEdgeEntity> type, Level level) {
        super(type, level);
    }

    /** 工厂方法。 */
    public static FlareEdgeEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                        Vec3 direction, float damage,
                                        int color, int lifetime, float roll, boolean multiHit) {
        FlareEdgeEntity e = new FlareEdgeEntity(ModEntities.FLARE_EDGE.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(direction);
        e.applyHeading(direction); // 出生即定向，使出生包携带正确 yRot/xRot
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
        com.wjx.kablade.util.SaDamage.hurtNoIFrame(target,
                damageSource(), Math.max(attackDamage / 2.0F, 1.0F));
        target.setRemainingFireTicks(100); // 5秒
        hitBlade(target);
    }
}

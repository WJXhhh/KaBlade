package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 闪电剑实体 —— 1.12.2 {@code EntityLightningSword} 的移植。
 * <p>
 * 命中时在目标位置生成一道真实闪电（包含伤害与点燃），然后自身消失。
 * 继承自 {@link PhantomSwordExEntity}，但重写 {@link #onHitEntity(LivingEntity)}
 * 以产生闪电而非骑乘目标。
 */
public class LightningSwordEntity extends PhantomSwordExEntity {

    public LightningSwordEntity(EntityType<? extends LightningSwordEntity> type, Level level) {
        super(type, level);
    }

    /** 工厂方法。 */
    public static LightningSwordEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                              Vec3 direction, float damage, int color,
                                              int lifetime, int interval, float iniYaw, float iniPitch) {
        LightningSwordEntity e = new LightningSwordEntity(ModEntities.LIGHTNING_SWORD.get(), level);
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
    protected void onHitEntity(LivingEntity target) {
        // 1.12.2 used a real lightning bolt here: it damaged and ignited the target.
        if (!level().isClientSide()) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
            if (bolt != null) {
                bolt.setPos(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(false);
                level().addFreshEntity(bolt);
            }
        }
        // The old lightning sword did not add a second direct-magic hit.
        discard();
    }
}

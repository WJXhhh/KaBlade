package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Phantom Butterfly's 1.12.2-style summoned butterfly sword. */
public class ButterflySwordEntity extends PhantomSwordExEntity {

    private static final double SPEED_GROWTH = 1.10D;
    private static final double MAX_SPEED = 4.0D;

    public ButterflySwordEntity(EntityType<? extends ButterflySwordEntity> type, Level level) {
        super(type, level);
    }

    public static ButterflySwordEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                             float damage, int color, int lifetime,
                                             int interval, float iniYaw, float iniPitch) {
        ButterflySwordEntity entity = new ButterflySwordEntity(ModEntities.BUTTERFLY_SWORD.get(), level);
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setYRot(iniYaw);
        entity.setXRot(iniPitch);
        entity.setThrower(thrower);
        entity.attackDamage = damage;
        entity.setColor(color);
        entity.setLifetime(lifetime);
        entity.setInterval(interval);
        entity.setIniYaw(iniYaw);
        entity.setIniPitch(iniPitch);
        entity.setRoll(0.0F);
        entity.initializeTrajectory(iniYaw, iniPitch, Vec3.ZERO);
        entity.blade = thrower.getMainHandItem();
        if (thrower != null) {
            entity.alreadyHit.add(thrower.getUUID());
        }
        level.addFreshEntity(entity);
        return entity;
    }

    public void setLegacyDriveSpeed(float speed) {
        setDriveVectorFromIni(speed);
    }

    @Override
    protected void tickFlying() {
        super.tickFlying();
        if (isRemoved() || tickCount <= getInterval()) {
            return;
        }

        Vec3 motion = getDeltaMovement();
        double length = motion.length();
        if (length <= 1.0E-8D) {
            return;
        }
        double next = Math.min(MAX_SPEED, length * SPEED_GROWTH);
        if (next > length) {
            setDeltaMovement(motion.scale(next / length));
            hasImpulse = true;
        }
    }
}

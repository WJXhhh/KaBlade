package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Short-lived, owner-bound homing missile; impacts never cause terrain damage. */
public final class TreasonMissileEntity extends PhantomSwordExEntity {
    public TreasonMissileEntity(EntityType<? extends TreasonMissileEntity> type, Level level) {
        super(type, level);
    }

    public static void spawn(LivingEntity owner, SaTarget target, Vec3 position,
                             Vec3 direction, float damage) {
        TreasonMissileEntity missile = new TreasonMissileEntity(ModEntities.TREASON_MISSILE.get(), owner.level());
        missile.setPos(position);
        missile.setThrower(owner);
        missile.attackDamage = damage;
        missile.blade = owner.getMainHandItem().copy();
        missile.setColor(0xFF3020);
        missile.setRoll(0);
        missile.setLifetime(60);
        missile.setInterval(0);
        missile.initializeTrajectory(0, 0, direction.scale(0.8D));
        if (target != null) missile.setTarget(target.hitEntity());
        owner.level().addFreshEntity(missile);
    }

    @Override
    protected void tickFlying() {
        if (level().isClientSide()) {
            Vec3 motion = getDeltaMovement();
            setPos(position().add(motion));
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(),
                    -motion.x * 0.08D, -motion.y * 0.08D, -motion.z * 0.08D);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        // The base tick cancels the missile if its owner is no longer available.
        Entity target = level().getEntity(getTargetEntityId());
        if (target != null && !SaTargeting.canDamageAttackable(thrower, target)) {
            setTargetEntityId(0);
            target = null;
        }
        Vec3 motion = getDeltaMovement();
        if (target != null) {
            Vec3 desired = SaTarget.center(target.getBoundingBox()).subtract(position()).normalize();
            motion = motion.normalize().scale(0.65D).add(desired.scale(0.35D)).normalize()
                    .scale(Math.min(1.35D, 0.8D + tickCount * 0.06D));
        }
        initializeTrajectory(0, 0, motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);
        var blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        Entity hit = null;
        Vec3 hitPosition = stop;
        double closest = start.distanceToSqr(stop);
        for (SaTarget candidate : SaTargeting.uniqueTargets(level(), thrower,
                new AABB(start, stop).inflate(0.3D), false)) {
            AABB box = candidate.hitEntity().getBoundingBox().inflate(0.2D);
            Vec3 point = box.contains(start) ? start : box.clip(start, stop).orElse(null);
            if (point != null && start.distanceToSqr(point) < closest) {
                closest = start.distanceToSqr(point);
                hit = candidate.hitEntity();
                hitPosition = point;
            }
        }
        setPos(hitPosition);
        if (hit != null) onHitEntity(hit);
        else if (blockHit.getType() != HitResult.Type.MISS) impact();
    }

    @Override
    protected void onHitEntity(Entity target) {
        if (thrower != null && thrower.isAlive() && SaTargeting.canDamage(thrower, target)) {
            SaDamage.hurtNoIFrame(target, damageSource(), attackDamage);
        }
        impact();
    }

    private void impact() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 12, 0.2D, 0.2D, 0.2D, 0.06D);
            server.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        discard();
    }
}

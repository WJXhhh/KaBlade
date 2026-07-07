package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;

/**
 * 「寒霜灵刃」的冰晶飞剑。
 * <p>
 * 复用幻影剑的目标同步和追踪飞行，在命中后切换成短暂的晶化爆闪阶段，
 * 因而飞行长尾和命中晶簇能由同一个网络实体连续呈现。
 */
public class FrostBladeEntity extends PhantomSwordExEntity {

    private static final EntityDataAccessor<Boolean> DATA_IMPACTING =
            SynchedEntityData.defineId(FrostBladeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_IMPACT_TICK =
            SynchedEntityData.defineId(FrostBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FINISHER =
            SynchedEntityData.defineId(FrostBladeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int FROST_SLOW_DURATION = 60;
    private static final int FROST_SLOW_AMPLIFIER = 2;
    private static final int NORMAL_IMPACT_LIFETIME = 8;
    private static final int FINISHER_IMPACT_LIFETIME = 11;
    private static final float FLIGHT_SPEED = 1.75F;

    private static final Vector3f ICE_BLUE = new Vector3f(0.08F, 0.78F, 1.0F);
    private static final Vector3f ICE_WHITE = new Vector3f(0.88F, 0.99F, 1.0F);

    public FrostBladeEntity(EntityType<? extends FrostBladeEntity> type, Level level) {
        super(type, level);
    }

    public static FrostBladeEntity spawn(Level level, LivingEntity thrower, LivingEntity target,
                                         Vec3 pos, Vec3 fallbackDirection, float damage,
                                         int color, boolean finisher) {
        FrostBladeEntity blade = new FrostBladeEntity(ModEntities.FROST_BLADE_EDGE.get(), level);
        Vec3 direction = target == null
                ? fallbackDirection.normalize()
                : target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D)
                        .subtract(pos).normalize();
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }

        // PhantomSwordExEntity#setDriveVectorFromIni 使用 -sin(yaw) / -sin(pitch)，
        // 因此这里必须取反 X/Y；否则初始速度会沿水平轴和垂直轴镜像。
        float yaw = (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
        float horizontal = Mth.sqrt((float) (direction.x * direction.x + direction.z * direction.z));
        float pitch = (float) (Mth.atan2(-direction.y, horizontal) * Mth.RAD_TO_DEG);

        blade.setPos(pos.x, pos.y, pos.z);
        blade.setYRot(yaw);
        blade.setXRot(pitch);
        blade.setThrower(thrower);
        blade.attackDamage = damage;
        blade.setColor(color);
        blade.setLifetime(36);
        blade.setInterval(target == null ? 0 : 2);
        blade.setIniYaw(yaw);
        blade.setIniPitch(pitch);
        blade.setRoll(0.0F);
        blade.setFinisher(finisher);
        blade.initializeTrajectory(yaw, pitch,
                target == null ? direction.scale(FLIGHT_SPEED) : Vec3.ZERO);
        blade.blade = thrower.getMainHandItem().copy();
        blade.alreadyHit.add(thrower.getUUID());
        if (target != null) {
            blade.setTarget(target);
        }
        level.addFreshEntity(blade);
        return blade;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IMPACTING, false);
        this.entityData.define(DATA_IMPACT_TICK, 0);
        this.entityData.define(DATA_FINISHER, false);
    }

    public boolean isImpacting() {
        return this.entityData.get(DATA_IMPACTING);
    }

    public int getImpactTick() {
        return this.entityData.get(DATA_IMPACT_TICK);
    }

    public boolean isFinisher() {
        return this.entityData.get(DATA_FINISHER);
    }

    public void setFinisher(boolean finisher) {
        this.entityData.set(DATA_FINISHER, finisher);
    }

    @Override
    protected void tickFlying() {
        if (isImpacting()) {
            return;
        }

        super.tickFlying();

        if (level().isClientSide() && !isRemoved() && !isImpacting()) {
            Vec3 motion = getDeltaMovement();
            if (motion.lengthSqr() > 0.01D) {
                Vec3 tail = motion.normalize().scale(-0.38D);
                level().addParticle(ParticleTypes.SNOWFLAKE,
                        getX() + tail.x, getY() + 0.08D + tail.y, getZ() + tail.z,
                        tail.x * 0.06D, 0.002D, tail.z * 0.06D);
                if ((tickCount & 1) == 0) {
                    level().addParticle(ParticleTypes.END_ROD,
                            getX() + tail.x * 1.5D, getY() + 0.08D + tail.y * 1.5D,
                            getZ() + tail.z * 1.5D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    @Override
    protected Optional<LivingEntity> findTarget() {
        int targetId = getTargetEntityId();
        AABB hitBox = getBoundingBox().inflate(isFinisher() ? 1.25D : 0.95D);

        if (targetId != 0) {
            Entity target = level().getEntity(targetId);
            if (target instanceof LivingEntity living
                    && SaTargeting.canDamage(thrower, living)
                    && hitBox.intersects(living.getBoundingBox())) {
                return Optional.of(living);
            }
            return Optional.empty();
        }

        return level().getEntitiesOfClass(LivingEntity.class, hitBox,
                        e -> SaTargeting.canDamage(thrower, e)
                                && !alreadyHit.contains(e.getUUID()))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr));
    }

    @Override
    protected void onHitEntity(LivingEntity target) {
        if (isImpacting()) {
            return;
        }

        alreadyHit.add(target.getUUID());
        target.invulnerableTime = 0;
        com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, damageSource(), attackDamage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                FROST_SLOW_DURATION, FROST_SLOW_AMPLIFIER));
        target.setDeltaMovement(target.getDeltaMovement().scale(0.35D));
        target.hurtMarked = true;
        hitBlade(target);

        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.52D, target.getZ());
        setDeltaMovement(Vec3.ZERO);
        setTargetEntityId(0);
        this.entityData.set(DATA_IMPACTING, true);
        this.entityData.set(DATA_IMPACT_TICK, tickCount);
        setLifetime(tickCount + (isFinisher() ? FINISHER_IMPACT_LIFETIME : NORMAL_IMPACT_LIFETIME));

        if (level() instanceof ServerLevel server) {
            double spread = isFinisher() ? 0.72D : 0.48D;
            int snowCount = isFinisher() ? 28 : 16;
            server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                    snowCount, spread, spread, spread, 0.12D);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                    isFinisher() ? 16 : 8, spread * 0.6D, spread * 0.6D, spread * 0.6D, 0.08D);
            server.sendParticles(new DustColorTransitionOptions(ICE_BLUE, ICE_WHITE,
                            isFinisher() ? 1.7F : 1.25F),
                    getX(), getY(), getZ(), isFinisher() ? 22 : 12,
                    spread, spread, spread, 0.04D);
            server.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            server.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                    isFinisher() ? 1.0F : 0.62F, isFinisher() ? 0.88F : 1.32F);
            server.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    isFinisher() ? 0.9F : 0.45F, isFinisher() ? 0.72F : 1.55F);
        }
    }
}

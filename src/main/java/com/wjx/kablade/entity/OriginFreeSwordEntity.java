package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 源能自由剑：1.12.2 {@code EntitySummonSwordFree} 的轻量复刻。
 * 延迟后高速压入目标区域，命中时造成魔法伤害，结束时爆成一圈小范围源能冲击。
 */
public class OriginFreeSwordEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(OriginFreeSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DELAY =
            SynchedEntityData.defineId(OriginFreeSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(OriginFreeSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(OriginFreeSwordEntity.class, EntityDataSerializers.FLOAT);

    private LivingEntity owner;
    private float damage = 1.0F;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public OriginFreeSwordEntity(EntityType<? extends OriginFreeSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static OriginFreeSwordEntity spawn(Level level, LivingEntity owner, double x, double y, double z,
                                             Vec3 direction, float damage, int color,
                                             int delay, int lifetime, float roll) {
        OriginFreeSwordEntity sword = new OriginFreeSwordEntity(ModEntities.ORIGIN_FREE_SWORD.get(), level);
        Vec3 motion = direction.lengthSqr() < 1.0e-6
                ? new Vec3(0.0, -1.0, 0.0)
                : direction.normalize().scale(1.28);
        sword.setPos(x, y, z);
        sword.setDeltaMovement(motion);
        sword.updateRotationFromMotion(motion);
        sword.owner = owner;
        sword.damage = damage;
        sword.setColor(color);
        sword.setDelay(delay);
        sword.setLifetime(lifetime);
        sword.setRoll(roll);
        sword.alreadyHit.add(owner.getUUID());
        level.addFreshEntity(sword);
        return sword;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, 24);
        this.entityData.define(DATA_DELAY, 6);
        this.entityData.define(DATA_COLOR, 0x00FFFF);
        this.entityData.define(DATA_ROLL, 0.0F);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int value) {
        this.entityData.set(DATA_LIFETIME, value);
    }

    public int getDelay() {
        return this.entityData.get(DATA_DELAY);
    }

    public void setDelay(int value) {
        this.entityData.set(DATA_DELAY, value);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int value) {
        this.entityData.set(DATA_COLOR, value);
    }

    public float getRoll() {
        return this.entityData.get(DATA_ROLL);
    }

    public void setRoll(float value) {
        this.entityData.set(DATA_ROLL, value);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= this.getLifetime()) {
            if (!this.level().isClientSide()) {
                burst();
            } else {
                this.discard();
            }
            return;
        }

        if (this.tickCount <= this.getDelay()) {
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        updateRotationFromMotion(motion);

        Vec3 from = this.position();
        Vec3 to = from.add(motion);
        if (!this.level().isClientSide()) {
            findHit(from, to).ifPresent(this::hitTarget);
            if (!this.isAlive()) {
                return;
            }
        }
        this.setPos(to.x, to.y, to.z);
        this.setDeltaMovement(motion.scale(1.055));
    }

    private Optional<LivingEntity> findHit(Vec3 from, Vec3 to) {
        AABB box = new AABB(
                Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z),
                Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z)).inflate(0.75);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
            AABB targetBox = target.getBoundingBox().inflate(0.35);
            Optional<Vec3> clip = targetBox.clip(from, to);
            double dist = clip.map(from::distanceTo).orElseGet(() -> targetBox.contains(from) ? 0.0 : Double.MAX_VALUE);
            if (dist < closestDist) {
                closest = target;
                closestDist = dist;
            }
        }
        return Optional.ofNullable(closest);
    }

    private boolean canHit(LivingEntity target) {
        return target.isAlive()
                && (this.owner == null || (target != this.owner && !target.isAlliedTo(this.owner)))
                && !this.alreadyHit.contains(target.getUUID());
    }

    private void hitTarget(LivingEntity target) {
        this.alreadyHit.add(target.getUUID());
        target.invulnerableTime = 0;
        Entity source = this.owner == null ? this : this.owner;
        target.hurt(this.level().damageSources().indirectMagic(this, source), this.damage);
        target.setDeltaMovement(0.0, 0.1, 0.0);
        target.hurtMarked = true;
        burst();
    }

    private void burst() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                    18, 0.35, 0.35, 0.35, 0.04);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(),
                    12, 0.45, 0.45, 0.45, 0.08);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.35F, 1.55F);

        if (!this.level().isClientSide()) {
            AABB box = this.getBoundingBox().inflate(1.15);
            Entity source = this.owner == null ? this : this.owner;
            for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
                target.invulnerableTime = 0;
                target.hurt(this.level().damageSources().indirectMagic(this, source), 1.0F);
            }
        }
        this.discard();
    }

    private void updateRotationFromMotion(Vec3 motion) {
        if (motion.lengthSqr() < 1.0e-6) {
            return;
        }
        double horizontal = motion.horizontalDistance();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG));
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

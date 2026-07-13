package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.List;

/** Visual and delayed-damage anchor for Frozen Naraka's Utpala Aura slash art. */
public class UtpalaAuraEntity extends Entity {

    public static final int LIFETIME = 78;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(UtpalaAuraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(UtpalaAuraEntity.class, EntityDataSerializers.INT);

    private static final Vector3f BLUE = new Vector3f(0.08F, 0.66F, 1.0F);
    private static final Vector3f WHITE = new Vector3f(0.92F, 1.0F, 1.0F);

    private static final double VORTEX_RADIUS = 5.2D;
    private static final double VORTEX_HEIGHT = 2.8D;
    private static final double ICE_RANGE = 7.6D;
    private static final double ICE_HALF_WIDTH = 2.8D;
    private static final double BLADE_RANGE = 12.5D;
    private static final double BLADE_HALF_WIDTH = 3.7D;

    private LivingEntity owner;
    private float baseDamage;

    public UtpalaAuraEntity(EntityType<? extends UtpalaAuraEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static UtpalaAuraEntity spawn(ServerLevel level, LivingEntity owner, float baseDamage) {
        UtpalaAuraEntity entity = new UtpalaAuraEntity(ModEntities.UTPALA_AURA.get(), level);
        entity.owner = owner;
        entity.baseDamage = baseDamage;
        entity.setOwnerId(owner.getId());
        entity.setLifetime(LIFETIME);
        entity.setPos(owner.getX(), owner.getY(), owner.getZ());
        entity.setYRot(owner.getYRot());
        entity.yRotO = owner.yRotO;
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_ID, 0);
        this.entityData.define(DATA_LIFETIME, LIFETIME);
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    private void setOwnerId(int ownerId) {
        this.entityData.set(DATA_OWNER_ID, ownerId);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    private void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        LivingEntity source = resolveOwner();
        if (source == null || !source.isAlive()) {
            this.discard();
            return;
        }

        if (this.tickCount <= 34) {
            followOwner(source);
        }

        ServerLevel level = (ServerLevel) this.level();
        playTimelineSounds(level);
        spawnTimelineParticles(level);
        applyTimelineHits(level, source);

        if (this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    private LivingEntity resolveOwner() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }

        Entity entity = this.level().getEntity(this.getOwnerId());
        if (entity instanceof LivingEntity living) {
            this.owner = living;
            return living;
        }
        return null;
    }

    private void followOwner(LivingEntity source) {
        this.setPos(source.getX(), source.getY(), source.getZ());
        this.yRotO = this.getYRot();
        this.setYRot(source.getYRot());
    }

    private void playTimelineSounds(ServerLevel level) {
        if (this.tickCount == 6) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.78F, 1.66F);
        } else if (this.tickCount == 18) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 1.48F);
        } else if (this.tickCount == 30) {
            level.playSound(null, this.getX(), this.getY() + 0.8D, this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.08F, 0.82F);
            level.playSound(null, this.getX(), this.getY() + 0.8D, this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 0.86F);
        } else if (this.tickCount == 38) {
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.08F, 0.76F);
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 0.82F, 1.86F);
        }
    }

    private void spawnTimelineParticles(ServerLevel level) {
        if (this.tickCount >= 4 && this.tickCount <= 34) {
            float grow = smootherStep(Mth.clamp((this.tickCount - 4.0F) / 20.0F, 0.0F, 1.0F));
            int count = 8 + Mth.floor(grow * 24.0F);
            for (int i = 0; i < count; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D + this.tickCount * 0.34D;
                double radius = 0.55D + grow * 4.25D + level.random.nextDouble() * (0.28D + grow * 0.32D);
                double y = 0.14D + level.random.nextDouble() * (0.34D + grow * 2.15D);
                double sx = Math.cos(angle) * radius;
                double sz = Math.sin(angle) * radius;
                Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle)).scale(0.09D + grow * 0.10D);
                level.sendParticles(new DustColorTransitionOptions(BLUE, WHITE, 1.25F + grow * 0.65F),
                        this.getX() + sx, this.getY() + y, this.getZ() + sz,
                        1, tangent.x, 0.012D, tangent.z, 0.035D);
            }
        }

        if (this.tickCount >= 24 && this.tickCount <= 70 && (this.tickCount & 1) == 0) {
            Vec3 forward = flatForward();
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            for (int i = 0; i < 3; i++) {
                double ahead = 1.0D + level.random.nextDouble() * 9.0D;
                double side = (level.random.nextDouble() - 0.5D) * 5.8D;
                Vec3 pos = this.position().add(forward.scale(ahead)).add(right.scale(side))
                        .add(0.0D, 0.45D + level.random.nextDouble() * 2.2D, 0.0D);
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z,
                        1, 0.02D, 0.04D, 0.02D, 0.025D);
            }
        }

        if (this.tickCount >= 27 && this.tickCount <= 31) {
            Vec3 forward = flatForward();
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            int step = this.tickCount - 27;
            for (int i = 0; i < 3; i++) {
                double ahead = 1.15D + step * 1.15D + i * 0.38D;
                double side = (i - 1) * (0.52D + step * 0.05D);
                Vec3 pos = this.position().add(forward.scale(ahead)).add(right.scale(side))
                        .add(0.0D, 0.12D, 0.0D);
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z,
                        10, 0.28D, 0.10D, 0.28D, 0.12D);
                level.sendParticles(new DustColorTransitionOptions(BLUE, WHITE, 1.35F),
                        pos.x, pos.y + 0.08D, pos.z, 7, 0.20D, 0.10D, 0.20D, 0.07D);
            }
        }

        if (this.tickCount == 30) {
            Vec3 burst = this.position().add(flatForward().scale(4.6D)).add(0.0D, 0.6D, 0.0D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, burst.x, burst.y, burst.z,
                    80, 1.9D, 1.1D, 1.9D, 0.18D);
            level.sendParticles(new DustColorTransitionOptions(BLUE, WHITE, 1.85F),
                    burst.x, burst.y, burst.z, 46, 1.45D, 0.85D, 1.45D, 0.10D);
            level.sendParticles(ParticleTypes.FLASH, burst.x, burst.y + 0.25D, burst.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        if (this.tickCount == 38 || this.tickCount == 42) {
            Vec3 origin = this.position().add(0.0D, 1.1D, 0.0D);
            Vec3 forward = flatForward();
            for (int i = 0; i < 9; i++) {
                Vec3 pos = origin.add(forward.scale(1.3D + i * 1.15D));
                level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                        6, 0.42D + i * 0.04D, 0.18D, 0.42D + i * 0.04D, 0.08D);
            }
        }
    }

    private void applyTimelineHits(ServerLevel level, LivingEntity source) {
        if (this.tickCount == 10 || this.tickCount == 16 || this.tickCount == 22 || this.tickCount == 28) {
            vortexPulse(level, source);
        } else if (this.tickCount == 30) {
            forwardHit(level, source, ICE_RANGE, ICE_HALF_WIDTH, this.baseDamage * 0.74F, 0.36D, 82, 1);
        } else if (this.tickCount == 38) {
            forwardHit(level, source, BLADE_RANGE, BLADE_HALF_WIDTH, this.baseDamage * 1.06F, 0.62D, 96, 1);
        } else if (this.tickCount == 42) {
            forwardHit(level, source, BLADE_RANGE, BLADE_HALF_WIDTH + 0.7D,
                    this.baseDamage * 0.42F, 0.34D, 70, 0);
        }
    }

    private void vortexPulse(ServerLevel level, LivingEntity source) {
        AABB area = new AABB(
                this.getX() - VORTEX_RADIUS, this.getY() - 0.2D, this.getZ() - VORTEX_RADIUS,
                this.getX() + VORTEX_RADIUS, this.getY() + VORTEX_HEIGHT, this.getZ() + VORTEX_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> SaTargeting.canDamage(source, target));
        DamageSource damageSource = level.damageSources().indirectMagic(this, source);

        for (LivingEntity target : targets) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > VORTEX_RADIUS) {
                continue;
            }

            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, this, source, this.baseDamage * 0.16F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 2));
            Vec3 pull = new Vec3(this.getX() - target.getX(), 0.0D, this.getZ() - target.getZ());
            if (pull.lengthSqr() > 1.0E-6D) {
                pull = pull.normalize().scale(0.14D);
            }
            target.setDeltaMovement(target.getDeltaMovement().scale(0.58D)
                    .add(pull.x, 0.055D, pull.z));
            target.hurtMarked = true;
        }
    }

    private void forwardHit(ServerLevel level, LivingEntity source, double range, double halfWidth,
                            float damage, double knockback, int freezeTicks, int freezeAmplifier) {
        Vec3 forward = flatForward();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 origin = this.position().add(0.0D, 1.0D, 0.0D);
        AABB area = new AABB(
                this.getX() - range, this.getY() - 1.0D, this.getZ() - range,
                this.getX() + range, this.getY() + 4.0D, this.getZ() + range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> SaTargeting.canDamage(source, target));
        DamageSource damageSource = level.damageSources().indirectMagic(this, source);

        for (LivingEntity target : targets) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
            Vec3 rel = center.subtract(origin);
            double ahead = rel.dot(forward);
            double lateral = Math.abs(rel.dot(right));
            double vertical = Math.abs(rel.y);
            if (ahead < 0.25D || ahead > range || lateral > halfWidth || vertical > 3.0D) {
                continue;
            }

            boolean hurt = com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, this, source, damage);
            if (hurt) {
                spawnForwardHitFeedback(level, target, damage >= this.baseDamage);
            }
            target.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(),
                    freezeTicks, freezeAmplifier));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    freezeTicks, Math.max(1, freezeAmplifier + 2)));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(),
                    target.getTicksRequiredToFreeze() + freezeTicks));
            target.setDeltaMovement(target.getDeltaMovement().scale(0.35D)
                    .add(forward.scale(knockback)).add(0.0D, 0.10D, 0.0D));
            target.hurtMarked = true;
        }
    }

    private void spawnForwardHitFeedback(ServerLevel level, LivingEntity target, boolean mainBlade) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        int snow = mainBlade ? 34 : 18;
        int crit = mainBlade ? 16 : 8;
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
                snow, 0.42D, 0.35D, 0.42D, mainBlade ? 0.18D : 0.10D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                crit, 0.34D, 0.28D, 0.34D, mainBlade ? 0.24D : 0.14D);
        level.sendParticles(new DustColorTransitionOptions(BLUE, WHITE, mainBlade ? 1.65F : 1.25F),
                center.x, center.y, center.z,
                mainBlade ? 22 : 12, 0.36D, 0.28D, 0.36D, mainBlade ? 0.13D : 0.08D);
        if (mainBlade) {
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.72F, 1.22F);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.48F, 1.42F);
        }
    }

    private Vec3 flatForward() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).normalize();
    }

    private static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerId")) {
            this.setOwnerId(tag.getInt("OwnerId"));
        }
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("BaseDamage")) {
            this.baseDamage = tag.getFloat("BaseDamage");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", this.getOwnerId());
        tag.putInt("Lifetime", this.getLifetime());
        tag.putFloat("BaseDamage", this.baseDamage);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double radius = 28.0D;
        return new AABB(
                this.getX() - radius, this.getY() - 1.0D, this.getZ() - radius,
                this.getX() + radius, this.getY() + 5.0D, this.getZ() + radius);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.util.SaDamage;
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

/** Visual and multi-hit anchor for Key of Limpidity's Sword Enlightenment SA. */
public class SwordEnlightenmentEntity extends Entity {

    public static final int LIFETIME = 62;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(SwordEnlightenmentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(SwordEnlightenmentEntity.class, EntityDataSerializers.INT);

    private static final Vector3f VIOLET = new Vector3f(0.55F, 0.20F, 1.0F);
    private static final Vector3f PALE = new Vector3f(1.0F, 0.86F, 1.0F);

    private static final double HIT_RADIUS = 8.2D;
    private static final double HIT_HEIGHT = 4.8D;
    private static final int PARALYSIS_DURATION = 80;
    private static final int PARALYSIS_AMPLIFIER = 2;

    private LivingEntity owner;
    private float baseDamage;

    public SwordEnlightenmentEntity(EntityType<? extends SwordEnlightenmentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static SwordEnlightenmentEntity spawn(ServerLevel level, LivingEntity owner, float baseDamage) {
        return spawn(level, owner, baseDamage, owner.position());
    }

    public static SwordEnlightenmentEntity spawn(ServerLevel level, LivingEntity owner,
                                                 float baseDamage, Vec3 attackPosition) {
        SwordEnlightenmentEntity entity = new SwordEnlightenmentEntity(ModEntities.SWORD_ENLIGHTENMENT.get(), level);
        entity.initialize(owner, baseDamage, attackPosition);
        level.addFreshEntity(entity);
        return entity;
    }

    /** Initializes a Sword Enlightenment-style entity while allowing named variants to use their own entity type. */
    protected final void initialize(LivingEntity owner, float baseDamage, Vec3 attackPosition) {
        this.owner = owner;
        this.baseDamage = baseDamage;
        this.setOwnerId(owner.getId());
        this.setLifetime(LIFETIME);
        this.setPos(attackPosition.x, attackPosition.y, attackPosition.z);
        this.setYRot(owner.getYRot());
        this.yRotO = owner.yRotO;
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

        ServerLevel level = (ServerLevel) this.level();
        playTimelineSounds(level);
        spawnTimelineParticles(level);
        applyTimelineHits(level, source);
        tickVariant(level, source);

        if (this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    /** Server-side extension point for named variants that add their own timeline beats. */
    protected void tickVariant(ServerLevel level, LivingEntity source) {
    }

    protected final float getBaseDamage() {
        return this.baseDamage;
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

    private void playTimelineSounds(ServerLevel level) {
        if (this.tickCount == 4) {
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.86F, 1.62F);
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.58F, 1.94F);
        } else if (this.tickCount == 8) {
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.02F, 1.74F);
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.82F, 1.35F);
        } else if (this.tickCount == 16 || this.tickCount == 24) {
            level.playSound(null, this.getX(), this.getY() + 1.1D, this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.82F, 1.18F);
            level.playSound(null, this.getX(), this.getY() + 1.1D, this.getZ(),
                    SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.70F, 1.62F);
        } else if (this.tickCount == 32) {
            level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.94F, 1.20F);
            level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 0.86F, 1.88F);
        }
    }

    private void spawnTimelineParticles(ServerLevel level) {
        if (this.tickCount >= 6 && this.tickCount <= 38) {
            float open = smootherStep(Mth.clamp((this.tickCount - 6.0F) / 18.0F, 0.0F, 1.0F));
            int count = 8 + Mth.floor(open * 22.0F);
            for (int i = 0; i < count; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D + this.tickCount * 0.36D;
                double radius = 0.62D + open * 4.85D + level.random.nextDouble() * 0.48D;
                double y = 0.10D + level.random.nextDouble() * (0.55D + open * 2.20D);
                double sx = Math.cos(angle) * radius;
                double sz = Math.sin(angle) * radius;
                Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle))
                        .scale(0.055D + open * 0.08D);
                level.sendParticles(new DustColorTransitionOptions(VIOLET, PALE, 1.05F + open * 0.75F),
                        this.getX() + sx, this.getY() + y, this.getZ() + sz,
                        1, tangent.x, 0.018D, tangent.z, 0.035D);
            }
        }

        if (this.tickCount >= 8 && this.tickCount <= 34 && (this.tickCount & 1) == 0) {
            Vec3 forward = flatForward();
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            for (int i = 0; i < 4; i++) {
                double ahead = 0.8D + level.random.nextDouble() * 5.4D;
                double side = (level.random.nextDouble() - 0.5D) * 5.8D;
                double y = 0.45D + level.random.nextDouble() * 2.35D;
                Vec3 pos = this.position().add(forward.scale(ahead)).add(right.scale(side)).add(0.0D, y, 0.0D);
                Vec3 sweep = forward.scale(0.04D + level.random.nextDouble() * 0.09D)
                        .add(right.scale((level.random.nextDouble() - 0.5D) * 0.10D));
                level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                        1, sweep.x, 0.06D, sweep.z, 0.035D);
            }
        }

        if (this.tickCount == 8 || this.tickCount == 12 || this.tickCount == 16
                || this.tickCount == 20 || this.tickCount == 24 || this.tickCount == 28
                || this.tickCount == 32) {
            Vec3 center = this.position().add(flatForward().scale(2.35D)).add(0.0D, 1.25D, 0.0D);
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    this.tickCount == 32 ? 34 : 18, 0.78D, 0.58D, 0.78D, 0.16D);
            level.sendParticles(new DustColorTransitionOptions(PALE, VIOLET, 1.72F),
                    center.x, center.y, center.z,
                    this.tickCount == 32 ? 42 : 24, 0.70D, 0.48D, 0.70D, 0.10D);
        }
    }

    private void applyTimelineHits(ServerLevel level, LivingEntity source) {
        if (this.tickCount < 8 || this.tickCount > 32 || ((this.tickCount - 8) & 3) != 0) {
            return;
        }

        float pulseDamage = this.baseDamage * (this.tickCount == 8 ? 0.38F : this.tickCount == 32 ? 1.08F : 0.56F);
        boolean finisher = this.tickCount == 32;
        double lift = finisher ? 0.32D : this.tickCount >= 20 ? 0.22D : 0.13D;
        slashPulse(level, source, pulseDamage, lift, finisher);
    }

    private void slashPulse(ServerLevel level, LivingEntity source, float damage, double lift, boolean finisher) {
        AABB area = new AABB(
                this.getX() - HIT_RADIUS, this.getY() - 0.65D, this.getZ() - HIT_RADIUS,
                this.getX() + HIT_RADIUS, this.getY() + HIT_HEIGHT, this.getZ() + HIT_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target != source && target.isPickable() && SaTargeting.canDamage(source, target));
        DamageSource damageSource = level.damageSources().indirectMagic(this, source);
        Vec3 forward = flatForward();
        Vec3 pullCenter = this.position().add(forward.scale(2.15D)).add(0.0D, 1.1D, 0.0D);

        for (LivingEntity target : targets) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
            Vec3 rel = center.subtract(this.position().add(0.0D, 1.0D, 0.0D));
            double horizontal = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            double vertical = Math.abs(rel.y);
            if (horizontal > HIT_RADIUS || vertical > HIT_HEIGHT) {
                continue;
            }

            float falloff = (float) Mth.clamp(1.0D - horizontal / (HIT_RADIUS * 1.35D), 0.62D, 1.0D);
            if (SaDamage.hurtSlashArtNoIFrame(target, level, this, source, damage * falloff)) {
                spawnHitFeedback(level, target, finisher);
                target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                        PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
            }

            Vec3 motion;
            if (finisher) {
                motion = forward.scale(0.52D).add(0.0D, lift, 0.0D);
            } else {
                Vec3 pull = pullCenter.subtract(center);
                if (pull.lengthSqr() > 1.0E-6D) {
                    pull = pull.normalize().scale(0.13D);
                }
                motion = pull.add(0.0D, lift, 0.0D);
            }
            target.setDeltaMovement(target.getDeltaMovement().scale(finisher ? 0.36D : 0.52D).add(motion));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    finisher ? 38 : 26, finisher ? 3 : 2, false, false));
            target.hurtMarked = true;
        }
    }

    private void spawnHitFeedback(ServerLevel level, LivingEntity target, boolean finisher) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                finisher ? 18 : 9, 0.32D, 0.26D, 0.32D, finisher ? 0.22D : 0.12D);
        level.sendParticles(new DustColorTransitionOptions(VIOLET, PALE, finisher ? 1.55F : 1.18F),
                center.x, center.y, center.z,
                finisher ? 24 : 12, 0.32D, 0.28D, 0.32D, finisher ? 0.12D : 0.07D);
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
        double radius = 18.0D;
        return new AABB(
                this.getX() - radius, this.getY() - 1.0D, this.getZ() - radius,
                this.getX() + radius, this.getY() + 6.0D, this.getZ() + radius);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

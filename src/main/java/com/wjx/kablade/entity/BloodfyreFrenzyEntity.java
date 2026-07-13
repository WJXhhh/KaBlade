package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Timeline, ownership and damage anchor for Bloodfyre Frenzy. */
public final class BloodfyreFrenzyEntity extends Entity {

    public static final int LIFETIME = 52;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(BloodfyreFrenzyEntity.class, EntityDataSerializers.INT);

    private static final double SCAN_RADIUS = 13.0D;
    private static final double VERTICAL_BELOW = 1.25D;
    private static final double VERTICAL_ABOVE = 4.5D;

    private final Set<UUID> burningTargets = new HashSet<>();
    private LivingEntity owner;
    private float baseDamage;

    public BloodfyreFrenzyEntity(EntityType<? extends BloodfyreFrenzyEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static BloodfyreFrenzyEntity spawn(ServerLevel level, LivingEntity owner, float baseDamage) {
        BloodfyreFrenzyEntity entity = new BloodfyreFrenzyEntity(ModEntities.BLOODFYRE_FRENZY.get(), level);
        entity.owner = owner;
        entity.baseDamage = baseDamage;
        entity.setOwnerId(owner.getId());
        entity.setPos(owner.getX(), owner.getY(), owner.getZ());
        entity.setYRot(owner.getYRot());
        entity.yRotO = owner.yRotO;
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_ID, 0);
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    private void setOwnerId(int ownerId) {
        this.entityData.set(DATA_OWNER_ID, ownerId);
    }

    public float getRenderAge(float partialTick) {
        return Mth.clamp(this.tickCount + partialTick, 0.0F, LIFETIME);
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

        // Keep the wind-up wrapped around the caster, then freeze the origin for the finisher and scar.
        if (this.tickCount <= 8) {
            this.setPos(source.getX(), source.getY(), source.getZ());
        }

        ServerLevel level = (ServerLevel) this.level();
        playTimelineSounds(level);
        applyTimelineDamage(level, source);
        if (this.tickCount >= LIFETIME) {
            this.discard();
        }
    }

    private LivingEntity resolveOwner() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }
        Entity resolved = this.level().getEntity(this.getOwnerId());
        if (resolved instanceof LivingEntity living) {
            this.owner = living;
            return living;
        }
        return null;
    }

    private void playTimelineSounds(ServerLevel level) {
        if (this.tickCount == 4 || this.tickCount == 7) {
            float pitch = this.tickCount == 4 ? 1.18F : 0.92F;
            level.playSound(null, this.getX(), this.getY() + 1.1D, this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, pitch);
            level.playSound(null, this.getX(), this.getY() + 1.1D, this.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.62F, pitch + 0.32F);
        } else if (this.tickCount == 9) {
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.18F, 0.68F);
            level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.72F);
        } else if (this.tickCount == 11) {
            Vec3 center = this.position().add(flatForward().scale(5.0D));
            level.playSound(null, center.x, center.y + 1.0D, center.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.55F, 0.72F);
            level.playSound(null, center.x, center.y + 1.0D, center.z,
                    SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.88F, 1.42F);
        } else if (this.tickCount == 40) {
            level.playSound(null, this.getX(), this.getY() + 0.8D, this.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.34F, 0.58F);
        }
    }

    private void applyTimelineDamage(ServerLevel level, LivingEntity source) {
        switch (this.tickCount) {
            case 4 -> hitSpin(level, source, 3.2D, this.baseDamage * 0.12F, 0.08D);
            case 7 -> hitSpin(level, source, 4.4D, this.baseDamage * 0.14F, 0.13D);
            case 9 -> hitForwardWave(level, source, this.baseDamage * 0.20F, false);
            case 11 -> hitForwardWave(level, source, this.baseDamage * 0.38F, true);
            case 15, 19, 23 -> hitBurningTargets(level, source, this.baseDamage * 0.053F);
            default -> {
            }
        }
    }

    private void hitSpin(ServerLevel level, LivingEntity source, double radius, float damage, double lift) {
        Vec3 origin = this.position().add(0.0D, 1.0D, 0.0D);
        DamageSource damageSource = level.damageSources().indirectMagic(this, source);
        for (LivingEntity target : nearbyTargets(level, source)) {
            Vec3 center = targetCenter(target);
            Vec3 offset = center.subtract(origin);
            if (Math.abs(offset.y) > 2.7D || offset.horizontalDistanceSqr() > radius * radius) {
                continue;
            }
            if (SaDamage.hurtSlashArtNoIFrame(target, level, this, source, damage)) {
                Vec3 pull = origin.subtract(center);
                pull = pull.horizontalDistanceSqr() > 1.0E-5D
                        ? new Vec3(pull.x, 0.0D, pull.z).normalize().scale(0.10D)
                        : Vec3.ZERO;
                target.setDeltaMovement(target.getDeltaMovement().scale(0.60D).add(pull).add(0.0D, lift, 0.0D));
                target.hurtMarked = true;
            }
        }
    }

    private void hitForwardWave(ServerLevel level, LivingEntity source, float damage, boolean finisher) {
        Vec3 origin = this.position().add(0.0D, 1.0D, 0.0D);
        Vec3 forward = flatForward();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        DamageSource damageSource = level.damageSources().indirectMagic(this, source);

        for (LivingEntity target : nearbyTargets(level, source)) {
            Vec3 center = targetCenter(target);
            Vec3 offset = center.subtract(origin);
            double ahead = offset.dot(forward);
            double side = Math.abs(offset.dot(right));
            double vertical = offset.y;
            double width = finisher ? 2.6D + Math.max(0.0D, ahead) * 0.48D
                    : 1.35D + Math.max(0.0D, ahead) * 0.32D;
            double reach = finisher ? 11.5D : 9.0D;
            if (ahead < -0.8D || ahead > reach || side > width
                    || vertical < -VERTICAL_BELOW || vertical > VERTICAL_ABOVE) {
                continue;
            }

            float falloff = (float) Mth.clamp(1.0D - Math.max(0.0D, ahead - 2.0D) / 22.0D,
                    0.62D, 1.0D);
            if (!SaDamage.hurtSlashArtNoIFrame(target, level, this, source, damage * falloff)) {
                continue;
            }

            this.burningTargets.add(target.getUUID());
            double push = finisher ? 0.72D : 0.28D;
            double lift = finisher ? 0.24D : 0.12D;
            target.setDeltaMovement(target.getDeltaMovement().scale(finisher ? 0.38D : 0.55D)
                    .add(forward.scale(push)).add(0.0D, lift, 0.0D));
            target.hurtMarked = true;
        }
    }

    private void hitBurningTargets(ServerLevel level, LivingEntity source, float damage) {
        DamageSource damageSource = level.damageSources().indirectMagic(this, source);
        this.burningTargets.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()
                    || !SaTargeting.canDamage(source, target)
                    || target.distanceToSqr(this) > SCAN_RADIUS * SCAN_RADIUS * 2.0D) {
                return true;
            }
            SaDamage.hurtSlashArtNoIFrame(target, level, this, source, damage);
            return false;
        });
    }

    private List<LivingEntity> nearbyTargets(ServerLevel level, LivingEntity source) {
        AABB area = new AABB(
                this.getX() - SCAN_RADIUS, this.getY() - VERTICAL_BELOW, this.getZ() - SCAN_RADIUS,
                this.getX() + SCAN_RADIUS, this.getY() + VERTICAL_ABOVE, this.getZ() + SCAN_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isPickable() && SaTargeting.canDamageAttackable(source, target));
    }

    private static Vec3 targetCenter(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
    }

    private Vec3 flatForward() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).normalize();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerId")) {
            this.setOwnerId(tag.getInt("OwnerId"));
        }
        if (tag.contains("BaseDamage")) {
            this.baseDamage = tag.getFloat("BaseDamage");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", this.getOwnerId());
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
                this.getX() - radius, this.getY() - 2.0D, this.getZ() - radius,
                this.getX() + radius, this.getY() + 8.0D, this.getZ() + radius);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

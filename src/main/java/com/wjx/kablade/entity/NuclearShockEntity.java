package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Timeline and damage entity for the Nuclear Shock (核能震动) Slash Art.
 */
public class NuclearShockEntity extends Entity {

    public static final int MAX_LIFETIME = 68; // 2.24s (30fps)
    public static final int HIT_TICK = 26;     // 0.88s ground impact frame
    public static final double RADIUS = 6.8D;  // Shockwave radius

    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(NuclearShockEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;
    private float totalDamage;

    public NuclearShockEntity(EntityType<? extends NuclearShockEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static NuclearShockEntity spawn(ServerLevel level, LivingEntity owner, Vec3 pos, float damage) {
        NuclearShockEntity entity = new NuclearShockEntity(ModEntities.NUCLEAR_SHOCK.get(), level);
        entity.setPos(pos.x, pos.y, pos.z);
        entity.ownerUUID = owner.getUUID();
        entity.totalDamage = damage;
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFETIME, 0);
    }

    @Override
    public void tick() {
        super.tick();
        int age = this.entityData.get(LIFETIME) + 1;
        this.entityData.set(LIFETIME, age);

        if (age >= MAX_LIFETIME) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        // Trigger ground nuclear burst at frame 26
        if (!this.level().isClientSide() && age == HIT_TICK) {
            triggerNuclearBurst();
        }
    }

    private void triggerNuclearBurst() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = getOwnerLiving();
        if (owner == null) {
            return;
        }

        // Play double burst sounds
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.4F, 0.75F);
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F);

        // Scan targets in radial area
        AABB box = this.getBoundingBox().inflate(RADIUS, 3.5D, RADIUS);
        List<SaTarget> targets = SaTargeting.uniqueTargets(level, owner, box);
        for (SaTarget target : targets) {
            LivingEntity victim = target.root();
            SaDamage.hurtSlashArtNoIFrame(target.hitEntity(), level, this, owner, this.totalDamage);

            Vec3 delta = victim.position().subtract(this.position());
            Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
            Vec3 knockback = (horizontal.lengthSqr() > 1e-4 ? horizontal.normalize() : new Vec3(0.0, 0.0, 1.0)).scale(1.85D);
            victim.setDeltaMovement(knockback.x, 0.65D, knockback.z);
            victim.hurtMarked = true;
            victim.hasImpulse = true;
        }
    }

    @Nullable
    public LivingEntity getOwnerLiving() {
        if (this.ownerUUID == null || !(this.level() instanceof ServerLevel sl)) {
            return null;
        }
        Entity e = sl.getEntity(this.ownerUUID);
        return e instanceof LivingEntity le ? le : null;
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("Damage")) {
            this.totalDamage = tag.getFloat("Damage");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putFloat("Damage", this.totalDamage);
    }
}

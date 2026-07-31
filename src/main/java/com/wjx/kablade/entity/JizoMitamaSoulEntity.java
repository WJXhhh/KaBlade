package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative 66-tick timeline and visual anchor for Soul Appearance. */
public final class JizoMitamaSoulEntity extends Entity {

    public static final int LIFETIME = 66;
    public static final int ASCEND_TICK = 36;
    public static final int SLAM_TICK = 46;
    public static final int IMPACT_TICK = 58;

    private static final double ATTACK_REACH = 16.0D;
    private static final double ATTACK_BASE_HALF_WIDTH = 4.8D;
    private static final double ATTACK_DOWN_REACH = 3.0D;
    private static final double ATTACK_UP_REACH = 7.0D;
    private static final double IMPACT_DISTANCE = 10.0D;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(JizoMitamaSoulEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START_TIME =
            SynchedEntityData.defineId(JizoMitamaSoulEntity.class, EntityDataSerializers.LONG);
    private static final Set<UUID> ACTIVE_CASTERS = ConcurrentHashMap.newKeySet();
    private static final Vector3f SOUL_RED = new Vector3f(0.78F, 0.035F, 0.025F);

    private UUID ownerUuid;
    private LivingEntity owner;
    private float damage;
    private boolean impacted;
    private boolean releasedCaster;

    public JizoMitamaSoulEntity(EntityType<? extends JizoMitamaSoulEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static JizoMitamaSoulEntity spawn(ServerLevel level, LivingEntity owner, float damage) {
        if (!ACTIVE_CASTERS.add(owner.getUUID())) {
            return null;
        }

        Vec3 forward = flatForward(owner.getYRot());
        Vec3 origin = owner.position().subtract(forward.scale(1.25D));
        JizoMitamaSoulEntity entity = new JizoMitamaSoulEntity(ModEntities.JIZO_MITAMA_SOUL.get(), level);
        entity.owner = owner;
        entity.ownerUuid = owner.getUUID();
        entity.damage = Math.max(1.0F, damage);
        entity.setOwnerId(owner.getId());
        entity.setStartGameTime(level.getGameTime());
        entity.setPos(origin);
        entity.setYRot(owner.getYRot());
        entity.yRotO = owner.getYRot();
        if (!level.addFreshEntity(entity)) {
            ACTIVE_CASTERS.remove(owner.getUUID());
            return null;
        }
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_ID, -1);
        this.entityData.define(DATA_START_TIME, 0L);
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    private void setOwnerId(int ownerId) {
        this.entityData.set(DATA_OWNER_ID, ownerId);
    }

    private void setStartGameTime(long time) {
        this.entityData.set(DATA_START_TIME, time);
    }

    public float getRenderAge(float partialTick) {
        if (this.level() == null) {
            return Mth.clamp(this.tickCount + partialTick, 0.0F, LIFETIME);
        }
        return Mth.clamp((float) (this.level().getGameTime() + partialTick
                - this.entityData.get(DATA_START_TIME)), 0.0F, LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide()) {
            return;
        }

        LivingEntity caster = resolveOwner();
        if (caster == null || !caster.isAlive()) {
            this.discard();
            return;
        }

        ServerLevel level = (ServerLevel) this.level();
        playTimelineFx(level);
        if (!this.impacted && this.tickCount >= IMPACT_TICK) {
            this.impacted = true;
            resolveSlam(level, caster);
        }
        if (this.tickCount >= LIFETIME) {
            this.discard();
        }
    }

    private LivingEntity resolveOwner() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }
        Entity byId = this.level().getEntity(getOwnerId());
        if (byId instanceof LivingEntity living
                && (this.ownerUuid == null || this.ownerUuid.equals(living.getUUID()))) {
            this.owner = living;
            this.ownerUuid = living.getUUID();
            return living;
        }
        if (this.level() instanceof ServerLevel serverLevel && this.ownerUuid != null) {
            Entity byUuid = serverLevel.getEntity(this.ownerUuid);
            if (byUuid instanceof LivingEntity living) {
                this.owner = living;
                this.setOwnerId(living.getId());
                return living;
            }
        }
        return null;
    }

    private void resolveSlam(ServerLevel level, LivingEntity caster) {
        Vec3 forward = flatForward(this.getYRot());
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 origin = this.position().add(0.0D, 1.0D, 0.0D);
        AABB scan = this.getBoundingBox().inflate(18.0D, 7.0D, 18.0D);
        var targets = SaTargeting.targets(level, caster, scan,
                selected -> SaTargeting.canDamageAttackable(caster, selected.root()));

        for (var selected : targets) {
            LivingEntity target = selected.root();
            Vec3 center = selected.anchor();
            Vec3 offset = center.subtract(origin);
            double ahead = offset.dot(forward);
            double side = Math.abs(offset.dot(right));
            if (ahead < -1.0D || ahead > ATTACK_REACH
                    || side > ATTACK_BASE_HALF_WIDTH + ahead * 0.18D
                    || offset.y < -ATTACK_DOWN_REACH || offset.y > ATTACK_UP_REACH) {
                continue;
            }
            if (!SaTargeting.canDamage(caster, target)
                    || !SaDamage.hurtSlashArtNoIFrame(
                    selected.hitEntity(), level, this, caster, this.damage)) {
                continue;
            }
            target.setDeltaMovement(target.getDeltaMovement().scale(0.35D)
                    .add(forward.scale(0.55D)).add(0.0D, -0.18D, 0.0D));
            target.hurtMarked = true;
        }

        Vec3 impact = this.position().add(forward.scale(IMPACT_DISTANCE)).add(0.0D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 0.7D, impact.z,
                2, 0.25D, 0.25D, 0.25D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z,
                8, 2.4D, 0.35D, 2.4D, 0.08D);
        level.sendParticles(new DustParticleOptions(SOUL_RED, 2.0F), impact.x, impact.y, impact.z,
                110, 4.4D, 0.65D, 4.4D, 0.12D);
        level.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z,
                68, 4.0D, 0.55D, 4.0D, 0.10D);
    }

    private void playTimelineFx(ServerLevel level) {
        if (this.tickCount == 1) {
            level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.58F);
            level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 0.72F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.2D, this.getZ(),
                    38, 1.3D, 1.6D, 1.3D, 0.025D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(),
                    26, 1.15D, 1.35D, 1.15D, 0.045D);
            level.sendParticles(new DustParticleOptions(SOUL_RED, 1.55F),
                    this.getX(), this.getY() + 1.2D, this.getZ(), 44,
                    1.3D, 1.6D, 1.3D, 0.04D);
        } else if (this.tickCount == ASCEND_TICK) {
            level.playSound(null, this.getX(), this.getY() + 2.0D, this.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 0.55F);
        } else if (this.tickCount == SLAM_TICK) {
            level.playSound(null, this.getX(), this.getY() + 2.2D, this.getZ(),
                    SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.15F, 0.52F);
            level.playSound(null, this.getX(), this.getY() + 2.2D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 0.62F);
        } else if (this.tickCount == IMPACT_TICK) {
            Vec3 impact = this.position().add(flatForward(this.getYRot()).scale(IMPACT_DISTANCE));
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8F, 0.62F);
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.0F, 0.72F);
        }
    }

    private static Vec3 flatForward(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians)).normalize();
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseCaster();
        super.remove(reason);
    }

    private void releaseCaster() {
        if (!this.releasedCaster && this.ownerUuid != null) {
            ACTIVE_CASTERS.remove(this.ownerUuid);
            this.releasedCaster = true;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.damage = tag.getFloat("Damage");
        this.impacted = tag.getBoolean("Impacted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Damage", this.damage);
        tag.putBoolean("Impacted", this.impacted);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(24.0D, 8.0D, 24.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

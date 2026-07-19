package com.wjx.kablade.entity;

import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.slasharts.RaizanCleaveTimeline;
import com.wjx.kablade.specialeffect.ThunderBlitz;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative cast, damage and synchronization anchor for Raizan Cleave. */
public final class RaizanCleaveEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(RaizanCleaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START_TIME =
            SynchedEntityData.defineId(RaizanCleaveEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_SEED =
            SynchedEntityData.defineId(RaizanCleaveEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(RaizanCleaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(RaizanCleaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(RaizanCleaveEntity.class, EntityDataSerializers.FLOAT);

    private static final Set<UUID> ACTIVE_CASTERS = ConcurrentHashMap.newKeySet();
    private static final Vector3f ELECTRIC_BLUE = new Vector3f(0.20F, 0.72F, 1.0F);
    private static final double PHASE_ONE_RADIUS = 6.5D;
    private static final double HEART_HALF_WIDTH = 10.0D;
    private static final double HEART_HALF_DEPTH = 5.0D;
    private static final double HIT_BELOW = 2.5D;
    private static final double HIT_ABOVE = 6.5D;
    private static final int PARALYSIS_AMPLIFIER = 12;
    private static final int NARUKAMI_DURATION = 300;
    private static final String DOMAIN_OF_SANCTION_TRANSLATION_KEY =
            "item.kablade.domain_of_sanction";

    private UUID ownerUuid;
    private LivingEntity owner;
    private float totalDamage;
    private double castY;
    private int nextHit;
    private boolean releasedActiveCaster;

    public RaizanCleaveEntity(EntityType<? extends RaizanCleaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static boolean isCasting(LivingEntity owner) {
        return owner != null && ACTIVE_CASTERS.contains(owner.getUUID());
    }

    public static RaizanCleaveEntity spawn(ServerLevel level, LivingEntity owner, Vec3 origin,
                                            Vec3 targetAnchor, float yaw, float totalDamage) {
        if (!ACTIVE_CASTERS.add(owner.getUUID())) {
            return null;
        }

        RaizanCleaveEntity entity = new RaizanCleaveEntity(ModEntities.RAIZAN_CLEAVE.get(), level);
        entity.owner = owner;
        entity.ownerUuid = owner.getUUID();
        entity.totalDamage = totalDamage;
        entity.castY = origin.y;
        entity.setPos(origin);
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.setOwnerId(owner.getId());
        entity.setStartGameTime(level.getGameTime());
        entity.setSeed(level.random.nextLong() ^ owner.getUUID().getMostSignificantBits());
        entity.setTargetAnchor(targetAnchor);
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
        this.entityData.define(DATA_SEED, 0L);
        this.entityData.define(DATA_TARGET_X, 0.0F);
        this.entityData.define(DATA_TARGET_Y, 0.0F);
        this.entityData.define(DATA_TARGET_Z, 0.0F);
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    private void setOwnerId(int ownerId) {
        this.entityData.set(DATA_OWNER_ID, ownerId);
    }

    public long getStartGameTime() {
        return this.entityData.get(DATA_START_TIME);
    }

    private void setStartGameTime(long time) {
        this.entityData.set(DATA_START_TIME, time);
    }

    public long getSeed() {
        return this.entityData.get(DATA_SEED);
    }

    private void setSeed(long seed) {
        this.entityData.set(DATA_SEED, seed);
    }

    public Vec3 getTargetAnchor() {
        return this.position().add(this.entityData.get(DATA_TARGET_X),
                this.entityData.get(DATA_TARGET_Y), this.entityData.get(DATA_TARGET_Z));
    }

    private void setTargetAnchor(Vec3 target) {
        Vec3 offset = target.subtract(this.position());
        this.entityData.set(DATA_TARGET_X, (float) offset.x);
        this.entityData.set(DATA_TARGET_Y, (float) offset.y);
        this.entityData.set(DATA_TARGET_Z, (float) offset.z);
    }

    public float getRenderAge(float partialTick) {
        if (this.level() == null) {
            return this.tickCount + partialTick;
        }
        return Mth.clamp((float) (this.level().getGameTime() + partialTick - getStartGameTime()),
                0.0F, RaizanCleaveTimeline.DURATION_TICKS);
    }

    public float getReferenceFrame(float partialTick) {
        return RaizanCleaveTimeline.referenceFrame(getRenderAge(partialTick));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        LivingEntity caster = resolveOwner();
        if (caster == null || !caster.isAlive() || !stillHoldingRaizan(caster)
                || Math.abs(caster.getY() - this.castY) > 3.0D) {
            discard();
            return;
        }

        followCaster(caster);
        lockCaster(caster);
        playTimelineSounds((ServerLevel) this.level());

        while (this.nextHit < RaizanCleaveTimeline.HIT_TICKS.length
                && this.tickCount >= RaizanCleaveTimeline.HIT_TICKS[this.nextHit]) {
            resolveHit((ServerLevel) this.level(), caster, this.nextHit);
            this.nextHit++;
        }

        if (this.tickCount >= RaizanCleaveTimeline.DURATION_TICKS) {
            discard();
        }
    }

    private void followCaster(LivingEntity caster) {
        // DATA_TARGET_* stores an offset from this entity, so moving the entity also moves
        // every visual anchor and every server-authoritative hit region by the same delta.
        // This handles knockback, scripted movement and corrected client positions without
        // letting the floating weapon remain at the original cast point.
        Vec3 casterPosition = caster.position();
        this.setPos(casterPosition.x, casterPosition.y, casterPosition.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private LivingEntity resolveOwner() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }
        Entity byId = this.level().getEntity(getOwnerId());
        if (byId instanceof LivingEntity living && (this.ownerUuid == null
                || this.ownerUuid.equals(living.getUUID()))) {
            this.owner = living;
            this.ownerUuid = living.getUUID();
            return living;
        }
        return null;
    }

    private static boolean stillHoldingRaizan(LivingEntity caster) {
        return caster.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> ModSlashArts.RAIZAN_CLEAVE.getId().equals(state.getSlashArtsKey())
                        && DOMAIN_OF_SANCTION_TRANSLATION_KEY.equals(state.getTranslationKey()))
                .orElse(false);
    }

    private void lockCaster(LivingEntity caster) {
        Vec3 velocity = caster.getDeltaMovement();
        caster.setDeltaMovement(0.0D, velocity.y, 0.0D);
        if (caster.distanceToSqr(this.getX(), caster.getY(), this.getZ()) > 1.0E-4D) {
            caster.setPos(this.getX(), caster.getY(), this.getZ());
        }
        caster.setYRot(this.getYRot());
        caster.yBodyRot = this.getYRot();
        caster.yHeadRot = this.getYRot();
        caster.hurtMarked = true;
        caster.hasImpulse = true;
    }

    private void resolveHit(ServerLevel level, LivingEntity caster, int hitIndex) {
        Vec3 center = getTargetAnchor();
        List<LivingEntity> targets = hitIndex < 2
                ? phaseOneTargets(level, caster, center)
                : heartTargets(level, caster, center);
        if (targets.isEmpty()) {
            playMissFx(level, center, hitIndex);
            return;
        }

        float damage = this.totalDamage * RaizanCleaveTimeline.DAMAGE_WEIGHTS[hitIndex];
        int paralysisTicks = Math.max(1,
                RaizanCleaveTimeline.DURATION_TICKS - this.tickCount + 1);
        for (LivingEntity target : targets) {
            if (!SaTargeting.canDamage(caster, target)) {
                continue;
            }
            if (!SaDamage.hurtSlashArtNoIFrame(target, level, this, caster, damage)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                    paralysisTicks, PARALYSIS_AMPLIFIER, false, true, true));
            target.getPersistentData().putInt(ThunderBlitz.NARUKAMI_TAG, NARUKAMI_DURATION);
            if (hitIndex == RaizanCleaveTimeline.HIT_TICKS.length - 1) {
                Vec3 left = localLeft();
                target.setDeltaMovement(target.getDeltaMovement().scale(0.35D)
                        .add(left.scale(0.35D)).add(0.0D, 0.12D, 0.0D));
                target.hurtMarked = true;
            }
            if (caster instanceof Player player) {
                player.crit(target);
            }
        }
        playHitFx(level, center, hitIndex);
    }

    private List<LivingEntity> phaseOneTargets(ServerLevel level, LivingEntity caster, Vec3 center) {
        AABB box = new AABB(center.x - PHASE_ONE_RADIUS, center.y - HIT_BELOW,
                center.z - PHASE_ONE_RADIUS, center.x + PHASE_ONE_RADIUS,
                center.y + HIT_ABOVE, center.z + PHASE_ONE_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                target -> target.isPickable()
                        && SaTargeting.canDamageAttackable(caster, target)
                        && horizontalDistanceSqr(target.position(), center)
                        <= PHASE_ONE_RADIUS * PHASE_ONE_RADIUS);
    }

    private List<LivingEntity> heartTargets(ServerLevel level, LivingEntity caster, Vec3 center) {
        double scan = HEART_HALF_WIDTH + HEART_HALF_DEPTH;
        AABB box = new AABB(center.x - scan, center.y - HIT_BELOW, center.z - scan,
                center.x + scan, center.y + HIT_ABOVE, center.z + scan);
        Vec3 forward = flatForward();
        Vec3 left = localLeft();
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> {
            if (!target.isPickable() || !SaTargeting.canDamageAttackable(caster, target)) {
                return false;
            }
            Vec3 offset = target.position().subtract(center);
            return Math.abs(offset.dot(left)) <= HEART_HALF_WIDTH
                    && Math.abs(offset.dot(forward)) <= HEART_HALF_DEPTH;
        });
    }

    private void playTimelineSounds(ServerLevel level) {
        Vec3 anchor = getTargetAnchor();
        if (this.tickCount == 6 || this.tickCount == 45) {
            level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.62F,
                    this.tickCount == 6 ? 1.48F : 1.18F);
        } else if (this.tickCount == 14 || this.tickCount == 50) {
            level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.78F, 1.62F);
        } else if (this.tickCount == 89 || this.tickCount == 96) {
            level.playSound(null, this.getX(), this.getY() + 1.1D, this.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.55F,
                    this.tickCount == 89 ? 1.52F : 1.82F);
        } else if (this.tickCount == 69) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.08F, 1.28F);
        }
    }

    private void playHitFx(ServerLevel level, Vec3 center, int hitIndex) {
        boolean phaseTwo = hitIndex >= 2;
        int count = phaseTwo ? 18 : 28;
        level.sendParticles(new DustParticleOptions(ELECTRIC_BLUE, phaseTwo ? 1.15F : 1.35F),
                center.x, center.y, center.z, count,
                phaseTwo ? 2.2D : 1.25D, 1.1D, phaseTwo ? 2.2D : 1.25D, 0.05D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                count / 2, 1.25D, 0.9D, 1.25D, 0.09D);
        level.playSound(null, center.x, center.y, center.z,
                phaseTwo ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, phaseTwo ? 0.82F : 1.08F, 1.24F + hitIndex * 0.055F);
        if (hitIndex == RaizanCleaveTimeline.HIT_TICKS.length - 1) {
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.82F, 1.62F);
        }
    }

    private void playMissFx(ServerLevel level, Vec3 center, int hitIndex) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                hitIndex < 2 ? 8 : 5, 0.65D, 0.6D, 0.65D, 0.05D);
    }

    private Vec3 flatForward() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).normalize();
    }

    private Vec3 localLeft() {
        Vec3 forward = flatForward();
        return new Vec3(forward.z, 0.0D, -forward.x);
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double x = a.x - b.x;
        double z = a.z - b.z;
        return x * x + z * z;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseActiveCaster();
        super.remove(reason);
    }

    private void releaseActiveCaster() {
        if (!this.releasedActiveCaster && this.ownerUuid != null) {
            ACTIVE_CASTERS.remove(this.ownerUuid);
            this.releasedActiveCaster = true;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.totalDamage = tag.getFloat("TotalDamage");
        this.castY = tag.getDouble("CastY");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("TotalDamage", this.totalDamage);
        tag.putDouble("CastY", this.castY);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(14.0D, 8.0D, 14.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

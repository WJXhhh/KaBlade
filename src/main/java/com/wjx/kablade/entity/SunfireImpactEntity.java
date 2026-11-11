package com.wjx.kablade.entity;

import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.compat.SstBladeCompat;
import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.network.SunfireImpactPacket;
import com.wjx.kablade.slasharts.SunfireImpactTimeline;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Real tick-driven cast; MinecraftServer's task queue is not a delayed tick scheduler. */
public final class SunfireImpactEntity extends Entity {
    private static final String ACTIVE_CAST_TAG = "kabladeSunfireCast";
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> LANDING = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> IMPACT = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> CENTER = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<CompoundTag> BURNING = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> PREVIOUS_NO_GRAVITY = SynchedEntityData.defineId(SunfireImpactEntity.class, EntityDataSerializers.BOOLEAN);
    private LivingEntity owner;
    private UUID ownerUuid;
    private ItemStack blade;
    private float damage;
    private boolean previousNoGravity;
    private boolean controlling;
    private final Map<UUID, SaTarget> burns = new LinkedHashMap<>();

    public SunfireImpactEntity(EntityType<? extends SunfireImpactEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public static boolean isCasting(LivingEntity owner) {
        if (owner.level() instanceof ServerLevel level) {
            Entity active = level.getEntity(owner.getPersistentData().getInt(ACTIVE_CAST_TAG));
            return active instanceof SunfireImpactEntity cast && cast.owner == owner && cast.controlling && !cast.isRemoved();
        }
        return !owner.level().getEntitiesOfClass(SunfireImpactEntity.class, owner.getBoundingBox().inflate(24),
                cast -> cast.getOwnerId() == owner.getId() && cast.isControlling()).isEmpty();
    }

    public static SunfireImpactEntity spawn(ServerLevel level, LivingEntity owner, Vec3 requested, float damage) {
        if (isCasting(owner) || owner.isPassenger() || owner.isFallFlying() || owner.noPhysics) return null;
        Vec3 origin = owner.position();
        Vec3 landing = safeLanding(level, owner, requested);
        if (landing == null) return null;
        SunfireImpactEntity cast = new SunfireImpactEntity(ModEntities.SUNFIRE_IMPACT.get(), level);
        cast.setPos(origin);
        cast.owner = owner;
        cast.ownerUuid = owner.getUUID();
        cast.blade = owner.getMainHandItem();
        cast.damage = damage;
        cast.previousNoGravity = owner.isNoGravity();
        cast.entityData.set(PREVIOUS_NO_GRAVITY, cast.previousNoGravity);
        cast.controlling = true;
        cast.entityData.set(OWNER, owner.getId());
        cast.entityData.set(START, level.getGameTime());
        cast.entityData.set(LANDING, landing.subtract(origin).toVector3f());
        cast.entityData.set(HEIGHT, (float) (Math.max(origin.y, landing.y) + 3 - origin.y));
        Vec3 delta = landing.subtract(origin);
        cast.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        SstBladeCompat.snapshotAffinity(cast, owner);
        if (!level.addFreshEntity(cast)) return null;
        owner.getPersistentData().putInt(ACTIVE_CAST_TAG, cast.getId());
        owner.setNoGravity(true);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.fallDistance = 0;
        owner.hasImpulse = true;
        owner.hurtMarked = true;
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9F, 0.7F);
        return cast;
    }

    private static Vec3 safeLanding(ServerLevel level, LivingEntity owner, Vec3 requested) {
        Vec3 origin = owner.position();
        for (int step = 8; step >= 0; step--) {
            Vec3 candidate = origin.lerp(requested, step / 8.0D);
            if (!level.hasChunkAt(BlockPos.containing(candidate))) continue;
            double top = Math.max(origin.y, candidate.y) + 1.5;
            HitResult ground = level.clip(new ClipContext(new Vec3(candidate.x, top, candidate.z),
                    new Vec3(candidate.x, Math.max(level.getMinBuildHeight(), candidate.y - 12), candidate.z),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (ground.getType() != HitResult.Type.BLOCK) continue;
            Vec3 landing = ground.getLocation().add(0, 0.001, 0);
            double apex = Math.max(origin.y, landing.y) + 3;
            boolean clear = true;
            Vec3 previous = origin;
            for (int tick = SunfireImpactTimeline.WINDUP + 1; tick <= SunfireImpactTimeline.EXPECTED_IMPACT; tick++) {
                Vec3 next = SunfireImpactTimeline.position(origin, landing, apex, tick);
                if (tick == SunfireImpactTimeline.EXPECTED_IMPACT) next = landing;
                int samples = Math.max(1, (int) Math.ceil(previous.distanceTo(next) / 0.15));
                for (int sample = 1; sample <= samples; sample++) {
                    Vec3 p = previous.lerp(next, sample / (double) samples);
                    var box = owner.getBoundingBox().move(p.subtract(origin));
                    if (!level.hasChunkAt(BlockPos.containing(p)) || !level.getWorldBorder().isWithinBounds(box)
                            || !level.noCollision(owner, box)) { clear = false; break; }
                }
                if (!clear) break;
                previous = next;
            }
            if (clear) return landing;
        }
        return null;
    }

    @Override protected void defineSynchedData() {
        entityData.define(OWNER, -1);
        entityData.define(START, 0L);
        entityData.define(LANDING, new Vector3f());
        entityData.define(HEIGHT, 3F);
        entityData.define(IMPACT, -1);
        entityData.define(CENTER, new Vector3f());
        entityData.define(BURNING, new CompoundTag());
        entityData.define(PREVIOUS_NO_GRAVITY, false);
    }

    public int getOwnerId() { return entityData.get(OWNER); }
    public boolean wasNoGravity() { return entityData.get(PREVIOUS_NO_GRAVITY); }
    public long getStartTime() { return entityData.get(START); }
    public int getImpactAge() { return entityData.get(IMPACT); }
    public float getAge(float partial) { return Math.max(0, level().getGameTime() - getStartTime() + partial); }
    public Vec3 getLanding() { return position().add(new Vec3(entityData.get(LANDING))); }
    public Vec3 getImpactCenter() { return position().add(new Vec3(entityData.get(CENTER))); }
    public double getApexY() { return getY() + entityData.get(HEIGHT); }
    public int[] getBurningIds() { return entityData.get(BURNING).getIntArray("Targets"); }
    public boolean isControlling() {
        if (!level().isClientSide()) return controlling;
        return getImpactAge() < 0 || getAge(0) < getImpactAge() + SunfireImpactTimeline.RECOVERY;
    }
    public Vec3 plannedPosition(float age) {
        return getImpactAge() >= 0 ? getImpactCenter()
                : SunfireImpactTimeline.position(position(), getLanding(), getApexY(), age);
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (owner == null || owner.isRemoved() || !owner.isAlive() || owner.level() != level
                || !owner.getUUID().equals(ownerUuid)) { discard(); return; }
        int age = (int) getAge(0);
        if (controlling && (owner.getMainHandItem() != blade || owner.noPhysics || owner.isPassenger() || owner.isFallFlying()
                || !blade.getCapability(ItemSlashBlade.BLADESTATE).map(state ->
                ModSlashArts.SUNFIRE_IMPACT.getId().equals(state.getSlashArtsKey())).orElse(false))) {
            discard(); return;
        }
        if (getImpactAge() < 0) {
            if (age > 40) { discard(); return; }
            moveCaster(level, age);
        } else {
            if (age < getImpactAge() + SunfireImpactTimeline.RECOVERY) moveCaster(level, age);
            else releaseCaster();
            tickBurns(level, age - getImpactAge());
            if (age >= getImpactAge() + SunfireImpactTimeline.BURN) discard();
        }
        if (age == SunfireImpactTimeline.WINDUP) {
            level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1F, 1.4F);
        }
        if (age == SunfireImpactTimeline.DIVE) {
            level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.3F, 0.6F);
        }
    }

    private void moveCaster(ServerLevel level, int age) {
        Vec3 desired = plannedPosition(age);
        if (owner.position().distanceToSqr(plannedPosition(Math.max(0, age - 1))) > 16) { discard(); return; }
        Vec3 motion = desired.subtract(owner.position());
        owner.setNoGravity(true);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.move(MoverType.SELF, motion);
        owner.fallDistance = 0;
        owner.hasImpulse = true;
        if (owner instanceof ServerPlayer player) {
            // The local client follows the shared curve. Teleporting here resets its previous
            // render position every 50 ms and defeats interpolation (as does sending zero velocity).
            // SunfireMovementMixin ignores stale position reports while this controller owns motion.
            player.connection.resetPosition();
        }
        if (getImpactAge() < 0 && age > SunfireImpactTimeline.DIVE && owner.verticalCollisionBelow) {
            impact(level, age);
        } else if (owner.position().distanceToSqr(desired) > 0.04) {
            discard();
        }
    }

    private void impact(ServerLevel level, int age) {
        if (getImpactAge() >= 0) return;
        Vec3 center = owner.position();
        entityData.set(IMPACT, age);
        entityData.set(CENTER, center.subtract(position()).toVector3f());
        var source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SaDamage.SLASH_ART), this, owner);
        List<Integer> hitIds = new ArrayList<>();
        for (SaTarget selected : SaTargeting.uniqueTargets(level, owner,
                owner.getBoundingBox().inflate(SunfireImpactTimeline.RADIUS), true)) {
            LivingEntity victim = selected.root();
            if (selected.distanceToSqr(center) > SunfireImpactTimeline.RADIUS * SunfireImpactTimeline.RADIUS
                    || !SaTargeting.canDamage(owner, selected.hitEntity())) continue;
            // Ignore previous i-frames, while retaining the new hit's animation and aggro.
            victim.invulnerableTime = 0;
            if (!SaDamage.hurt(selected.hitEntity(), source, damage)) continue;
            hitIds.add(victim.getId());
            Vec3 away = victim.position().subtract(center).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-6) away = away.normalize().scale(0.22);
            double resistance = victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            double strength = Math.max(0, 1 - resistance);
            victim.setDeltaMovement(away.x * strength, 0.9 * strength, away.z * strength);
            victim.hasImpulse = true;
            victim.hurtMarked = true;
            if (canBurn(victim)) {
                for (SunfireImpactEntity other : level.getEntitiesOfClass(SunfireImpactEntity.class,
                        getBoundingBox().inflate(24), cast -> cast != this && ownerUuid.equals(cast.ownerUuid))) {
                    if (other.burns.remove(victim.getUUID()) != null) other.syncBurns();
                }
                burns.put(victim.getUUID(), selected);
            }
        }
        syncBurns();
        KabladeNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                center.x, center.y, center.z, 64, level.dimension())),
                new SunfireImpactPacket(getId(), getOwnerId(), age, center, hitIds.stream().mapToInt(Integer::intValue).toArray()));
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.7F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 0.65F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4F, 0.7F);
    }

    private boolean canBurn(LivingEntity victim) {
        return victim.isAlive() && victim.level() == level() && !victim.fireImmune()
                && !victim.hasEffect(MobEffects.FIRE_RESISTANCE) && !victim.isInWaterRainOrBubble()
                && SaTargeting.canDamage(owner, victim);
    }

    private void tickBurns(ServerLevel level, int elapsed) {
        boolean changed = burns.values().removeIf(target -> !target.isAlive() || !canBurn(target.root()));
        if (elapsed > 0 && elapsed % 20 == 0) {
            DamageSource fire = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.ON_FIRE), this, owner);
            for (SaTarget target : burns.values()) SaDamage.hurt(target.hitEntity(), fire, 1F);
        }
        if (elapsed >= SunfireImpactTimeline.BURN && !burns.isEmpty()) { burns.clear(); changed = true; }
        if (changed) syncBurns();
    }

    private void syncBurns() {
        CompoundTag data = new CompoundTag();
        data.putIntArray("Targets", burns.values().stream().mapToInt(target -> target.root().getId()).toArray());
        entityData.set(BURNING, data);
    }

    private void releaseCaster() {
        if (!controlling) return;
        controlling = false;
        if (owner != null) {
            if (owner.getPersistentData().getInt(ACTIVE_CAST_TAG) == getId()) owner.getPersistentData().remove(ACTIVE_CAST_TAG);
            owner.setNoGravity(previousNoGravity);
            if (owner.level() == level()) {
                owner.setDeltaMovement(Vec3.ZERO);
                owner.fallDistance = 0;
                owner.hurtMarked = true;
                if (owner instanceof ServerPlayer player && !player.isRemoved()) {
                    // One final authoritative correction also handles an interrupted or shortened arc.
                    player.connection.teleport(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
                }
            }
        }
    }

    @Override public void remove(RemovalReason reason) { releaseCaster(); super.remove(reason); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}

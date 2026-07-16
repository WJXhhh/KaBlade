package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.network.RaidenCycloneEndPacket;
import com.wjx.kablade.network.RaidenCycloneFxPacket;
import com.wjx.kablade.specialeffect.EMPulsar;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** MAG-Typhoon's five-second magnetic lightning slash art. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaidenCycloneArts extends SlashArts {

    private static final Map<UUID, ActiveCast> ACTIVE = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_CAST_ID = new AtomicLong(1L);
    private static final double TARGET_RANGE = 8.0D;
    private static final double MAIN_HIT_MAX_RANGE = 10.0D;
    private static final float ATTACK_FACTOR = 10.0F;
    private static final float DAMAGE_MULTIPLIER = 3.0F;
    private static final int MOVEMENT_END_TICK =
            RaidenCycloneTimeline.HIT_TICKS[RaidenCycloneTimeline.HIT_TICKS.length - 1];
    private static final int PARALYSIS_DURATION = 100;
    private static final int PARALYSIS_AMPLIFIER = 3;
    private static final Vector3f ELECTRIC = new Vector3f(0.05F, 0.92F, 1.0F);
    private static final double REFERENCE_START_X = -1.54D;
    private static final double REFERENCE_START_Z = -0.40D;

    public RaidenCycloneArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        if (ACTIVE.containsKey(user.getUUID())) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        LivingEntity target = resolveTarget(level, user, blade);
        Vec3 origin = user.position();
        Vec3 targetPoint = target == null
                ? origin.add(SaFx.flatLook(user).scale(1.6D))
                : target.position();

        double referenceAngle = Math.atan2(REFERENCE_START_Z, REFERENCE_START_X);
        double worldAngle = Math.atan2(origin.z - targetPoint.z, origin.x - targetPoint.x);
        float basisRotation = (float) (worldAngle - referenceAngle);
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extra = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        float totalDamage = DAMAGE_MULTIPLIER * 1.35F * (60.0F + 2.0F * extra);

        long now = level.getServer().getTickCount();
        long castId = NEXT_CAST_ID.getAndIncrement();
        long seed = level.random.nextLong() ^ user.getUUID().getMostSignificantBits() ^ castId;
        ActiveCast cast = new ActiveCast(castId, level.dimension(), user.getUUID(),
                target == null ? null : target.getUUID(), now, level.getGameTime(), seed,
                origin, targetPoint, targetPoint, basisRotation, totalDamage);
        ACTIVE.put(user.getUUID(), cast);
        EMPulsar.activate(user, blade);

        KabladeNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> user),
                new RaidenCycloneFxPacket(castId, user.getId(), target == null ? -1 : target.getId(),
                        cast.startGameTime, seed,
                        origin.x, origin.y, origin.z,
                        targetPoint.x, targetPoint.y, targetPoint.z, basisRotation));

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.85F, 1.85F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.72F, 1.35F);
        level.sendParticles(new DustParticleOptions(ELECTRIC, 1.25F),
                origin.x, origin.y + user.getBbHeight() * 0.55D, origin.z,
                28, 0.65D, 0.75D, 0.65D, 0.035D);
        return super.doArts(type, user);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

        long now = event.getServer().getTickCount();
        ACTIVE.entrySet().removeIf(entry -> tickCast(event, now, entry.getValue()));
    }

    private static boolean tickCast(TickEvent.ServerTickEvent event, long now, ActiveCast cast) {
        ServerLevel level = event.getServer().getLevel(cast.dimension);
        if (level == null) return true;

        Entity ownerEntity = level.getEntity(cast.ownerUuid);
        if (!(ownerEntity instanceof LivingEntity user) || !user.isAlive() || !stillHoldingArt(user)) {
            if (ownerEntity instanceof LivingEntity living) {
                releaseMagneticThrust(living, cast);
                sendEnd(living, cast.castId, RaidenCycloneEndPacket.OWNER_LOST);
            }
            return true;
        }

        long elapsedTicks = now - cast.startServerTick;
        if (elapsedTicks > RaidenCycloneTimeline.DURATION_TICKS) {
            releaseMagneticThrust(user, cast);
            sendEnd(user, cast.castId, RaidenCycloneEndPacket.COMPLETE);
            return true;
        }

        if (elapsedTicks > MOVEMENT_END_TICK) {
            releaseMagneticThrust(user, cast);
        }

        LivingEntity target = null;
        if (cast.targetUuid != null) {
            Entity targetEntity = level.getEntity(cast.targetUuid);
            if (!(targetEntity instanceof LivingEntity living) || !living.isAlive()
                    || !isAttackable(user, living)
                    || living.position().distanceToSqr(cast.lastTargetPosition) > 144.0D) {
                releaseMagneticThrust(user, cast);
                sendEnd(user, cast.castId, RaidenCycloneEndPacket.TARGET_LOST);
                return true;
            }
            target = living;
            cast.lastTargetPosition = target.position();
        }

        float seconds = elapsedTicks / 20.0F;
        if (elapsedTicks <= MOVEMENT_END_TICK) {
            applyMagneticThrust(level, user, target == null ? cast.virtualTarget : target.position(),
                    cast.basisRotation, seconds);
        }

        while (cast.nextHit < RaidenCycloneTimeline.HIT_TICKS.length
                && elapsedTicks >= RaidenCycloneTimeline.HIT_TICKS[cast.nextHit]) {
            if (target != null) resolveHit(level, user, target, cast, cast.nextHit);
            cast.nextHit++;
        }
        return false;
    }

    private static void applyMagneticThrust(ServerLevel level, LivingEntity user, Vec3 targetPoint,
                                            float rotation, float seconds) {
        RaidenCycloneTimeline.LocalPose pose = RaidenCycloneTimeline.samplePlayer(seconds);
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        double offsetX = pose.x() * cos - pose.z() * sin;
        double offsetZ = pose.x() * sin + pose.z() * cos;
        Vec3 desired = new Vec3(targetPoint.x + offsetX, user.getY(), targetPoint.z + offsetZ);
        Vec3 error = desired.subtract(user.position()).multiply(0.45D, 0.0D, 0.45D);
        if (error.horizontalDistanceSqr() > 0.65D * 0.65D) {
            error = error.normalize().scale(0.65D);
        }

        Vec3 current = user.getDeltaMovement();
        double vx = current.x + error.x;
        double vz = current.z + error.z;
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (horizontal > 1.15D) {
            vx = vx / horizontal * 1.15D;
            vz = vz / horizontal * 1.15D;
        }

        AABB currentBox = user.getBoundingBox();
        if (!level.noCollision(user, currentBox.move(vx, 0.0D, 0.0D))) vx = current.x;
        if (!level.noCollision(user, currentBox.move(0.0D, 0.0D, vz))) vz = current.z;
        user.setDeltaMovement(vx, current.y, vz);
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    private static void releaseMagneticThrust(LivingEntity user, ActiveCast cast) {
        if (cast.movementReleased) return;
        Vec3 current = user.getDeltaMovement();
        user.setDeltaMovement(0.0D, current.y, 0.0D);
        user.hurtMarked = true;
        user.hasImpulse = true;
        cast.movementReleased = true;
    }

    private static void resolveHit(ServerLevel level, LivingEntity user, LivingEntity primary,
                                   ActiveCast cast, int hitIndex) {
        boolean mainPhase = hitIndex >= 5;
        Vec3 center;
        double radius;
        if (mainPhase) {
            if (user.distanceToSqr(primary) > MAIN_HIT_MAX_RANGE * MAIN_HIT_MAX_RANGE) return;
            center = primary.position().add(0.0D, primary.getBbHeight() * 0.48D, 0.0D);
            radius = 4.5D;
        } else {
            center = user.position().add(0.0D, user.getBbHeight() * 0.48D, 0.0D);
            radius = 3.25D;
        }

        float damage = cast.totalDamage * RaidenCycloneTimeline.DAMAGE_WEIGHTS[hitIndex];
        AABB bounds = AABB.ofSize(center, radius * 2.0D, 6.0D, radius * 2.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> isAttackable(user, target)
                        && horizontalDistanceSqr(target.position(), center) <= radius * radius);
        if (targets.isEmpty()) return;

        for (LivingEntity target : targets) {
            SaDamage.hurtSlashArtNoIFrame(target, level, user, damage);
            target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                    PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
            if (user instanceof Player player) player.crit(target);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                    mainPhase ? 18 : 10,
                    target.getBbWidth() * 0.42D, target.getBbHeight() * 0.32D,
                    target.getBbWidth() * 0.42D, 0.085D);
        }

        if (mainPhase ? !cast.mainDurabilityPaid : !cast.orbitDurabilityPaid) {
            user.getMainHandItem().hurtAndBreak(1, user,
                    entity -> entity.broadcastBreakEvent(user.getUsedItemHand()));
            if (mainPhase) cast.mainDurabilityPaid = true;
            else cast.orbitDurabilityPaid = true;
        }

        float pitch = 1.42F + hitIndex * 0.035F;
        level.playSound(null, center.x, center.y, center.z,
                mainPhase ? SoundEvents.TRIDENT_THUNDER : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, mainPhase ? 0.72F : 0.52F, pitch);
    }

    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level)).orElse(null);
        if (locked instanceof LivingEntity living && isValidCandidate(user, living)) return living;

        Vec3 eye = user.getEyePosition();
        Vec3 end = eye.add(user.getLookAngle().scale(TARGET_RANGE));
        LivingEntity crosshair = level.getEntitiesOfClass(LivingEntity.class,
                        user.getBoundingBox().expandTowards(user.getLookAngle().scale(TARGET_RANGE)).inflate(1.0D),
                        target -> isValidCandidate(user, target))
                .stream()
                .filter(target -> target.getBoundingBox().inflate(target.getPickRadius() + 0.25D)
                        .clip(eye, end).isPresent())
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(user)))
                .orElse(null);
        if (crosshair != null) return crosshair;

        return level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(TARGET_RANGE),
                        target -> isValidCandidate(user, target))
                .stream().min(Comparator.comparingDouble(target -> target.distanceToSqr(user)))
                .orElse(null);
    }

    private static boolean isValidCandidate(LivingEntity user, LivingEntity target) {
        return target.distanceToSqr(user) <= TARGET_RANGE * TARGET_RANGE && isAttackable(user, target);
    }

    private static boolean isAttackable(LivingEntity user, LivingEntity target) {
        if (!target.isPickable()) return false;
        try {
            return SaTargeting.canDamageAttackable(user, target);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    private static boolean stillHoldingArt(LivingEntity user) {
        return user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> ModSlashArts.RAIDEN_CYCLONE.getId().equals(state.getSlashArtsKey()))
                .orElse(false);
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static void sendEnd(LivingEntity user, long castId, byte reason) {
        KabladeNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> user),
                new RaidenCycloneEndPacket(castId, reason));
    }

    private static final class ActiveCast {
        private final long castId;
        private final ResourceKey<Level> dimension;
        private final UUID ownerUuid;
        private final UUID targetUuid;
        private final long startServerTick;
        private final long startGameTime;
        private final long seed;
        private final Vec3 origin;
        private final Vec3 virtualTarget;
        private Vec3 lastTargetPosition;
        private final float basisRotation;
        private final float totalDamage;
        private int nextHit;
        private boolean orbitDurabilityPaid;
        private boolean mainDurabilityPaid;
        private boolean movementReleased;

        private ActiveCast(long castId, ResourceKey<Level> dimension, UUID ownerUuid, UUID targetUuid,
                           long startServerTick, long startGameTime, long seed, Vec3 origin,
                           Vec3 virtualTarget, Vec3 lastTargetPosition, float basisRotation,
                           float totalDamage) {
            this.castId = castId;
            this.dimension = dimension;
            this.ownerUuid = ownerUuid;
            this.targetUuid = targetUuid;
            this.startServerTick = startServerTick;
            this.startGameTime = startGameTime;
            this.seed = seed;
            this.origin = origin;
            this.virtualTarget = virtualTarget;
            this.lastTargetPosition = lastTargetPosition;
            this.basisRotation = basisRotation;
            this.totalDamage = totalDamage;
        }
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.FallingPetalsMarkPacket;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SakuraEnd;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FallingPetalsArts extends SlashArts {

    private static final Map<MarkKey, Long> ACTIVE_MARKS = new ConcurrentHashMap<>();
    private static final Map<PendingSlashKey, PendingSlash> PENDING_SAKURA_ENDS = new ConcurrentHashMap<>();
    private static final String MARK_EXPIRES_AT = "kablade.falling_petals.expires_at";
    private static final double RAY_DISTANCE = 10.0D;
    private static final float BASE_DAMAGE = 15.0F;
    private static final float ATTACK_FACTOR = 12.0F;
    private static final int MARK_DURATION = 100;
    private static final int SLOW_DURATION = 40;
    private static final int SLOW_AMPLIFIER = 2;
    private static final int PETAL_COLOR = 0xFF83D2;

    public FallingPetalsArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }
        if (type == ArtsType.Fail) {
            SaFx.cancelPiercingCharge(user);
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        ResourceLocation combo = SaFx.startPiercingCombo(user, type);
        SaFx.applyPiercingBoost(user);

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        LivingEntity marked = damageLine(level, user, damage);
        if (marked != null) {
            markTarget(level, marked);
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        queueSakuraEnd(level, user, 4, 22.5F, false, 0.5D, true);
        queueSakuraEnd(level, user, 6, 157.5F, true, 0.76D, false);

        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        for (int i = 0; i < 18; i++) {
            double d = i * 0.45D;
            level.sendParticles(ParticleTypes.END_ROD,
                    eye.x + look.x * d,
                    eye.y + look.y * d,
                    eye.z + look.z * d,
                    1, 0.12D, 0.12D, 0.12D, 0.02D);
        }

        return combo;
    }

    private static LivingEntity damageLine(ServerLevel level, LivingEntity user, float damage) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));
        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(1.0D, 1.0D, 1.0D);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, target -> isAttackable(user, target));
        DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        LivingEntity closest = null;
        double closestDistance = RAY_DISTANCE;
        boolean hitAny = false;

        for (LivingEntity target : candidates) {
            AABB box = target.getBoundingBox().inflate(target.getPickRadius());
            var hit = box.clip(eye, end);
            double distance;
            if (box.contains(eye)) {
                distance = 0.0D;
            } else if (hit.isPresent()) {
                distance = eye.distanceTo(hit.get());
            } else {
                continue;
            }

            if (user instanceof Player player) {
                player.crit(target);
            }
            target.invulnerableTime = 0;
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, source, damage);
            hitAny = true;

            if (distance < closestDistance) {
                closest = target;
                closestDistance = distance;
            }
        }

        return hitAny ? closest : null;
    }

    private static boolean isAttackable(LivingEntity user, LivingEntity target) {
        if (!target.isPickable()) {
            return false;
        }
        return SaTargeting.canDamageAttackable(user, target);
    }

    private static void markTarget(ServerLevel level, LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        long expiresAt = level.getGameTime() + MARK_DURATION;
        data.putLong(MARK_EXPIRES_AT, expiresAt);
        ACTIVE_MARKS.put(new MarkKey(level.dimension(), target.getId()), expiresAt);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_DURATION, 0, false, false));
        KabladeNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new FallingPetalsMarkPacket(target.getId(), MARK_DURATION));
        spawnMarkParticles(level, target);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        long expiresAt = data.getLong(MARK_EXPIRES_AT);
        if (expiresAt <= target.level().getGameTime()) {
            data.remove(MARK_EXPIRES_AT);
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Player && SaTargeting.canDamage(attacker, target)) {
            event.setAmount(event.getAmount() * 2.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOW_DURATION, SLOW_AMPLIFIER));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long now = event.getServer().getTickCount();
        PENDING_SAKURA_ENDS.entrySet().removeIf(entry -> {
            PendingSlash slash = entry.getValue();
            if (now < slash.dueTick()) {
                return false;
            }

            ServerLevel level = event.getServer().getLevel(slash.dimension());
            if (level == null) {
                return true;
            }
            Entity entity = level.getEntity(slash.ownerUuid());
            if (!(entity instanceof LivingEntity user) || !user.isAlive()) {
                return true;
            }

            SakuraEnd.doSlash(user, slash.roll(), PETAL_COLOR, Vec3.ZERO, false, slash.critical(), slash.damage(),
                    mods.flammpfeil.slashblade.util.KnockBacks.cancel);
            if (slash.playSound()) {
                level.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.1F, 1.35F);
            }
            return true;
        });

        if (ACTIVE_MARKS.isEmpty() || now % 4 != 0) {
            return;
        }

        ACTIVE_MARKS.entrySet().removeIf(entry -> {
            MarkKey key = entry.getKey();
            ServerLevel level = event.getServer().getLevel(key.dimension());
            if (level == null) {
                return true;
            }
            Entity entity = level.getEntity(key.entityId());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()
                    || level.getGameTime() >= entry.getValue()) {
                if (entity instanceof LivingEntity living) {
                    living.getPersistentData().remove(MARK_EXPIRES_AT);
                }
                return true;
            }

            spawnMarkParticles(level, target);
            return false;
        });
    }

    private static void spawnMarkParticles(ServerLevel level, LivingEntity target) {
        double y = target.getY() + target.getBbHeight() * 0.55D;
        double radius = Math.max(0.45D, target.getBbWidth() * 0.75D);
        DustParticleOptions pink = new DustParticleOptions(SaFx.rgb(PETAL_COLOR), 1.1F);

        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0D * i / 12.0D + level.getGameTime() * 0.18D;
            double x = target.getX() + Math.cos(a) * radius;
            double z = target.getZ() + Math.sin(a) * radius;
            level.sendParticles(pink, x, y, z, 1, 0.0D, 0.015D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                target.getX(), y, target.getZ(), 3, radius * 0.35D, 0.2D, radius * 0.35D, 0.01D);
    }

    private static void queueSakuraEnd(ServerLevel level, LivingEntity user, int delayTicks,
                                       float roll, boolean critical, double damage, boolean playSound) {
        long dueTick = level.getServer().getTickCount() + delayTicks;
        PendingSlash slash = new PendingSlash(level.dimension(), user.getUUID(), dueTick,
                roll, critical, damage, playSound);
        PENDING_SAKURA_ENDS.put(new PendingSlashKey(user.getUUID(), dueTick, roll), slash);
    }

    private record MarkKey(ResourceKey<Level> dimension, int entityId) {
    }

    private record PendingSlashKey(UUID ownerUuid, long dueTick, float roll) {
    }

    private record PendingSlash(ResourceKey<Level> dimension, UUID ownerUuid, long dueTick,
                                float roll, boolean critical, double damage, boolean playSound) {
    }
}

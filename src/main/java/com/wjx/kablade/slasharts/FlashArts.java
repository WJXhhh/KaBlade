package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Flash, adapted from the 1.12.2 AL_WeiZhan slash art.
 */
public final class FlashArts extends SlashArts {

    private static final double LOCKED_TARGET_RANGE = 100.0D;
    private static final double WATCH_TARGET_RANGE = 30.0D;
    private static final int HIT_COUNT = 8;
    private static final int HITS_PER_TICK = 2;

    public FlashArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        ServerLevel level = (ServerLevel) user.level();

        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (bladeAttack / 3.0F) * 2.0F + MathFunc.amplifierCalc(bladeAttack, 1.0F);

        LivingEntity target = resolveTarget(level, user);
        if (target == null) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
            return super.doArts(type, user);
        }

        prepareTarget(user, target);
        scheduleFlashHits(level, user, target, damage);

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.15F, 1.45F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 1.8F);

        return super.doArts(type, user);
    }

    private static void prepareTarget(LivingEntity user, LivingEntity target) {
        if (user instanceof Player player) {
            player.crit(target);
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.hurtTime = 0;
        target.invulnerableTime = 0;
        user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(state -> state.setTargetEntityId(target));
    }

    private static void scheduleFlashHits(ServerLevel level, LivingEntity user, LivingEntity target, float damage) {
        for (int i = 0; i < HIT_COUNT; i++) {
            int hitIndex = i;
            int delay = i / HITS_PER_TICK;
            SaFx.schedule(level, delay, () -> applyFlashHit(level, user, target, damage, hitIndex));
        }
    }

    private static void applyFlashHit(ServerLevel level, LivingEntity user, LivingEntity target,
                                      float damage, int hitIndex) {
        if (!user.isAlive() || !target.isAlive()) {
            return;
        }

        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.hurtTime = 0;
        target.invulnerableTime = 0;
        target.hurt(damageSource(level, user), damage);
        target.invulnerableTime = 0;

        spawnSlashLine(level, target, hitIndex);
        if (hitIndex % HITS_PER_TICK == 0) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.65F, 1.55F + hitIndex * 0.035F);
        }
    }

    private static DamageSource damageSource(ServerLevel level, LivingEntity user) {
        if (user instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        return level.damageSources().mobAttack(user);
    }

    private static void spawnSlashLine(ServerLevel level, LivingEntity target, int hitIndex) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
        double yaw = Math.toRadians(target.getYRot() + hitIndex * 45.0D);
        Vec3 horizontal = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 vertical = new Vec3(0.0D, hitIndex % 2 == 0 ? 0.72D : -0.72D, 0.0D);
        Vec3 slash = horizontal.scale(0.9D).add(vertical).normalize();

        for (int step = -4; step <= 4; step++) {
            Vec3 p = center.add(slash.scale(step * 0.18D));
            level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z,
                    2, 0.04D, 0.04D, 0.04D, 0.015D);
            if ((step + hitIndex) % 3 == 0) {
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                        1, 0.02D, 0.02D, 0.02D, 0.08D);
            }
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user) {
        Entity locked = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity living && isValidLockedTarget(user, living)) {
            return living;
        }

        Entity watched = SATool.getEntityToWatch(user);
        if (watched instanceof LivingEntity living && isValidWatchedTarget(user, living)) {
            return living;
        }

        return null;
    }

    private static boolean isValidLockedTarget(LivingEntity user, LivingEntity target) {
        return SaTargeting.canDamage(user, target)
                && target.distanceToSqr(user) <= LOCKED_TARGET_RANGE * LOCKED_TARGET_RANGE;
    }

    private static boolean isValidWatchedTarget(LivingEntity user, LivingEntity target) {
        return isValidLockedTarget(user, target)
                && target.distanceToSqr(user) <= WATCH_TARGET_RANGE * WATCH_TARGET_RANGE;
    }
}

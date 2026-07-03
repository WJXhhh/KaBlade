package com.wjx.kablade.slasharts;

import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * Absolute Zero -- Plasma Kagehide SA, ported from 1.12.2 HonkaiAbsoluteZero.
 */
public final class AbsoluteZeroArts extends SlashArts {

    private static final double RAY_DISTANCE = 8.0D;
    private static final float BASE_DAMAGE = 20.0F;
    private static final float ATTACK_FACTOR = 5.0F;
    private static final int FREEZE_DURATION = 140;
    private static final int FREEZE_AMPLIFIER = 1;
    private static final int SMOKE_COUNT = 60;

    public AbsoluteZeroArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        LivingEntity target = raycastTarget(level, user);

        if (target != null) {
            if (user instanceof Player player) {
                player.crit(target);
            }

            float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                    .map(ISlashBladeState::getBaseAttackModifier)
                    .orElse(4.0F);
            float damage = BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
            DamageSource source = user instanceof Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(user);

            target.invulnerableTime = 0;
            target.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(),
                    FREEZE_DURATION, FREEZE_AMPLIFIER));
            target.hurt(source, damage);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(),
                    target.getTicksRequiredToFreeze() + FREEZE_DURATION));
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        spawnSmoke(level, user);
        return super.doArts(type, user);
    }

    private static void spawnSmoke(ServerLevel level, LivingEntity user) {
        for (int i = 0; i < SMOKE_COUNT; i++) {
            double xSign = level.random.nextBoolean() ? 1.0D : -1.0D;
            double zSign = level.random.nextBoolean() ? 1.0D : -1.0D;
            level.sendParticles(ParticleTypes.SMOKE,
                    user.getX() + level.random.nextDouble() * 3.0D * xSign,
                    user.getY() + level.random.nextDouble(),
                    user.getZ() + level.random.nextDouble() * 3.0D * zSign,
                    1, 0.0D, 0.1D, 0.0D, 0.0D);
        }
    }

    private static LivingEntity raycastTarget(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));
        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(1.0D, 1.0D, 1.0D);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, e -> e != user && canBeFrozenTarget(e));

        LivingEntity pointed = null;
        double closestDistance = RAY_DISTANCE;

        for (LivingEntity candidate : candidates) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hit = box.clip(eye, end);

            if (box.contains(eye)) {
                if (closestDistance >= 0.0D) {
                    pointed = candidate;
                    closestDistance = 0.0D;
                }
            } else if (hit.isPresent()) {
                double distance = eye.distanceTo(hit.get());
                if (distance < closestDistance || closestDistance == 0.0D) {
                    if (candidate.getRootVehicle() == user.getRootVehicle() && !user.canRiderInteract()) {
                        if (closestDistance == 0.0D) {
                            pointed = candidate;
                        }
                    } else {
                        pointed = candidate;
                        closestDistance = distance;
                    }
                }
            }
        }

        return pointed;
    }

    private static boolean canBeFrozenTarget(LivingEntity entity) {
        if (!entity.isAlive() || !entity.isPickable()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isSpectator();
        }
        return entity instanceof Mob;
    }
}

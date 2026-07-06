package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.FreezeDomainEntity;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Function;

/** Snow Dance, Ice Epiphyllum's actual 1.12.2 slash art bound by SpecialAttackType 299. */
public final class SnowDanceArts extends SlashArts {

    private static final int DOMAIN_FREEZE_DURATION = 60;
    private static final int DOMAIN_FREEZE_AMPLIFIER = 0;
    private static final double DOMAIN_RANGE_XZ = 8.0D;
    private static final double DOMAIN_RANGE_UP = 4.0D;
    private static final double AREA_RANGE = 4.0D;
    private static final float BASE_DAMAGE = 20.0F;
    private static final float ATTACK_FACTOR = 12.0F;

    public SnowDanceArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();

        FreezeDomainEntity.spawn(level, user);
        freezeInitialDomainTargets(level, user);
        damageNearby(level, user, blade);
        spawnClouds(level, user);

        return super.doArts(type, user);
    }

    private static void freezeInitialDomainTargets(ServerLevel level, LivingEntity user) {
        AABB box = new AABB(
                user.getX() - DOMAIN_RANGE_XZ, user.getY(), user.getZ() - DOMAIN_RANGE_XZ,
                user.getX() + DOMAIN_RANGE_XZ, user.getY() + DOMAIN_RANGE_UP, user.getZ() + DOMAIN_RANGE_XZ);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                target -> SaTargeting.canDamage(user, target));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(),
                    DOMAIN_FREEZE_DURATION, DOMAIN_FREEZE_AMPLIFIER));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(),
                    target.getTicksRequiredToFreeze() + DOMAIN_FREEZE_DURATION));
        }
    }

    private static void damageNearby(ServerLevel level, LivingEntity user, ItemStack blade) {
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        AABB box = user.getBoundingBox()
                .inflate(AREA_RANGE, AREA_RANGE, AREA_RANGE)
                .move(user.getDeltaMovement());
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                target -> SaTargeting.canDamage(user, target));

        for (LivingEntity target : targets) {
            if (user instanceof Player player) {
                player.crit(target);
            }
            target.invulnerableTime = 0;
            target.hurt(source, damage);
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }
    }

    private static void spawnClouds(ServerLevel level, LivingEntity user) {
        for (int i = 0; i < 60; i++) {
            double sx = level.random.nextBoolean() ? 1.0D : -1.0D;
            double sz = level.random.nextBoolean() ? 1.0D : -1.0D;
            level.sendParticles(ParticleTypes.CLOUD,
                    user.getX() + level.random.nextDouble() * 3.0D * sx,
                    user.getY() + level.random.nextDouble(),
                    user.getZ() + level.random.nextDouble() * 3.0D * sz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

}

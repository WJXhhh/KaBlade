package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Liedi, ported from the 1.12.2 AL_Liedi slash art.
 */
public final class LiediArts extends SlashArts {

    private static final double RANGE_XZ = 12.0D;
    private static final double RANGE_Y = 5.0D;
    private static final float EXTRA_DAMAGE = 93.75F;

    public LiediArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 3, false, true));

        AABB box = user.getBoundingBox()
                .inflate(RANGE_XZ, RANGE_Y, RANGE_XZ)
                .move(user.getDeltaMovement());

        DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                target -> SaTargeting.canDamageAttackable(user, target))) {
            AttackManager.doMeleeAttack(user, target, true, true);
            if (user instanceof Player player) {
                player.crit(target);
            }
            target.setDeltaMovement(0.0D, 2.0D, 0.0D);
            target.hasImpulse = true;
            target.hurtMarked = true;
            target.invulnerableTime = 0;
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, user, EXTRA_DAMAGE);
            target.invulnerableTime = 0;
        }

        Vec3 center = user.position().add(0.0D, 0.2D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
                8, 2.6D, 0.2D, 2.6D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 1.0D, center.z,
                40, 3.0D, 1.2D, 3.0D, 0.22D);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1F, 0.9F);

        return super.doArts(type, user);
    }
}

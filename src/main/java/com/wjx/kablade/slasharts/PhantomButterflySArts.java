package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ButterflySwordEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import com.wjx.kablade.util.SaTarget;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Phantom Butterfly S, ported from the 1.12.2 AL_HuanyingdieS slash art.
 * No target: 12 swords. Locked target: 50 swords.
 */
public final class PhantomButterflySArts extends SlashArts {

    private static final int SWORDS_NO_TARGET = 12;
    private static final int SWORDS_WITH_TARGET = 50;
    private static final double LOCK_RANGE = 30.0;
    private static final int WHITE = 0xFFFFFF;
    private static final int LIFETIME = 100;
    private static final float DAMAGE_MULTIPLIER = 7.5F;

    public PhantomButterflySArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        final ServerLevel level = (ServerLevel) user.level();
        final RandomSource rng = level.random;
        final float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier).orElse(4.0F);

        final SaTarget target = resolveTarget(level, user);

        if (target == null) {
            // No target: scatter 12 phantom swords around the user.
            final float damage = (bladeAttack + MathFunc.amplifierCalc(bladeAttack, 4.0F))
                    / 10.0F * DAMAGE_MULTIPLIER;
            for (int i = 0; i < SWORDS_NO_TARGET; i++) {
                float a = rng.nextFloat() * 100.0F;
                float b = rng.nextFloat() * 40.0F;
                float e = rng.nextFloat() * 100.0F;
                double x = user.getX() + (50.0 - a) / 10.0;
                double y = user.getY() + (20.0 - b) / 10.0 + 0.75 + user.getEyeHeight() / 2.0;
                double z = user.getZ() + (50.0 - e) / 10.0;
                float yaw = user.getYRot() + (float) (i * 30);
                int interval = i + 1;

                ButterflySwordEntity sword = ButterflySwordEntity.spawn(
                        level, user, new Vec3(x, y, z),
                        damage, WHITE, LIFETIME, interval, yaw, 0.0F);
                sword.setRoll(0F);
                sword.setLegacyDriveSpeed(1.0E-4F * a * i + 1.0E-4F);
            }
        } else {
            // Locked target: spawn 50 delayed tracking swords around the target.
            final float damage = (bladeAttack + MathFunc.amplifierCalc(bladeAttack, 2.0F))
                    / 50.0F * 3.0F * DAMAGE_MULTIPLIER;
            for (int i = 0; i < SWORDS_WITH_TARGET; i++) {
                float a = rng.nextFloat() * 100.0F;
                float b = rng.nextFloat() * 40.0F;
                float e = rng.nextFloat() * 100.0F;
                double x = target.anchor().x + (50.0 - a) / 3.0;
                double y = target.anchor().y + (20.0 - b) / 10.0 + user.getEyeHeight() / 2.0;
                double z = target.anchor().z + (50.0 - e) / 3.0;
                float yaw = target.hitEntity().getYRot() + (float) (i * 7);
                int interval = i + 1;

                ButterflySwordEntity sword = ButterflySwordEntity.spawn(
                        level, user, new Vec3(x, y, z),
                        damage, WHITE, LIFETIME, interval, yaw, 0.0F);
                sword.setRoll(0F);
                sword.setTargetEntityId(target.hitEntity().getId());
                sword.setLegacyDriveSpeed(1.0E-4F * a * i + 1.0E-4F);
            }
        }

        // Sound and butterfly-like particles.
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        for (int s = 0; s < 12; s++) {
            level.sendParticles(ParticleTypes.END_ROD,
                    user.getX() + (rng.nextDouble() - 0.5) * 4,
                    user.getY() + rng.nextDouble() * 3,
                    user.getZ() + (rng.nextDouble() - 0.5) * 4,
                    1, 0, 0, 0, 0.02);
        }

        return super.doArts(type, user);
    }

    /**
     * Prefer SlashBlade's locked target, then fall back to a forward search.
     */
    private static SaTarget resolveTarget(ServerLevel level, LivingEntity user) {
        Entity locked = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level)).orElse(null);
        return SaTargeting.findTarget(user, locked, LOCK_RANGE).orElse(null);
    }
}

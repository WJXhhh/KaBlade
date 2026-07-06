package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ButterflySwordEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Phantom Butterfly, ported from the 1.12.2 AL_Huanyingdie slash art.
 */
public final class PhantomButterflyArts extends SlashArts {

    private static final int SWORD_COUNT = 50;
    private static final int WHITE = 0xFFFFFF;
    private static final int LIFETIME = 100;
    private static final double LOCK_RANGE = 30.0D;
    private static final float DAMAGE_MULTIPLIER = 7.5F;

    public PhantomButterflyArts(Function<LivingEntity, ResourceLocation> state) {
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
        LivingEntity target = resolveTarget(level, user);

        if (target == null) {
            spawnAroundUser(level, user, bladeAttack);
        } else {
            spawnAroundTarget(level, user, target, bladeAttack);
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);
        return super.doArts(type, user);
    }

    private static void spawnAroundUser(ServerLevel level, LivingEntity user, float bladeAttack) {
        float damage = ((bladeAttack + MathFunc.amplifierCalc(bladeAttack, 4.0F)) / 45.0F
                + bladeAttack * 0.1F) * DAMAGE_MULTIPLIER;

        for (int i = 0; i < SWORD_COUNT; i++) {
            float a = level.random.nextFloat() * 100.0F;
            float b = level.random.nextFloat() * 40.0F;
            float e = level.random.nextFloat() * 100.0F;
            double x = user.getX() + (50.0D - a) / 10.0D;
            double y = user.getY() + (20.0D - b) / 10.0D + 0.75D + user.getEyeHeight() / 2.0D;
            double z = user.getZ() + (50.0D - e) / 10.0D;

            ButterflySwordEntity sword = ButterflySwordEntity.spawn(
                    level, user, new Vec3(x, y, z),
                    damage, WHITE, LIFETIME, i + 1, user.getYRot() + i * 30.0F, 0.0F);
            sword.setRoll(0.0F);
            sword.setLegacyDriveSpeed(1.0E-4F * a * i + 1.0E-4F);
        }

        level.sendParticles(ParticleTypes.END_ROD, user.getX(), user.getEyeY(), user.getZ(),
                32, 2.6D, 1.3D, 2.6D, 0.02D);
    }

    private static void spawnAroundTarget(ServerLevel level, LivingEntity user,
                                          LivingEntity target, float bladeAttack) {
        float damage = ((bladeAttack + MathFunc.amplifierCalc(bladeAttack, 2.0F)) / 50.0F
                + bladeAttack * 0.02F) * DAMAGE_MULTIPLIER;

        for (int i = 0; i < SWORD_COUNT; i++) {
            float a = level.random.nextFloat() * 100.0F;
            float b = level.random.nextFloat() * 40.0F;
            float e = level.random.nextFloat() * 100.0F;
            double x = target.getX() + (50.0D - a) / 3.0D;
            double y = target.getY() + (20.0D - b) / 10.0D + 1.0D + user.getEyeHeight() / 2.0D;
            double z = target.getZ() + (50.0D - e) / 3.0D;

            ButterflySwordEntity sword = ButterflySwordEntity.spawn(
                    level, user, new Vec3(x, y, z),
                    damage, WHITE, LIFETIME, i + 1, target.getYRot() + i * 7.0F, 0.0F);
            sword.setRoll(0.0F);
            sword.setTargetEntityId(target.getId());
            sword.setLegacyDriveSpeed(1.0E-4F * a * i + 1.0E-4F);
        }

        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getEyeY(), target.getZ(),
                48, 2.0D, 1.4D, 2.0D, 0.22D);
    }

    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user) {
        int id = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getTargetEntityId)
                .orElse(0);
        if (id != 0) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && isValidTarget(user, living)) {
                return living;
            }
        }

        Entity watched = SATool.getEntityToWatch(user);
        if (watched instanceof LivingEntity living && isValidTarget(user, living)) {
            return living;
        }
        return null;
    }

    private static boolean isValidTarget(LivingEntity user, LivingEntity target) {
        return SaTargeting.canDamage(user, target)
                && target.distanceToSqr(user) <= LOCK_RANGE * LOCK_RANGE;
    }
}

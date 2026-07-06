package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.PhantomSwordExEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Soul of Frost, ported from the 1.12.2 AL_Xuepo slash art used by Xuezou.
 */
public final class SoulOfFrostArts extends SlashArts {

    private static final double LOCK_RANGE = 30.0D;
    private static final float DAMAGE_RATIO = 5.0F;
    private static final float INITIAL_SPEED = 0.008F;
    private static final float ACCELERATED_SPEED = 1.8F;
    private static final int ACCELERATION_DELAY = 10;
    private static final int SNOW_TRAIL_TICKS = 18;
    private static final int SNOW_BURST_COUNT = 26;

    public SoulOfFrostArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (bladeAttack / 5.0F) + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO);

        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        EntityDrive drive = spawnSoulOfFrostDrive(level, user, eye, look, damage * 3.5F);

        spawnSnowBurst(level, eye);
        scheduleSnowTrail(level, drive, SNOW_TRAIL_TICKS);
        SaFx.schedule(level, ACCELERATION_DELAY, () -> accelerateDrive(drive, look));

        LivingEntity target = resolveTarget(level, user);
        if (target != null) {
            // Old AL_Xuepo applied a managed melee hit before spawning the tracking swords.
            AttackManager.doMeleeAttack(user, target, true, true);
            if (user instanceof Player player) {
                player.crit(target);
            }
            blade.getCapability(ItemSlashBlade.BLADESTATE)
                    .ifPresent(state -> state.setTargetEntityId(target));

            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            target.hurtTime = 0;
            target.invulnerableTime = 0;
            spawnPotionSwords(level, user, target, damage);
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 1.2F);

        return super.doArts(type, user);
    }

    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user) {
        Entity locked = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity living && isValidTarget(user, living)) {
            return living;
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

    private static EntityDrive spawnSoulOfFrostDrive(ServerLevel level, LivingEntity user, Vec3 pos, Vec3 look, double damage) {
        EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
        drive.setPos(pos.x, pos.y, pos.z);
        drive.setShooter(user);
        drive.setDamage(SaFx.driveDamageCoeff(user, damage));
        drive.setColor(0xFFFFFF);
        drive.setBaseSize(5.0F);
        drive.setLifetime(80.0F);
        drive.setRotationRoll(SaFx.VERTICAL_ROLL);
        drive.setSpeed(INITIAL_SPEED);
        drive.shoot(look.x, look.y, look.z, INITIAL_SPEED, 0.0F);
        level.addFreshEntity(drive);
        return drive;
    }

    private static void accelerateDrive(EntityDrive drive, Vec3 look) {
        if (drive == null || !drive.isAlive()) {
            return;
        }
        Vec3 dir = look.normalize().scale(ACCELERATED_SPEED);
        drive.setSpeed(ACCELERATED_SPEED);
        drive.setDeltaMovement(dir);
        drive.hurtMarked = true;
        drive.hasImpulse = true;
    }

    private static void scheduleSnowTrail(ServerLevel level, EntityDrive drive, int remainingTicks) {
        if (remainingTicks <= 0) {
            return;
        }
        SaFx.schedule(level, 1, () -> {
            if (drive != null && drive.isAlive()) {
                Vec3 pos = drive.position();
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        pos.x, pos.y, pos.z, 4, 0.42D, 0.08D, 0.42D, 0.003D);
                scheduleSnowTrail(level, drive, remainingTicks - 1);
            }
        });
    }

    private static void spawnSnowBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.x, pos.y, pos.z, SNOW_BURST_COUNT, 2.8D, 1.2D, 2.8D, 0.025D);
    }

    private static void spawnPotionSwords(ServerLevel level, LivingEntity user, LivingEntity target, float damage) {
        for (int i = 0; i < 3; i++) {
            PhantomSwordExEntity sword = PhantomSwordExEntity.spawn(
                    level,
                    user,
                    new Vec3(user.getX(), user.getY() + (i + 1) * 0.5D, user.getZ()),
                    Vec3.ZERO,
                    damage,
                    0xFFFFFF,
                    30,
                    25,
                    user.getYRot(),
                    0.0F);
            sword.setRoll(0.0F);
            sword.setTargetEntityId(target.getId());
        }
    }
}

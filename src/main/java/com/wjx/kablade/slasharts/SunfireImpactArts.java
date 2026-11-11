package com.wjx.kablade.slasharts;

import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.network.SunfireImpactPacket;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaCombatScaling;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTarget;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Function;

/** A leap followed by a single, owner-attributed impact on landing. */
public final class SunfireImpactArts extends SlashArts {
    private static final double RADIUS = 4.0D;

    public SunfireImpactArts(Function<LivingEntity, ResourceLocation> state) { super(state); }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (!user.level().isClientSide() && type != ArtsType.Fail && user.level() instanceof ServerLevel level) {
            var state = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE);
            Entity locked = state.resolve().map(value -> value.getTargetEntity(user.level())).orElse(null);
            SaTarget target = SaTargeting.findTarget(user, locked, 8.0D).orElse(null);
            float attack = state.map(ISlashBladeState::getBaseAttackModifier).orElse(14.0F);
            // Death Gaze hits at ages 3..24 every three ticks, each for total / 6.5.
            float damage = SaCombatScaling.scaleDamage(user,
                    (16.0F + MathFunc.amplifierCalc(attack, 4.5F)) * 1.5F * (8.0F / 6.5F));
            Vec3 direction = target == null ? Vec3.ZERO : target.anchor().subtract(user.position()).multiply(1, 0, 1);
            if (direction.lengthSqr() > 1.0e-6) direction = direction.normalize().scale(0.8D);
            user.setDeltaMovement(direction.x, 0.95D, direction.z);
            user.hasImpulse = true;
            user.hurtMarked = true;
            user.fallDistance = 0;
            level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            awaitLanding(level, user, damage, 0, false);
        }
        return super.doArts(type, user);
    }

    private static void awaitLanding(ServerLevel level, LivingEntity owner, float damage, int age, boolean airborne) {
        SaFx.schedule(level, 1, owner, () -> {
            if (!owner.isAlive() || owner.isRemoved() || owner.level() != level || age >= 100) return;
            boolean inAir = airborne || !owner.onGround();
            owner.fallDistance = 0;
            if (inAir && owner.onGround()) {
                impact(level, owner, damage);
            } else {
                awaitLanding(level, owner, damage, age + 1, inAir);
            }
        });
    }

    private static void impact(ServerLevel level, LivingEntity owner, float damage) {
        Vec3 center = owner.position();
        KabladeNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                center.x, center.y, center.z, 64, level.dimension())),
                new SunfireImpactPacket(center.x, center.y, center.z));
        for (SaTarget target : SaTargeting.uniqueTargets(level, owner, new AABB(center, center).inflate(RADIUS), true)) {
            if (target.distanceToSqr(center) <= RADIUS * RADIUS) {
                SaDamage.hurtSlashArtNoIFrame(target.hitEntity(), level, owner, damage);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.35, center.z, 180, 2.0, 0.55, 2.0, 0.12);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z, 45, 1.6, 0.2, 1.6, 0.05);
        level.playSound(null, owner.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.1F);
        level.playSound(null, owner.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
    }
}

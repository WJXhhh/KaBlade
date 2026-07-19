package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** Visual and multi-hit anchor for Domain of Unity's Conceptual Metaphor SA. */
public final class ConceptualMetaphorEntity extends SwordEnlightenmentEntity {

    private static final int UNITY_FINISH_TICK = 36;
    private static final float UNITY_DAMAGE_MULTIPLIER = 1.20F;
    private static final double HIT_RADIUS = 8.2D;
    private static final double HIT_HEIGHT = 4.8D;
    private static final Vector3f IVORY_GOLD = new Vector3f(0.95F, 0.86F, 0.63F);
    private static final Vector3f PALE_LAVENDER = new Vector3f(0.74F, 0.76F, 1.0F);

    public ConceptualMetaphorEntity(EntityType<? extends ConceptualMetaphorEntity> type, Level level) {
        super(type, level);
    }

    public static ConceptualMetaphorEntity spawn(ServerLevel level, LivingEntity owner,
                                                  float baseDamage, Vec3 attackPosition) {
        ConceptualMetaphorEntity entity =
                new ConceptualMetaphorEntity(ModEntities.CONCEPTUAL_METAPHOR.get(), level);
        entity.initialize(owner, baseDamage, attackPosition);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void tickVariant(ServerLevel level, LivingEntity source) {
        spawnUnityParticles(level);

        if (this.tickCount == 30) {
            level.playSound(null, this.getX(), this.getY() + 1.15D, this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.72F, 2.0F);
            level.playSound(null, this.getX(), this.getY() + 1.15D, this.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.48F, 1.72F);
        }

        if (this.tickCount == UNITY_FINISH_TICK) {
            applyUnityFinisher(level, source);
            playUnityFinisherSounds(level);
        }
    }

    private void spawnUnityParticles(ServerLevel level) {
        if (this.tickCount >= 8 && this.tickCount <= UNITY_FINISH_TICK && (this.tickCount & 1) == 0) {
            float gather = Mth.clamp((this.tickCount - 30.0F) / 6.0F, 0.0F, 1.0F);
            double radius = Mth.lerp(gather * gather, 4.25D, 0.42D);
            double spin = this.tickCount * (0.24D + gather * 0.20D);
            Vec3 center = unityCenter();
            for (int i = 0; i < 3; i++) {
                double angle = spin + i * Math.PI * 2.0D / 3.0D;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                double y = center.y + Math.sin(angle * 2.0D) * 0.22D;
                Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle))
                        .scale(0.08D + gather * 0.12D);
                level.sendParticles(new DustColorTransitionOptions(IVORY_GOLD, PALE_LAVENDER,
                                1.18F + gather * 0.52F),
                        x, y, z, 2, tangent.x, 0.025D, tangent.z, 0.035D);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z,
                        1, tangent.x, 0.015D, tangent.z, 0.02D);
            }
        }

        if (this.tickCount == UNITY_FINISH_TICK) {
            Vec3 center = unityCenter();
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(new DustColorTransitionOptions(IVORY_GOLD, PALE_LAVENDER, 1.85F),
                    center.x, center.y, center.z,
                    72, 0.92D, 0.70D, 0.92D, 0.16D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    46, 0.78D, 0.58D, 0.78D, 0.20D);
        }
    }

    private void applyUnityFinisher(ServerLevel level, LivingEntity source) {
        AABB area = new AABB(
                this.getX() - HIT_RADIUS, this.getY() - 0.65D, this.getZ() - HIT_RADIUS,
                this.getX() + HIT_RADIUS, this.getY() + HIT_HEIGHT, this.getZ() + HIT_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isPickable() && SaTargeting.canDamageAttackable(source, target));
        Vec3 pullCenter = unityCenter();

        for (LivingEntity target : targets) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
            Vec3 relative = center.subtract(this.position().add(0.0D, 1.0D, 0.0D));
            double horizontal = Math.sqrt(relative.x * relative.x + relative.z * relative.z);
            if (horizontal > HIT_RADIUS || Math.abs(relative.y) > HIT_HEIGHT) {
                continue;
            }

            float falloff = (float) Mth.clamp(
                    1.0D - horizontal / (HIT_RADIUS * 1.35D), 0.62D, 1.0D);
            float damage = this.getBaseDamage() * UNITY_DAMAGE_MULTIPLIER * falloff;
            if (!SaDamage.hurtSlashArtNoIFrame(target, level, this, source, damage)) {
                continue;
            }

            target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(), 80, 2));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    38, 3, false, false));

            Vec3 pull = pullCenter.subtract(center);
            if (pull.lengthSqr() > 1.0E-6D) {
                pull = pull.normalize().scale(0.28D);
            }
            target.setDeltaMovement(target.getDeltaMovement().scale(0.35D)
                    .add(pull)
                    .add(0.0D, 0.18D, 0.0D));
            target.hurtMarked = true;
            spawnUnityHitFeedback(level, target);
        }
    }

    private void spawnUnityHitFeedback(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                22, 0.38D, 0.30D, 0.38D, 0.24D);
        level.sendParticles(new DustColorTransitionOptions(IVORY_GOLD, PALE_LAVENDER, 1.62F),
                center.x, center.y, center.z,
                30, 0.38D, 0.32D, 0.38D, 0.13D);
    }

    private void playUnityFinisherSounds(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.74F, 1.82F);
        level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.66F, 1.54F);
        level.playSound(null, this.getX(), this.getY() + 1.2D, this.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.82F, 1.96F);
    }

    private Vec3 unityCenter() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        return this.position().add(forward.scale(2.15D)).add(0.0D, 1.1D, 0.0D);
    }
}

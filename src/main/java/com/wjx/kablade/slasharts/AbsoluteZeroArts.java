package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 绝对零度 —— 等离子影秀专属 SA。
 * 从 1.12.2 {@code HonkaiAbsoluteZero} 移植并简化而来。
 * <p>
 * 射线锁定玩家前方 8 格内最近的生物，造成伤害并施加缓慢 VII（替代 1.12.2 自定义冻结药水），
 * 同时在玩家周围喷发烟雾粒子。
 */
public final class AbsoluteZeroArts extends SlashArts {

    private static final double RAY_DISTANCE = 8.0;
    private static final float BASE_DAMAGE = 20.0F;
    /** 攻击力补正系数（1.12.2 伤害公式系数 5），amplifierCalc 对数补正。 */
    private static final float ATTACK_FACTOR = 5.0F;
    /** 缓慢时长（tick），1.12.2 原版冻结 140 tick。 */
    private static final int SLOW_DURATION = 140;
    private static final int SLOW_AMPLIFIER = 6;

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
            float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                    .map(ISlashBladeState::getBaseAttackModifier)
                    .orElse(4.0F);
            float damage = BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

            DamageSource src = user instanceof Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(user);
            target.hurt(src, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, SLOW_AMPLIFIER));
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        // 烟雾粒子
        Vec3 pos = user.position();
        for (int i = 0; i < 30; i++) {
            double ox = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble() * 3.0;
            double oz = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble() * 3.0;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x + ox, pos.y + level.random.nextDouble(), pos.z + oz,
                    1, 0.0, 0.1, 0.0, 0.0);
        }
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

        return super.doArts(type, user);
    }

    /** 射线扫描前方最近生物。 */
    private static LivingEntity raycastTarget(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, e -> e != user && e.isAlive() && e.isPickable());

        LivingEntity closest = null;
        double closestDist = RAY_DISTANCE;

        for (LivingEntity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hit = bb.clip(eye, end);
            if (bb.contains(eye)) {
                return candidate;
            } else if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < closestDist) {
                    closest = candidate;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }
}

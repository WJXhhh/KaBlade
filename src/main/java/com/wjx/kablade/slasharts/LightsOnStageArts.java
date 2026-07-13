package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.StageLightEntity;
import com.wjx.kablade.util.MathFunc;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 鑱氬厜鑸炲彴锛圠ights on Stage锛夆€斺€旈摱娌宠拷鍏変笓灞?SA銆? * <p>
 * 璧锋墜鏃嬫柀鍚庡湪鑴氫笅灞曞紑閲戠櫧鑹茶垶鍙板厜鐜紱绗?5 tick 瀵硅垶鍙拌寖鍥村唴鐨勯潪鍙嬫柟鐩爣缁撶畻涓€娆? * 鐜舰鏂╁嚮锛屽厜鐜殢鍚庝綔涓烘紨鍑烘畫鐣欏苟缂撴參娣″嚭銆? */
public final class LightsOnStageArts extends SlashArts {

    private static final int STAGE_LIFETIME = 80;
    private static final int HIT_DELAY = 5;
    private static final double RANGE = 6.25;
    private static final double VERTICAL_RANGE = 3.0;
    private static final float BASE_DAMAGE = 6.0F;
    private static final float ATTACK_FACTOR = 1.15F;

    public LightsOnStageArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        Vec3 origin = user.position();
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float timingBonus = type == ArtsType.Jackpot || type == ArtsType.Super ? 1.25F : 1.0F;
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * timingBonus;

        StageLightEntity.spawn(level, origin.x, origin.y + 0.06, origin.z,
                user.getYRot(), STAGE_LIFETIME);
        openingSparkles(level, origin);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.15F, 1.75F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.1F);

        SaFx.schedule(level, HIT_DELAY, () -> strikeStage(level, user, origin, damage));
        return super.doArts(type, user);
    }

    private static void strikeStage(ServerLevel level, LivingEntity user, Vec3 origin, float damage) {
        if (!user.isAlive() || user.level() != level) {
            return;
        }

        AABB bounds = AABB.ofSize(origin.add(0.0, 1.0, 0.0),
                RANGE * 2.0, VERTICAL_RANGE * 2.0, RANGE * 2.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds, target -> {
            if (!SaTargeting.canDamageAttackable(user, target)) {
                return false;
            }
            double dx = target.getX() - origin.x;
            double dz = target.getZ() - origin.z;
            return dx * dx + dz * dz <= RANGE * RANGE;
        });

        DamageSource source = level.damageSources().mobAttack(user);
        for (LivingEntity target : targets) {
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, user, damage);
            target.knockback(0.55,
                    origin.x - target.getX(), origin.z - target.getZ());
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
                    12, target.getBbWidth() * 0.45, target.getBbHeight() * 0.35,
                    target.getBbWidth() * 0.45, 0.08);
        }

        for (int i = 0; i < 42; i++) {
            double angle = Math.PI * 2.0 * i / 42.0;
            double radius = RANGE * (0.92 + level.random.nextDouble() * 0.08);
            level.sendParticles(ParticleTypes.END_ROD,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + 0.16 + level.random.nextDouble() * 0.3,
                    origin.z + Math.sin(angle) * radius,
                    1, 0.02, 0.06, 0.02, 0.0);
        }
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.45F, 1.35F);
    }

    private static void openingSparkles(ServerLevel level, Vec3 origin) {
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0 * i / 18.0;
            double radius = 0.9 + i * 0.12;
            level.sendParticles(ParticleTypes.END_ROD,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + 0.12,
                    origin.z + Math.sin(angle) * radius,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}

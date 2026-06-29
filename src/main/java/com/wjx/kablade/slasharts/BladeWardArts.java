package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.RaikiriShieldEntity;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 刃盾 —— 「雷切」专属 SA。
 * <p>
 * 从 1.12.2 {@code HonkaiBladeWard} 移植而来：
 * 先清除持有者周围已有的雷切护盾，再召唤一个新的护盾实体。
 */
public final class BladeWardArts extends SlashArts {

    public BladeWardArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        // 移除该玩家已有的雷切护盾，避免叠加（同 1.12.2 逻辑：只清自己的护盾）
        List<RaikiriShieldEntity> existing = ((ServerLevel) user.level())
                .getEntitiesOfClass(RaikiriShieldEntity.class,
                        user.getBoundingBox().inflate(32.0),
                        e -> e.isAlive() && e.getThrower() == user);
        for (RaikiriShieldEntity e : existing) {
            e.discard();
        }

        // 召唤新护盾
        RaikiriShieldEntity.spawn((ServerLevel) user.level(), user);

        // 激活反馈：音效 + 粒子
        ServerLevel level = (ServerLevel) user.level();
        Vec3 pos = user.position();
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.8F);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.5F);

        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2.0 * i / 24.0;
            double r = 1.2;
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.x + Math.cos(a) * r, pos.y + 0.3 + Math.sin(a * 2) * 0.4, pos.z + Math.sin(a) * r,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
        for (int i = 0; i < 12; i++) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x + (level.random.nextDouble() - 0.5) * 2.5,
                    pos.y + level.random.nextDouble() * 2.0,
                    pos.z + (level.random.nextDouble() - 0.5) * 2.5,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        return super.doArts(type, user);
    }
}

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 月牙天冲 —— 白「天锁斩月」专属 SA（复刻 1.12.2 {@code AL_Yueyatianchong}）。
 * 朝面向推出五道由小及大的皓白月牙波，层层叠进、撕裂前方。
 */
public final class MoonFangArts extends SlashArts {

    private static final int WAVES = 5;
    private static final float DAMAGE_BASE = 1.0F;
    private static final float DAMAGE_RATIO = 4.0F;
    private static final int WHITE = 0xFFFFFF;

    public MoonFangArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        final ServerLevel level = (ServerLevel) user.level();
        final float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier).orElse(4.0F);
        final float damage = DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO);

        final Vec3 eye = user.getEyePosition(1.0F);
        final Vec3 look = user.getLookAngle();

        // 五道月牙：越往后越大、越快，逐层推进
        for (int i = 0; i < WAVES; i++) {
            float size = 0.8F + 0.5F * i * i; // 由小及大
            float speed = 0.55F + 0.15F * i;
            int delay = i * 2;
            final int idx = i;
            SaFx.schedule(level, delay, () -> {
                if (!user.isAlive()) return;
                Vec3 e = user.getEyePosition(1.0F);
                Vec3 l = user.getLookAngle();
                SaFx.drive(level, user, e, l, speed, damage, WHITE, size, 40.0F, SaFx.VERTICAL_ROLL);
                for (int s = 0; s < 14; s++) {
                    double d = s * 0.7;
                    level.sendParticles(ParticleTypes.END_ROD,
                            e.x + l.x * d, e.y + l.y * d, e.z + l.z * d, 1, 0.15, 0.15, 0.15, 0.0);
                }
                level.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.3F, 0.8F + idx * 0.08F);
            });
        }
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 0.9F);

        return super.doArts(type, user);
    }
}

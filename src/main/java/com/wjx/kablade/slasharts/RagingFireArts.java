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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 焰击 —— 炎王「流刃若火」专属 SA（复刻 1.12.2 {@code AL_YanjiFZ}）。
 * 朝玩家面向喷出一道贯穿式的大型赤焰飞刃，沿途引燃，火舌翻腾。
 */
public final class RagingFireArts extends SlashArts {

    private static final float DAMAGE_BASE = 8.0F;
    private static final float DAMAGE_RATIO = 6.0F;

    public RagingFireArts(Function<LivingEntity, ResourceLocation> state) {
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
        final float damage = (DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO)) * 3.0F;

        final Vec3 eye = user.getEyePosition(1.0F);
        final Vec3 look = user.getLookAngle();

        // 单独一道巨型赤焰竖斩贯穿向前（1.12.2 AL_YanjiFZ 只放 1 道、scale 40 超大；roll=90 使刃面竖立）
        SaFx.driveWithFinalDamage(level, user, eye, look, 1.3F, damage, 0xFF2A00, 5.0F, 50.0F, SaFx.VERTICAL_ROLL);

        // 火焰演出：沿视线倾泻火舌
        for (int s = 0; s < 24; s++) {
            double d = s * 0.6;
            double x = eye.x + look.x * d, y = eye.y + look.y * d, z = eye.z + look.z * d;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 3, 0.25, 0.25, 0.25, 0.02);
            if (s % 3 == 0) {
                level.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.6F, 0.7F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.8F);

        return super.doArts(type, user);
    }
}

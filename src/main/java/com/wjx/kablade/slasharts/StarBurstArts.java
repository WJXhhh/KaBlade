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
 * 星爆气流斩 —— 夜空之剑「阐释者」专属 SA（复刻 1.12.2 {@code AL_Xingbao}）。
 * 在身周拔起一column赤红飞刃，朝玩家正前方激射而出。
 * <p>1.12.2 行为：21 道飞刃<b>同朝向（玩家偏航、俯仰=0，水平前射）、滚转角随机</b>，
 * 散布在身周/头顶的列阵中——随机的是<b>滚转角与生成位置</b>，不是飞行方向。
 */
public final class StarBurstArts extends SlashArts {

    private static final int DRIVES = 21;
    private static final float DAMAGE_BASE = 1.0F;
    private static final float DAMAGE_RATIO = 2.0F;

    public StarBurstArts(Function<LivingEntity, ResourceLocation> state) {
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
        final float damage = DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO);

        // 水平前射方向（俯仰=0），对应 1.12.2 setLocationAndAngles(rotationYaw, 0.0f)
        final Vec3 flat = SaFx.flatLook(user);
        final double eyeY = user.getEyeHeight();

        for (int i = 0; i < DRIVES; i++) {
            // 生成位置：眼高附近一团紧凑簇（不再像 1.12.2 那样拉到头顶 0..13 格，避免飞刃在 Y 轴上拉成横条）
            double ox = (rng.nextDouble() - 0.5) * 2.4;
            double oy = eyeY + (rng.nextDouble() - 0.5) * 1.2;
            double oz = (rng.nextDouble() - 0.5) * 2.4;
            Vec3 pos = user.position().add(ox, oy, oz);
            // 随机滚转角（星爆的散乱观感来自滚转角随机，而非方向），统一水平前射
            float roll = rng.nextFloat() * 360.0F;
            float speed = 1.4F + rng.nextFloat() * 0.3F;
            SaFx.drive(level, user, pos, flat, speed, damage, 0xFF1010, 2.0F, 90.0F, roll);
        }

        Vec3 center = user.position().add(0.0, user.getBbHeight() * 0.6, 0.0);
        for (int i = 0; i < 40; i++) {
            level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 1,
                    (rng.nextDouble() - 0.5), (rng.nextDouble() - 0.5), (rng.nextDouble() - 0.5), 0.4);
            level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 1,
                    (rng.nextDouble() - 0.5) * 2, (rng.nextDouble() - 0.5) * 2, (rng.nextDouble() - 0.5) * 2, 0.6);
        }
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.6F, 0.9F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.3F, 0.7F);

        return super.doArts(type, user);
    }
}

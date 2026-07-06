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
 * 虹极斩 —— 光剑「监视者」专属 SA（复刻 1.12.2 {@code AL_Qicai}「七彩斩」）。
 * 朝正前方泼出七道同向飞刃，颜色依虹序、速度递减，层层拖尾如长虹贯日。
 * <p>1.12.2 行为：7 道飞刃<b>同方向（玩家朝向）、同滚转角（Battou.swingDirection=0）</b>，
 * 仅颜色与初速不同——并非扇形散开。
 */
public final class RainbowSlashArts extends SlashArts {

    private static final int[] COLORS = {
            0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x4D4DFF, 0x9932CD
    };
    private static final float DAMAGE_BASE = 1.0F;
    private static final float DAMAGE_RATIO = 4.0F;

    public RainbowSlashArts(Function<LivingEntity, ResourceLocation> state) {
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

        // 出生点：眼前一格（贴合 1.12.2 EntityDriveAdd 默认定位），统一朝向玩家视线
        final Vec3 look = user.getLookAngle();
        final Vec3 pos = user.getEyePosition(1.0F).add(look);

        for (int i = 0; i < COLORS.length; i++) {
            // 同方向、同滚转角(竖直)，仅颜色与速度不同 —— 红在前、紫在后，沿同一直线拉开拖尾。
            // 速度用正常飞刃量级（重锋无 1.12.2 的初速→加速机制），前快后慢保留虹序拖尾。
            float speed = 1.7F - i * 0.13F;
            SaFx.driveWithFinalDamage(level, user, pos, look, speed, damage, COLORS[i], 1.6F, 80.0F, SaFx.VERTICAL_ROLL);
        }

        for (int s = 0; s < 16; s++) {
            double d = s * 0.6;
            level.sendParticles(ParticleTypes.ENCHANT,
                    pos.x + look.x * d, pos.y + look.y * d, pos.z + look.z * d,
                    2, 0.3, 0.3, 0.3, 0.5);
        }
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.2F);

        return super.doArts(type, user);
    }
}

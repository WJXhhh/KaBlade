package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.LacerateDriveEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 撕裂灵刃 —— 妖刀村正专属 SA。
 * 从 1.12.2 {@code HonkaiLacerateBlade} 移植而来。
 * <p>
 * 实体行为（AABB 命中、可破坏物销毁、粒子、速度曲线）详见 {@link LacerateDriveEntity}。
 * 此处负责伤害计算 + 随机偏移 + 凋零 II 施加。
 */
public final class LacerateBladeArts extends SlashArts {

    /** 基础伤害（1.12.2 基础值 10；额外伤害 = amplifierCalc(attack, 3)，对数补正）。 */
    private static final float BASE_DAMAGE = 10.0F;
    private static final float ATTACK_FACTOR = 3.0F;
    /** 凋零持续时间（tick）。 */
    private static final int WITHER_DURATION = 100;
    /** 凋零等级（0-based，即凋零 II）。 */
    private static final int WITHER_AMPLIFIER = 1;

    public LacerateBladeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        final ServerLevel level = (ServerLevel) user.level();
        final ItemStack blade = user.getMainHandItem();
        final float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        float totalDamage = BASE_DAMAGE + extraDamage;

        // 玩家朝向 = 飞行方向（匹配 1.12.2 setDriveVector + setInitialSpeed 行为）
        Vec3 lookDir = user.getLookAngle();
        double speed = 0.101F;
        Vec3 dir = lookDir.scale(speed);

        // 生成位置：玩家眼睛高度前方 1 格（匹配 1.12.2 setInitialSpeed 最终定位）
        double px = user.getX() + lookDir.x;
        double py = user.getY() + user.getEyeHeight() / 2.0;
        double pz = user.getZ() + lookDir.z;

        // 随机视觉翻滚角（1.12.2 的 a 值只用于 roll，不用于方向）
        float roll = level.getRandom().nextFloat() * 360.0F;

        // 召唤撕裂灵刃驱动实体（1.12.2 EntityDriveAdd 全套参数）
        LacerateDriveEntity.spawn(level, user,
                new Vec3(px, py, pz), dir, totalDamage, roll);

        // 对正在攻击玩家的敌人施加凋零 II
        LivingEntity attacker = user.getLastHurtByMob();
        if (attacker != null && SaTargeting.canDamage(user, attacker)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, WITHER_AMPLIFIER, false, false));
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);

        return super.doArts(type, user);
    }
}

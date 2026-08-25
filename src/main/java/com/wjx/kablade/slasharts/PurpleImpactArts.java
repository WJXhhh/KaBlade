package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.PurpleImpactDriveEntity;
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
 * 深紫冲击 (Purple Impact) —— 深紫骑士专属 SA。
 * <p>
 * 基于撕裂灵刃移植，释放深紫色幻影刃驱动实体。
 */
public final class PurpleImpactArts extends SlashArts {

    private static final float BASE_DAMAGE = 10.0F;
    private static final float ATTACK_FACTOR = 3.0F;
    private static final int WITHER_DURATION = 100;
    private static final int WITHER_AMPLIFIER = 1;

    public PurpleImpactArts(Function<LivingEntity, ResourceLocation> state) {
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

        Vec3 lookDir = user.getLookAngle();
        double speed = 0.101F;
        Vec3 dir = lookDir.scale(speed);

        double px = user.getX() + lookDir.x;
        double py = user.getY() + user.getEyeHeight() / 2.0;
        double pz = user.getZ() + lookDir.z;

        float roll = level.getRandom().nextFloat() * 360.0F;

        // 召唤深紫冲击驱动实体
        PurpleImpactDriveEntity.spawn(level, user,
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

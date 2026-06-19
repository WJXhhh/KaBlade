package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.CutMetalRingEntity;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 斩铁断金 —— 复合刃「斩铁」专属 SA。
 * 从 1.12.2 {@code SaCutMetal} 移植而来。
 * <p>
 * 自身获得力量 II 3 秒，对周围大范围敌人造成高额伤害（基础 8 + 刀攻击力补正），
 * 并追加目标护甲值 50% 的额外伤害。被击中的敌人爆出伤害指示粒子。
 */
public final class CutMetalArts extends SlashArts {

    /** 基础伤害（不含攻击力补正）。 */
    private static final float BASE_DAMAGE = 8.0F;
    /** 攻击力补正系数：extraDamage = amplifierCalc(bladeAttack, 8)。 */
    private static final float ATTACK_FACTOR = 8.0F;
    /** 护甲追加伤害倍率。 */
    private static final float ARMOR_RATIO = 0.5F;
    /** 力量效果持续时间（tick）。 */
    private static final int STRENGTH_DURATION = 60;
    /** AABB 扩展范围。 */
    private static final double RANGE_XZ = 8.0;
    private static final double RANGE_Y = 4.0;
    private static final int RING_WHITE = 0xF7FBFF;
    private static final int RING_BLUE_WHITE = 0xDDEEFF;

    public CutMetalArts(Function<LivingEntity, ResourceLocation> state) {
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

        // 力量 II 3 秒
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION, 1, false, false));
        spawnBladeLight(level, user);

        // 扫描周围敌人
        AABB bb = user.getBoundingBox().inflate(RANGE_XZ, RANGE_Y, RANGE_XZ)
                .move(user.getDeltaMovement());
        DamageSource src = user.level().damageSources().playerAttack((Player) user);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bb,
                e -> e != user && e.isAlive());

        float extraDamage = amplifierCalc(bladeAttack, ATTACK_FACTOR);

        for (LivingEntity target : targets) {
            // 暴击特效
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            target.hurt(src, BASE_DAMAGE + extraDamage);
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));

            // 护甲追加伤害
            double armor = target.getAttribute(Attributes.ARMOR).getValue();
            if (armor > 0) {
                target.hurt(src, (float) (armor * ARMOR_RATIO));
                blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
            }

            // 伤害指示粒子
            for (int i = 0; i < 20; i++) {
                double px = target.getX() + (level.random.nextDouble() - 0.5) * 4;
                double py = target.getY() + level.random.nextDouble() * target.getBbHeight();
                double pz = target.getZ() + (level.random.nextDouble() - 0.5) * 4;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, px, py, pz, 1, 0, 0, 0, 0);
            }
        }

        // 音效
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);

        return super.doArts(type, user);
    }

    /** 白色环形刀光：主环 + 两道错峰余辉，贴近拔刀剑重锋的干净银白刀痕。 */
    private static void spawnBladeLight(ServerLevel level, LivingEntity user) {
        Vec3 center = user.position();
        double y = user.getY() + user.getBbHeight() * 0.58;
        float yaw = user.getYRot();

        CutMetalRingEntity.spawn(level, center.x, y, center.z, yaw, 1.16F, 18, RING_WHITE);
        CutMetalRingEntity.spawn(level, center.x, y + 0.10, center.z, yaw + 32.0F, 0.92F, 15, RING_BLUE_WHITE);
        CutMetalRingEntity.spawn(level, center.x, y - 0.08, center.z, yaw - 26.0F, 0.74F, 13, RING_WHITE);

        for (int i = 0; i < 36; i++) {
            double a = Math.PI * 2.0 * i / 36.0;
            double r = 2.45 + (i % 3) * 0.22;
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x + Math.cos(a) * r, y, center.z + Math.sin(a) * r,
                    1, 0.015, 0.015, 0.015, 0.0);
        }
    }

    /**
     * 攻击力补正计算（复刻 1.12.2 的 MathFunc.amplifierCalc）。
     * baseAttack 越高，extra 越大，但受 factor 封顶。
     */
    private static float amplifierCalc(float baseAttack, float factor) {
        return Math.min(baseAttack, factor);
    }
}

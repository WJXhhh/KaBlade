package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.DraconicVortexRingEntity;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 魔龙旋斩 —— 大剑「魔龙之脊」专属 SA。
 * 复制自「斩铁断金」，后续在此基础上继续定制。
 */
public final class DraconicVortexArts extends SlashArts {

    /** 基础伤害（不含攻击力补正）。 */
    private static final float BASE_DAMAGE = 28.0F;
    /** 攻击力补正系数：extraDamage = amplifierCalc(bladeAttack, 8)。 */
    private static final float ATTACK_FACTOR = 8.0F;
    /** 护甲追加伤害倍率。 */
    private static final float ARMOR_RATIO = 0.5F;
    /** 力量效果持续时间（tick）。 */
    private static final int STRENGTH_DURATION = 60;
    /** AABB 扩展范围。 */
    private static final double RANGE_XZ = 8.0;
    private static final double RANGE_Y = 4.0;
    private static final int RING_MAIN = 0xFF2838;
    private static final int RING_LIGHT = 0xFF6678;
    private static final int RING_DARK = 0x900010;

    public DraconicVortexArts(Function<LivingEntity, ResourceLocation> state) {
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

        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION, 1, false, false));
        spawnBladeLight(level, user);
        AABB bb = user.getBoundingBox().inflate(RANGE_XZ, RANGE_Y, RANGE_XZ)
                .move(user.getDeltaMovement());
        DamageSource src = user.level().damageSources().playerAttack((Player) user);
        var targets = SaTargeting.uniqueTargets(level, user, bb);

        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        for (var selected : targets) {
            LivingEntity target = selected.root();
            // 暴击特效
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(
                    selected.hitEntity(), level, user, BASE_DAMAGE + extraDamage);
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));

            // 护甲追加伤害
            double armor = target.getAttribute(Attributes.ARMOR).getValue();
            if (armor > 0) {
                com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(
                        selected.hitEntity(), level, user, (float) (armor * ARMOR_RATIO));
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

    /** 魔龙旋刃光效：主旋刃 + 两道错峰环形余辉与龙焰灵气粒子。 */
    private static void spawnBladeLight(ServerLevel level, LivingEntity user) {
        Vec3 center = user.position();
        double y = user.getY() + user.getBbHeight() * 0.58;
        float yaw = user.getYRot();

        DraconicVortexRingEntity.spawn(level, center.x, y, center.z, yaw, 1.22F, 20, RING_MAIN);
        DraconicVortexRingEntity.spawn(level, center.x, y + 0.12, center.z, yaw + 32.0F, 1.00F, 16, RING_LIGHT);
        DraconicVortexRingEntity.spawn(level, center.x, y - 0.10, center.z, yaw - 26.0F, 0.80F, 14, RING_DARK);

        for (int i = 0; i < 36; i++) {
            double a = Math.PI * 2.0 * i / 36.0;
            double r = 2.60 + (i % 3) * 0.24;
            double px = center.x + Math.cos(a) * r;
            double pz = center.z + Math.sin(a) * r;
            level.sendParticles(ParticleTypes.FLAME, px, y, pz, 1, 0.02, 0.02, 0.02, 0.01);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, px, y + (i % 2 == 0 ? 0.12 : -0.10), pz, 1, 0.015, 0.015, 0.015, 0.0);
        }
    }
}

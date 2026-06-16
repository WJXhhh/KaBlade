package com.wjx.kablade.slasharts;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 弧光破晓 —— 弧光刃「流芒」专属 SA（在 1.12.2 {@code SaBreakTheDawn} 基础上重做为<b>向前突进式</b>大招）。
 * <p>
 * 「以一刀划破黑夜，引来破晓」，全程朝玩家面向方向劈出：致盲正前方的敌人 → 横扫一道金色弧光 →
 * 向前贯出一道晨曦光枪 → 最后一记裂空「破晓斩」劈穿前方、金光遍野。金色光辉（END_ROD 光束 + 琥珀金尘）。
 * <ul>
 *   <li>t=0：自身夜视；前方敌人致盲 + 发光；脚下两道金环 + 指向前方的晨曦蓄力光束 + 6 把环身金刃</li>
 *   <li>t=5：9 道金色飞斩横扫成一道前向弧光（±45°）</li>
 *   <li>t=10：向前贯出晨曦光枪（END_ROD 光束 + 紧束金刃齐射）</li>
 *   <li>t=15：向前裂空「破晓斩」+ 金色爆闪 + 前方扇区神圣伤害</li>
 * </ul>
 */
public final class BreakTheDawnArts extends SlashArts {

    private static final int GUARD_SWORDS = 6;
    private static final int ARC_DRIVES = 9;
    private static final int LANCE_DRIVES = 8;

    private static final float BASE_DRIVE_DAMAGE = 0.75F;
    private static final float BASE_SWORD_DAMAGE = 1.25F;
    private static final float ATTACK_RATIO = 0.5F;
    private static final float FINALE_BASE_DAMAGE = 1.75F;
    private static final float FINALE_ATTACK_RATIO = 0.4F;

    private static final double FORWARD_RANGE = 14.0;
    private static final int GLOW_DURATION = 2400;
    private static final int NIGHT_VISION_DURATION = 1600;
    private static final int BLIND_DURATION = 60;

    /** 金色调（弧光 / 晨曦）。偏暖的琥珀金，跟白色 END_ROD/FLASH 光束拉开对比，避免 climax 糊成一坨白。 */
    private static final int GOLD = 0xFFC83C;
    private static final int PALE_GOLD = 0xFFE07A;

    public BreakTheDawnArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    private static DustParticleOptions goldDust(float scale) {
        return new DustParticleOptions(SaFx.rgb(GOLD), scale);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        final ServerLevel level = (ServerLevel) user.level();
        final RandomSource rng = level.random;
        final ItemStack stack = user.getMainHandItem();
        final float bladeAttack = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        final float driveDamage = BASE_DRIVE_DAMAGE + bladeAttack * ATTACK_RATIO;
        final float swordDamage = BASE_SWORD_DAMAGE + bladeAttack * ATTACK_RATIO;
        final float finaleDamage = FINALE_BASE_DAMAGE + bladeAttack * FINALE_ATTACK_RATIO;

        // ── t=0：破晓（向前蓄势）──
        if (user instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false));
        }
        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_DURATION, 0));  // 刺目晨光
        }
        dawnCharge(level, user);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.4F);

        // 环身金刃（自动索敌，朝前方目标补刀）
        for (int i = 0; i < GUARD_SWORDS; i++) {
            EntityBlisteringSwords sword = new EntityBlisteringSwords(
                    SlashBlade.RegistryEvents.BlisteringSwords, level);
            sword.setPos(user.getX(), user.getY() + 1.2, user.getZ());
            sword.setShooter(user);
            sword.setDamage(swordDamage);
            sword.setColor(PALE_GOLD);
            sword.setDelay(i * 3);
            level.addFreshEntity(sword);
            sword.startRiding(user, true);
        }

        // ── t=5：前向弧光横扫 ──
        SaFx.schedule(level, 5, () -> {
            if (!user.isAlive()) return;
            arcSweep(level, user, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.5F);
        });

        // ── t=10：向前贯出晨曦光枪 ──
        SaFx.schedule(level, 10, () -> {
            if (!user.isAlive()) return;
            dawnLance(level, user, rng, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.6F);
        });

        // ── t=15：向前裂空破晓斩 + 前方引爆 ──
        SaFx.schedule(level, 15, () -> {
            if (!user.isAlive()) return;
            daybreakFinale(level, user, finaleDamage);
        });

        return super.doArts(type, user);
    }

    /** 蓄力：脚下两道金环 + 指向前方的晨曦光束。 */
    private static void dawnCharge(ServerLevel level, LivingEntity user) {
        Vec3 center = user.position();
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48.0;
            for (double r = 2.0; r <= 3.6; r += 1.6) {
                level.sendParticles(goldDust(1.3F),
                        center.x + Math.cos(a) * r, center.y + 0.1, center.z + Math.sin(a) * r,
                        1, 0.0, 0.02, 0.0, 0.0);
            }
        }
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 flat = SaFx.flatLook(user);
        for (int s = 0; s < 16; s++) {
            double d = s * 0.5;
            level.sendParticles(ParticleTypes.END_ROD,
                    eye.x + flat.x * d, eye.y + flat.y * d, eye.z + flat.z * d, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    /** 一道前向弧光：金色飞斩沿水平扇面 ±45° 依次射出。 */
    private static void arcSweep(ServerLevel level, LivingEntity user, float damage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        for (int i = 0; i < ARC_DRIVES; i++) {
            float deg = -45.0F + 90.0F * i / (ARC_DRIVES - 1);
            Vec3 dir = look.yRot((float) Math.toRadians(deg));
            SaFx.drive(level, user, eye, new Vec3(dir.x, look.y + 0.02, dir.z),
                    1.6F, damage, i % 2 == 0 ? GOLD : PALE_GOLD, 1.2F, 18.0F);
        }
    }

    /** 向前贯出的晨曦光枪：一道前向 END_ROD 光束 + 紧束金刃齐射。 */
    private static void dawnLance(ServerLevel level, LivingEntity user, RandomSource rng, float damage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        // 前向光束
        for (int s = 0; s < 32; s++) {
            double d = s * 0.6;
            double x = eye.x + look.x * d;
            double y = eye.y + look.y * d;
            double z = eye.z + look.z * d;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
            if (s % 2 == 0) {
                level.sendParticles(goldDust(1.4F), x, y, z, 1, 0.12, 0.12, 0.12, 0.0);
            }
        }
        // 紧束金刃齐射（小散布，扎成一束向前贯穿）
        for (int i = 0; i < LANCE_DRIVES; i++) {
            Vec3 dir = new Vec3(
                    look.x + (rng.nextDouble() - 0.5) * 0.12,
                    look.y + (rng.nextDouble() - 0.5) * 0.08,
                    look.z + (rng.nextDouble() - 0.5) * 0.12).normalize();
            SaFx.drive(level, user, eye, dir, 2.0F, damage, PALE_GOLD, 1.1F, 22.0F);
        }
    }

    /** 向前裂空「破晓斩」+ 金色爆闪 + 前方扇区神圣伤害。 */
    private static void daybreakFinale(ServerLevel level, LivingEntity user, float damage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        // 一记巨大的金色裂空斩向前劈开天幕
        SaFx.judgementCut(level, user, eye, look, GOLD, damage, 90.0F, 18, 1.2F);

        // 前方爆发点（眼前约 2.5 格）
        Vec3 burst = eye.add(look.scale(2.5));
        level.sendParticles(ParticleTypes.FLASH, burst.x, burst.y, burst.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(goldDust(1.6F), burst.x, burst.y, burst.z, 90, 0.7, 0.7, 0.7, 0.3);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24.0;
            level.sendParticles(ParticleTypes.END_ROD, burst.x, burst.y, burst.z,
                    0, Math.cos(a) * 0.5, 0.1, Math.sin(a) * 0.5, 1.0);
        }

        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            t.hurt(level.damageSources().magic(), damage);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.3F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4F, 0.9F);
    }
}

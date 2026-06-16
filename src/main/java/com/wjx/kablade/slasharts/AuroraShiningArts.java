package com.wjx.kablade.slasharts;

import com.wjx.kablade.event.AuroraColorCycling;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustColorTransitionOptions;
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
 * 极光映天 —— 极光刃「映天」专属 SA（在 1.12.2 {@code SaAuroraShining} 基础上重做为<b>向前突进式</b>大招）。
 * <p>
 * 全程朝玩家面向方向倾泻：蓄力 → 极光刃洪流向前喷射 → 把极光从天召至<b>正前方</b>的打击区 →
 * 最后裂空三连「极光斩」向前撕开天幕、引爆前方一片。颜色取自极光色谱并平滑流转。
 * <ul>
 *   <li>t=0：自身夜视、前方敌人发光；脚下极光蓄力环 + 一道指向前方的极光光束（蓄力，不放招）+ 8 把环身索敌幻影剑</li>
 *   <li>t=6：16 道极光飞斩呈锥形向前喷射</li>
 *   <li>t=12：极光自天而降，砸向正前方约 8 格处的打击区</li>
 *   <li>t=18：向前裂空「极光斩」五连 + 前方极光爆发 + 前方扇区无视护甲伤害/缓速/发光</li>
 * </ul>
 */
public final class AuroraShiningArts extends SlashArts {

    private static final int GUARD_SWORDS = 8;
    private static final int STORM_DRIVES = 16;
    private static final int RAIN_DRIVES = 12;

    private static final float BASE_DRIVE_DAMAGE = 2.0F;
    private static final float BASE_SWORD_DAMAGE = 3.0F;
    private static final float ATTACK_RATIO = 0.5F;
    private static final float BYPASS_BASE_DAMAGE = 4.0F;
    private static final float BYPASS_ATTACK_RATIO = 0.4F;

    /** 前方打击 / 结算射程（格）。 */
    private static final double FORWARD_RANGE = 18.0;
    /** 召唤区在正前方多少格。 */
    private static final double STRIKE_AHEAD = 8.0;
    private static final int GLOW_DURATION = 2400;
    private static final int NIGHT_VISION_DURATION = 2400;
    private static final int SLOW_DURATION = 80;

    public AuroraShiningArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    private static int auroraColor(RandomSource rng) {
        return AuroraColorCycling.getRandomColor(rng);
    }

    private static DustColorTransitionOptions auroraDust(RandomSource rng, float scale) {
        return new DustColorTransitionOptions(
                SaFx.rgb(auroraColor(rng)), SaFx.rgb(auroraColor(rng)), scale);
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
        final float bypassDamage = BYPASS_BASE_DAMAGE + bladeAttack * BYPASS_ATTACK_RATIO;

        // ── t=0：蓄力（不放招，向前蓄势）──
        if (user instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false));
        }
        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
        }
        auroraCharge(level, user, rng);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.7F);

        // 环身极光幻影剑（自动索敌，朝前方目标补刀）
        for (int i = 0; i < GUARD_SWORDS; i++) {
            EntityBlisteringSwords sword = new EntityBlisteringSwords(
                    SlashBlade.RegistryEvents.BlisteringSwords, level);
            sword.setPos(user.getX(), user.getY() + 1.2, user.getZ());
            sword.setShooter(user);
            sword.setDamage(swordDamage);
            sword.setColor(auroraColor(rng));
            sword.setDelay(i * 3);
            level.addFreshEntity(sword);
            sword.startRiding(user, true);
        }

        // ── t=6：极光刃洪流向前喷射 ──
        SaFx.schedule(level, 6, () -> {
            if (!user.isAlive()) return;
            auroraStorm(level, user, rng, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3F, 1.4F);
        });

        // ── t=12：极光自天而降，砸向正前方打击区 ──
        SaFx.schedule(level, 12, () -> {
            if (!user.isAlive()) return;
            auroraRainAhead(level, user, rng, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.1F);
        });

        // ── t=18：向前裂空极光斩 + 前方引爆 ──
        SaFx.schedule(level, 18, () -> {
            if (!user.isAlive()) return;
            auroraFinale(level, user, rng, bypassDamage);
        });

        return super.doArts(type, user);
    }

    /** 蓄力：脚下极光环 + 一道指向前方的极光光束（END_ROD 芯 + 极光尘）。 */
    private static void auroraCharge(ServerLevel level, LivingEntity user, RandomSource rng) {
        Vec3 center = user.position();
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24.0;
            level.sendParticles(auroraDust(rng, 1.3F),
                    center.x + Math.cos(a) * 1.5, center.y + 0.1, center.z + Math.sin(a) * 1.5,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 flat = SaFx.flatLook(user);
        for (int s = 0; s < 16; s++) {
            double d = s * 0.5;
            double x = eye.x + flat.x * d;
            double y = eye.y + flat.y * d;
            double z = eye.z + flat.z * d;
            level.sendParticles(auroraDust(rng, 1.2F), x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            if (s % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    /** 极光刃洪流：16 道飞斩朝视线呈锥形向前喷射。 */
    private static void auroraStorm(ServerLevel level, LivingEntity user, RandomSource rng, float damage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        for (int i = 0; i < STORM_DRIVES; i++) {
            Vec3 dir = new Vec3(
                    look.x + (rng.nextDouble() - 0.5) * 0.5,
                    look.y + (rng.nextDouble() - 0.5) * 0.3,
                    look.z + (rng.nextDouble() - 0.5) * 0.5).normalize();
            float speed = 1.5F + rng.nextFloat() * 0.5F;
            SaFx.drive(level, user, eye, dir, speed, damage, auroraColor(rng), 1.1F, 20.0F);
        }
        for (int i = 0; i < 24; i++) {
            level.sendParticles(auroraDust(rng, 1.2F), eye.x, eye.y, eye.z, 0,
                    look.x * 0.4 + (rng.nextDouble() - 0.5) * 0.3, look.y * 0.4,
                    look.z * 0.4 + (rng.nextDouble() - 0.5) * 0.3, 1.0);
        }
    }

    /** 极光自天而降，砸向正前方约 {@link #STRIKE_AHEAD} 格的打击区。 */
    private static void auroraRainAhead(ServerLevel level, LivingEntity user, RandomSource rng, float damage) {
        Vec3 flat = SaFx.flatLook(user);
        Vec3 base = user.position();
        Vec3 zone = new Vec3(base.x + flat.x * STRIKE_AHEAD, base.y, base.z + flat.z * STRIKE_AHEAD);
        for (int i = 0; i < RAIN_DRIVES; i++) {
            double ox = (rng.nextDouble() - 0.5) * 8.0;
            double oz = (rng.nextDouble() - 0.5) * 8.0;
            Vec3 pos = new Vec3(zone.x + ox, zone.y + 12.0 + rng.nextDouble() * 3.0, zone.z + oz);
            Vec3 dir = new Vec3((rng.nextDouble() - 0.5) * 0.15, -1.0, (rng.nextDouble() - 0.5) * 0.15);
            SaFx.drive(level, user, pos, dir, 1.7F, damage, auroraColor(rng), 1.0F, 28.0F);
            for (int k = 0; k < 5; k++) {
                level.sendParticles(auroraDust(rng, 1.0F), pos.x, pos.y - k * 1.8, pos.z,
                        1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    /** 向前裂空极光斩五连 + 前方极光爆发 + 前方扇区引爆。 */
    private static void auroraFinale(ServerLevel level, LivingEntity user, RandomSource rng, float bypassDamage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        for (int k = -2; k <= 2; k++) {
            Vec3 dir = look.yRot((float) Math.toRadians(k * 12.0));
            SaFx.judgementCut(level, user, eye, dir, auroraColor(rng), bypassDamage, 80.0F, 16, 1.2F);
        }

        // 前方爆发点（眼前约 2.5 格）
        Vec3 burst = eye.add(look.scale(2.5));
        for (int i = 0; i < 20; i++) {
            level.sendParticles(auroraDust(rng, 1.6F), burst.x, burst.y, burst.z, 7, 0.6, 0.6, 0.6, 0.35);
        }
        for (int i = 0; i < 30; i++) {
            level.sendParticles(ParticleTypes.END_ROD, burst.x, burst.y, burst.z,
                    0, (rng.nextDouble() - 0.5) * 0.6, (rng.nextDouble() - 0.5) * 0.6,
                    (rng.nextDouble() - 0.5) * 0.6, 1.0);
        }

        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            t.hurt(level.damageSources().magic(), bypassDamage);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, 1));
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6F, 0.7F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4F, 1.2F);
    }
}

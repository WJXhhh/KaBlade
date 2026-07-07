package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.AuroraVeilEntity;
import com.wjx.kablade.event.AuroraColorCycling;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
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
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 鏋佸厜鑰€澶?鈥斺€?鏋佸厜鍒冦€屾槧澶┿€嶄笓灞?SA锛堝湪 1.12.2 {@code SaAuroraShining} 鍩虹涓婇噸鍋氫负<b>鍚戝墠绐佽繘寮?/b>澶ф嫑锛夈€? * <p>
 * 鍏ㄧ▼鏈濈帺瀹堕潰鍚戞柟鍚戝€炬郴锛氳搫鍔?鈫?鏋佸厜鍒冩椽娴佸悜鍓嶅柗灏?鈫?鎶婃瀬鍏変粠澶╁彫鑷?b>姝ｅ墠鏂?/b>鐨勬墦鍑诲尯 鈫? * 鏈€鍚庤绌轰笁杩炪€屾瀬鍏夋柀銆嶅悜鍓嶆挄寮€澶╁箷銆佸紩鐖嗗墠鏂逛竴鐗囥€傞鑹插彇鑷瀬鍏夎壊璋卞苟骞虫粦娴佽浆銆? * <ul>
 *   <li>t=0锛氳嚜韬瑙嗐€佸墠鏂规晫浜哄彂鍏夛紱鑴氫笅鏋佸厜钃勫姏鐜?+ 涓€閬撴寚鍚戝墠鏂圭殑鏋佸厜鍏夋潫锛堣搫鍔涳紝涓嶆斁鎷涳級+ 8 鎶婄幆韬储鏁屽够褰卞墤</li>
 *   <li>t=6锛?6 閬撴瀬鍏夐鏂╁憟閿ュ舰鍚戝墠鍠峰皠</li>
 *   <li>t=12锛氭瀬鍏夎嚜澶╄€岄檷锛岀牳鍚戞鍓嶆柟绾?8 鏍煎鐨勬墦鍑诲尯</li>
 *   <li>t=18锛氬悜鍓嶈绌恒€屾瀬鍏夋柀銆嶄簲杩?+ 鍓嶆柟鏋佸厜鐖嗗彂 + 鍓嶆柟鎵囧尯鏃犺鎶ょ敳浼ゅ/缂撻€?鍙戝厜</li>
 * </ul>
 */
public final class AuroraShiningArts extends SlashArts {

    private static final int GUARD_SWORDS = 5;
    private static final int STORM_DRIVES = 10;
    private static final int RAIN_DRIVES = 8;

    private static final float DRIVE_BASE = 0.84F;
    private static final float SWORD_BASE = 1.41F;
    private static final float HIT_RATIO = 0.169F;
    /** 缁堢粨鎶€鏃犺鎶ょ敳銆佸墠鏂规墖鍖轰竴鍑荤殑涓讳激瀹炽€?*/
    private static final float AOE_BASE = 6.75F;
    private static final float AOE_RATIO = 1.41F;

    /** 鍓嶆柟鎵撳嚮 / 缁撶畻灏勭▼锛堟牸锛夈€?*/
    private static final double FORWARD_RANGE = 18.0;
    /** 鍙敜鍖哄湪姝ｅ墠鏂瑰灏戞牸銆?*/
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
        final float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        final float driveDamage = DRIVE_BASE + MathFunc.amplifierCalc(bladeAttack, HIT_RATIO);
        final float swordDamage = SWORD_BASE + MathFunc.amplifierCalc(bladeAttack, HIT_RATIO);
        final float bypassDamage = AOE_BASE + MathFunc.amplifierCalc(bladeAttack, AOE_RATIO);

        // 鈹€鈹€ t=0锛氳搫鍔涳紙涓嶆斁鎷涳紝鍚戝墠钃勫娍锛夆攢鈹€
        if (user instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false));
        }
        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
        }
        auroraCharge(level, user, rng);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.7F);

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

        // 鈹€鈹€ t=6锛氭瀬鍏夊垉娲祦鍚戝墠鍠峰皠 鈹€鈹€
        SaFx.schedule(level, 6, () -> {
            if (!user.isAlive()) return;
            auroraStorm(level, user, rng, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3F, 1.4F);
        });

        // 鈹€鈹€ t=12锛氭瀬鍏夎嚜澶╄€岄檷锛岀牳鍚戞鍓嶆柟鎵撳嚮鍖?鈹€鈹€
        SaFx.schedule(level, 12, () -> {
            if (!user.isAlive()) return;
            auroraRainAhead(level, user, rng, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.1F);
        });

        // 鈹€鈹€ t=18锛氬悜鍓嶈绌烘瀬鍏夋柀 + 鍓嶆柟寮曠垎 鈹€鈹€
        SaFx.schedule(level, 18, () -> {
            if (!user.isAlive()) return;
            auroraFinale(level, user, rng, bypassDamage, driveDamage);
        });

        return super.doArts(type, user);
    }

    /** 钃勫姏锛氳剼涓嬫瀬鍏夌幆 + 涓€閬撴寚鍚戝墠鏂圭殑鏋佸厜鍏夋潫锛圗ND_ROD 鑺?+ 鏋佸厜灏橈級銆?*/
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

    /** 鏋佸厜鍒冩椽娴侊細16 閬撻鏂╂湞瑙嗙嚎鍛堥敟褰㈠悜鍓嶅柗灏勩€?*/
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
        Vec3 flat = SaFx.flatLook(user);
        Vec3 perp = new Vec3(-flat.z, 0.0, flat.x);
        Vec3 vel = flat.scale(0.55);
        for (int k = -1; k <= 1; k++) {
            Vec3 off = perp.scale(k * 2.0);
            AuroraVeilEntity.spawn(level, user.getX() + off.x, user.getY() + 0.2, user.getZ() + off.z,
                    user.getYRot(), vel, 1.0F, 26, rng.nextInt(), user, damage);
        }
        for (int i = 0; i < 24; i++) {
            level.sendParticles(auroraDust(rng, 1.2F), eye.x, eye.y, eye.z, 0,
                    look.x * 0.4 + (rng.nextDouble() - 0.5) * 0.3, look.y * 0.4,
                    look.z * 0.4 + (rng.nextDouble() - 0.5) * 0.3, 1.0);
        }
    }

    /** 鏋佸厜鑷ぉ鑰岄檷锛岀牳鍚戞鍓嶆柟绾?{@link #STRIKE_AHEAD} 鏍肩殑鎵撳嚮鍖恒€?*/
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

    /** 鍚戝墠瑁傜┖鏋佸厜鏂╀簲杩?+ 鍓嶆柟鏋佸厜鐖嗗彂 + 鍓嶆柟鎵囧尯寮曠垎銆?*/
    private static void auroraFinale(ServerLevel level, LivingEntity user, RandomSource rng,
                                     float bypassDamage, float veilDamage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        AuroraVeilEntity.spawn(level, user.getX(), user.getY() + 0.2, user.getZ(),
                user.getYRot(), SaFx.flatLook(user).scale(0.45), 2.2F, 34, rng.nextInt(), user, veilDamage);

        // 鍓嶆柟鐖嗗彂鐐癸紙鐪煎墠绾?2.5 鏍硷級
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
            if (!SaTargeting.canDamageAttackable(user, t)) continue;
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(t, level.damageSources().magic(), bypassDamage);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, 1));
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6F, 0.7F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4F, 1.2F);
    }
}

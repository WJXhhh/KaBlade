package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.DawnCrescentEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
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
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 寮у厜鐮存檽 鈥斺€?寮у厜鍒冦€屾祦鑺掋€嶄笓灞?SA锛堝湪 1.12.2 {@code SaBreakTheDawn} 鍩虹涓婇噸鍋氫负<b>鍚戝墠绐佽繘寮?/b>澶ф嫑锛夈€? * <p>
 * 銆屼互涓€鍒€鍒掔牬榛戝锛屽紩鏉ョ牬鏅撱€嶏紝鍏ㄧ▼鏈濈帺瀹堕潰鍚戞柟鍚戝妶鍑猴細鑷寸洸姝ｅ墠鏂圭殑鏁屼汉 鈫?妯壂涓€閬撻噾鑹插姬鍏?鈫? * 鍚戝墠璐嚭涓€閬撴櫒鏇﹀厜鏋?鈫?鏈€鍚庝竴璁拌绌恒€岀牬鏅撴柀銆嶅妶绌垮墠鏂广€侀噾鍏夐亶閲庛€傞噾鑹插厜杈夛紙END_ROD 鍏夋潫 + 鐞ョ弨閲戝皹锛夈€? * <ul>
 *   <li>t=0锛氳嚜韬瑙嗭紱鍓嶆柟鏁屼汉鑷寸洸 + 鍙戝厜锛涜剼涓嬩袱閬撻噾鐜?+ 鎸囧悜鍓嶆柟鐨勬櫒鏇﹁搫鍔涘厜鏉?+ 6 鎶婄幆韬噾鍒?/li>
 *   <li>t=5锛? 閬撻噾鑹查鏂╂í鎵垚涓€閬撳墠鍚戝姬鍏夛紙卤45掳锛?/li>
 *   <li>t=10锛氬悜鍓嶈疮鍑烘櫒鏇﹀厜鏋紙END_ROD 鍏夋潫 + 绱ф潫閲戝垉榻愬皠锛?/li>
 *   <li>t=15锛氬悜鍓嶈绌恒€岀牬鏅撴柀銆? 閲戣壊鐖嗛棯 + 鍓嶆柟鎵囧尯绁炲湥浼ゅ</li>
 * </ul>
 */
public final class BreakTheDawnArts extends SlashArts {

    private static final int GUARD_SWORDS = 4;
    private static final int ARC_DRIVES = 6;
    private static final int LANCE_DRIVES = 5;

    private static final float DRIVE_BASE = 0.56F;
    private static final float SWORD_BASE = 0.94F;
    private static final float HIT_RATIO = 0.112F;
    /** 缁堢粨鎶€鍓嶆柟鎵囧尯涓€鍑荤殑涓讳激瀹炽€?*/
    private static final float AOE_BASE = 4.5F;
    private static final float AOE_RATIO = 0.94F;

    private static final double FORWARD_RANGE = 14.0;
    private static final int GLOW_DURATION = 2400;
    private static final int NIGHT_VISION_DURATION = 1600;
    private static final int BLIND_DURATION = 60;

    /** 閲戣壊璋冿紙寮у厜 / 鏅ㄦ洣锛夈€傚亸鏆栫殑鐞ョ弨閲戯紝璺熺櫧鑹?END_ROD/FLASH 鍏夋潫鎷夊紑瀵规瘮锛岄伩鍏?climax 绯婃垚涓€鍧ㄧ櫧銆?*/
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
        final float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        final float driveDamage = DRIVE_BASE + MathFunc.amplifierCalc(bladeAttack, HIT_RATIO);
        final float swordDamage = SWORD_BASE + MathFunc.amplifierCalc(bladeAttack, HIT_RATIO);
        final float finaleDamage = AOE_BASE + MathFunc.amplifierCalc(bladeAttack, AOE_RATIO);

        // 鈹€鈹€ t=0锛氱牬鏅擄紙鍚戝墠钃勫娍锛夆攢鈹€
        if (user instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false));
        }
        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_DURATION, 0));  // 鍒虹洰鏅ㄥ厜
        }
        dawnCharge(level, user);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.4F);

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

        // 鈹€鈹€ t=5锛氬墠鍚戝姬鍏夋í鎵?鈹€鈹€
        SaFx.schedule(level, 5, () -> {
            if (!user.isAlive()) return;
            arcSweep(level, user, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.5F);
        });

        // 鈹€鈹€ t=10锛氬悜鍓嶈疮鍑烘櫒鏇﹀厜鏋?鈹€鈹€
        SaFx.schedule(level, 10, () -> {
            if (!user.isAlive()) return;
            dawnLance(level, user, rng, driveDamage);
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.6F);
        });

        // 鈹€鈹€ t=15锛氬悜鍓嶈绌虹牬鏅撴柀 + 鍓嶆柟寮曠垎 鈹€鈹€
        SaFx.schedule(level, 15, () -> {
            if (!user.isAlive()) return;
            daybreakFinale(level, user, finaleDamage, driveDamage);
        });

        return super.doArts(type, user);
    }

    /** 钃勫姏锛氳剼涓嬩袱閬撻噾鐜?+ 鎸囧悜鍓嶆柟鐨勬櫒鏇﹀厜鏉熴€?*/
    private static void dawnCharge(ServerLevel level, LivingEntity user) {
        Vec3 center = user.position();
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48.0;
            for (double r = 2.0; r <= 3.6; r += 1.6) {
                level.sendParticles(goldDust(0.9F),
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

    /** 涓€閬撳墠鍚戝姬鍏夛細涓夊集閲戣壊寮ф湀锛堟嫑鐗岋級鎵囧舰椋炲嚭锛岃緟浠ョ粏閲戦鏂╁～鍏呫€?*/
    private static void arcSweep(ServerLevel level, LivingEntity user, float damage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        // 鎷涚墝锛氫笁寮姬鏈堝憟鎵囧舰鍚戝墠椋炴帬锛堟湞鍚戝悇鑷殑椋炶鏂瑰悜锛屾湀闈㈡湞鍓嶏級
        float baseYaw = user.getYRot();
        for (int k = -1; k <= 1; k++) {
            float yaw = baseYaw + k * 20.0F;
            DawnCrescentEntity.spawn(level, eye.x, eye.y, eye.z, yaw, SaFx.yawDir(yaw, 0.9),
                    1.0F, 16, GOLD, user, damage);
        }
        // 缁嗛噾椋炴柀濉厖鎵囬潰
        for (int i = 0; i < ARC_DRIVES; i++) {
            float deg = -45.0F + 90.0F * i / (ARC_DRIVES - 1);
            Vec3 dir = look.yRot((float) Math.toRadians(deg));
            SaFx.drive(level, user, eye, new Vec3(dir.x, look.y + 0.02, dir.z),
                    1.6F, damage, i % 2 == 0 ? GOLD : PALE_GOLD, 1.0F, 18.0F);
        }
    }

    /** 鍚戝墠璐嚭鐨勬櫒鏇﹀厜鏋細涓€閬撳墠鍚?END_ROD 鍏夋潫 + 绱ф潫閲戝垉榻愬皠銆?*/
    private static void dawnLance(ServerLevel level, LivingEntity user, RandomSource rng, float damage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        // 鍓嶅悜鍏夋潫
        for (int s = 0; s < 32; s++) {
            double d = s * 0.6;
            double x = eye.x + look.x * d;
            double y = eye.y + look.y * d;
            double z = eye.z + look.z * d;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
            if (s % 2 == 0) {
                level.sendParticles(goldDust(0.9F), x, y, z, 1, 0.12, 0.12, 0.12, 0.0);
            }
        }
        // 绱ф潫閲戝垉榻愬皠锛堝皬鏁ｅ竷锛屾墡鎴愪竴鏉熷悜鍓嶈疮绌匡級
        for (int i = 0; i < LANCE_DRIVES; i++) {
            Vec3 dir = new Vec3(
                    look.x + (rng.nextDouble() - 0.5) * 0.12,
                    look.y + (rng.nextDouble() - 0.5) * 0.08,
                    look.z + (rng.nextDouble() - 0.5) * 0.12).normalize();
            SaFx.drive(level, user, eye, dir, 2.0F, damage, PALE_GOLD, 1.1F, 22.0F);
        }
    }

    /** 鍚戝墠瑁傜┖銆岀牬鏅撴柀銆? 閲戣壊鐖嗛棯 + 鍓嶆柟鎵囧尯绁炲湥浼ゅ銆?*/
    private static void daybreakFinale(ServerLevel level, LivingEntity user, float damage, float crescentDamage) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        // 鎷涚墝鏀跺熬锛氫竴寮法澶х殑閲戣壊寮ф湀鍚戝墠鍔堝紑澶╁箷锛堣繛鍙戜袱寮竴澶т竴灏忥紝鏈堥潰鏈濆墠锛岃嚜甯︽壂杩囧嵆浼わ級
        float yaw = user.getYRot();
        DawnCrescentEntity.spawn(level, eye.x, eye.y, eye.z, yaw, SaFx.yawDir(yaw, 0.9), 2.2F, 22, GOLD,
                user, crescentDamage);
        DawnCrescentEntity.spawn(level, eye.x, eye.y, eye.z, yaw, SaFx.yawDir(yaw, 0.78), 1.7F, 20, PALE_GOLD,
                user, crescentDamage);

        Vec3 burst = eye.add(look.scale(2.5));
        level.sendParticles(goldDust(1.0F), burst.x, burst.y, burst.z, 36, 0.5, 0.5, 0.5, 0.25);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16.0;
            level.sendParticles(ParticleTypes.END_ROD, burst.x, burst.y, burst.z,
                    0, Math.cos(a) * 0.5, 0.1, Math.sin(a) * 0.5, 1.0);
        }
        for (LivingEntity t : SaFx.forwardHostiles(level, user, FORWARD_RANGE)) {
            if (!SaTargeting.canDamageAttackable(user, t)) continue;
            t.hurt(level.damageSources().magic(), damage);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0));
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.3F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4F, 0.9F);
    }
}

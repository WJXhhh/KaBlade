package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.CutMetalRingEntity;
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

import java.util.List;
import java.util.function.Function;

/**
 * 鏂╅搧鏂噾 鈥斺€?澶嶅悎鍒冦€屾柀閾併€嶄笓灞?SA銆? * 浠?1.12.2 {@code SaCutMetal} 绉绘鑰屾潵銆? * <p>
 * 鑷韩鑾峰緱鍔涢噺 II 3 绉掞紝瀵瑰懆鍥村ぇ鑼冨洿鏁屼汉閫犳垚楂橀浼ゅ锛堝熀纭€ 8 + 鍒€鏀诲嚮鍔涜ˉ姝ｏ級锛? * 骞惰拷鍔犵洰鏍囨姢鐢插€?50% 鐨勯澶栦激瀹炽€傝鍑讳腑鐨勬晫浜虹垎鍑轰激瀹虫寚绀虹矑瀛愩€? */
public final class CutMetalArts extends SlashArts {

    /** 鍩虹浼ゅ锛堜笉鍚敾鍑诲姏琛ユ锛夈€?*/
    private static final float BASE_DAMAGE = 8.0F;
    /** 鏀诲嚮鍔涜ˉ姝ｇ郴鏁帮細extraDamage = amplifierCalc(bladeAttack, 8)銆?*/
    private static final float ATTACK_FACTOR = 8.0F;
    /** 鎶ょ敳杩藉姞浼ゅ鍊嶇巼銆?*/
    private static final float ARMOR_RATIO = 0.5F;
    /** 鍔涢噺鏁堟灉鎸佺画鏃堕棿锛坱ick锛夈€?*/
    private static final int STRENGTH_DURATION = 60;
    /** AABB 鎵╁睍鑼冨洿銆?*/
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

        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION, 1, false, false));
        spawnBladeLight(level, user);
        AABB bb = user.getBoundingBox().inflate(RANGE_XZ, RANGE_Y, RANGE_XZ)
                .move(user.getDeltaMovement());
        DamageSource src = user.level().damageSources().playerAttack((Player) user);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bb,
                e -> SaTargeting.canDamageAttackable(user, e));

        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        for (LivingEntity target : targets) {
            // 鏆村嚮鐗规晥
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, user, BASE_DAMAGE + extraDamage);
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));

            // 鎶ょ敳杩藉姞浼ゅ
            double armor = target.getAttribute(Attributes.ARMOR).getValue();
            if (armor > 0) {
                com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, user, (float) (armor * ARMOR_RATIO));
                blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
            }

            // 浼ゅ鎸囩ず绮掑瓙
            for (int i = 0; i < 20; i++) {
                double px = target.getX() + (level.random.nextDouble() - 0.5) * 4;
                double py = target.getY() + level.random.nextDouble() * target.getBbHeight();
                double pz = target.getZ() + (level.random.nextDouble() - 0.5) * 4;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, px, py, pz, 1, 0, 0, 0, 0);
            }
        }

        // 闊虫晥
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);

        return super.doArts(type, user);
    }

    /** 鐧借壊鐜舰鍒€鍏夛細涓荤幆 + 涓ら亾閿欏嘲浣欒緣锛岃创杩戞嫈鍒€鍓戦噸閿嬬殑骞插噣閾剁櫧鍒€鐥曘€?*/
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
}

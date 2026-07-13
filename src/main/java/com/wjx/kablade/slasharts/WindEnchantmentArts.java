package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.WindEnchantmentEntity;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

/**
 * 椋庝箣缁撶晫 鈥斺€?銆屽绮惧墤路甯屽皵鏂囥€嶄笓灞?SA銆? * <p>
 * 瀵瑰簲 1.12.2 鐨?{@code HonkaiWindEnchantment}锛圫A ID 451锛夈€? * <ol>
 *   <li>鍚戝墠灏勭嚎杩借釜 8 鏍奸攣瀹氱洰鏍?/li>
 *   <li>鑻ュ懡涓細鎵ц SA 缁勫悎鏀诲嚮 + 鏆村嚮 + 20 鐐归澶栦激瀹?/li>
 *   <li>鐜╁鍛ㄨ韩鐖嗗彂 60 棰楃儫闆剧矑瀛?/li>
 *   <li>鐢熸垚 {@link WindEnchantmentEntity} 椋庝箣缁撶晫鍏夌幆锛圓OE 缁欓檮杩戠帺瀹跺姞椋庝箣鍔?buff锛?/li>
 * </ol>
 */
public final class WindEnchantmentArts extends SlashArts {

    private static final double RAY_DISTANCE = 8.0;
    private static final float EXTRA_DAMAGE = 20.0F;
    private static final int SMOKE_PARTICLES = 60;

    public WindEnchantmentArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        if (!(user instanceof Player player)) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return super.doArts(type, user);
        }

        // 1. 灏勭嚎閿佸畾鐩爣
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        AABB scanBox = player.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox,
                e -> e != player && e.isAlive() && e.isPickable());

        LivingEntity target = null;
        double closestDist = RAY_DISTANCE;

        for (LivingEntity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hitOpt = bb.clip(eye, end);
            if (bb.contains(eye)) {
                target = candidate;
                closestDist = 0;
                break;
            } else if (hitOpt.isPresent()) {
                double d = eye.distanceTo(hitOpt.get());
                if (d < closestDist) {
                    target = candidate;
                    closestDist = d;
                }
            }
        }

        // 2. 鏀诲嚮鐩爣
        if (target != null && !SaTargeting.canDamageAttackable(player, target)) {
            return super.doArts(type, user);
        }
        if (target != null) {
            blade.getItem().hurtEnemy(blade, target, player);
            player.crit(target);
            // 绗簩娈碉細棰濆浼ゅ
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, player, EXTRA_DAMAGE);
        }

        for (int i = 0; i < SMOKE_PARTICLES; i++) {
            double px = player.getX() + signedRandomOffset(level, 3.0);
            double py = player.getY() + level.random.nextDouble();
            double pz = player.getZ() + signedRandomOffset(level, 3.0);
            level.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1,
                    0, 0.1, 0, 0);
        }

        // 4. 鐢熸垚椋庝箣缁撶晫鍏夌幆
        WindEnchantmentEntity.spawn(level, player.getX(), player.getY(), player.getZ());

        return super.doArts(type, user);
    }

    private static double signedRandomOffset(ServerLevel level, double radius) {
        double sign = level.random.nextBoolean() ? 1.0 : -1.0;
        return level.random.nextDouble() * radius * sign;
    }
}

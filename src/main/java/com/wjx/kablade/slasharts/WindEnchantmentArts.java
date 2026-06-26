package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.WindEnchantmentEntity;
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
 * 风之结界 —— 「妖精剑·希尔文」专属 SA。
 * <p>
 * 对应 1.12.2 的 {@code HonkaiWindEnchantment}（SA ID 451）。
 * <ol>
 *   <li>向前射线追踪 8 格锁定目标</li>
 *   <li>若命中：执行 SA 组合攻击 + 暴击 + 20 点额外伤害</li>
 *   <li>玩家周身爆发 60 颗烟雾粒子</li>
 *   <li>生成 {@link WindEnchantmentEntity} 风之结界光环（AOE 给附近玩家加风之力 buff）</li>
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
        // 仅在服务端和执行成功时干活
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

        // 1. 射线锁定目标
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

        // 2. 攻击目标
        if (target != null) {
            blade.getItem().hurtEnemy(blade, target, player);  // ItemSlashBlade.attackTargetEntity 等价于 hurtEnemy + 后续
            player.crit(target);  // player.onCriticalHit → player.crit
            target.hurt(level.damageSources().playerAttack(player), EXTRA_DAMAGE);
        }

        // 3. 烟雾粒子：保留 1.12.2 原版的正负 3 格分布。
        for (int i = 0; i < SMOKE_PARTICLES; i++) {
            double px = player.getX() + signedRandomOffset(level, 3.0);
            double py = player.getY() + level.random.nextDouble();
            double pz = player.getZ() + signedRandomOffset(level, 3.0);
            level.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1,
                    0, 0.1, 0, 0);
        }

        // 4. 生成风之结界光环
        WindEnchantmentEntity.spawn(level, player.getX(), player.getY(), player.getZ());

        return super.doArts(type, user);
    }

    private static double signedRandomOffset(ServerLevel level, double radius) {
        double sign = level.random.nextBoolean() ? 1.0 : -1.0;
        return level.random.nextDouble() * radius * sign;
    }
}

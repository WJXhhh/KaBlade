package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.FlareEdgeEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 龙一文字线 SA「熔岩驱动」—— 1.12.2 {@code LaveDriveEx} 完整移植。
 * <p>
 * 对周围 5 格敌人造成 AOE 斩击，并向玩家前方扇形射出 3×3 列熔岩飞刃，
 * 熔岩飞刃命中时造成灼烧 + 魔法伤害。
 */
public final class LaveDriveArts extends SlashArts {

    private static final float AOE_RADIUS = 5.0F;
    private static final int DRIVE_COLOR = 0x600030; // 6291504

    public LaveDriveArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return super.doArts(type, user);
        }

        // 爆炸音
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        // 100 个熔岩粒子
        for (int i = 0; i < 100; i++) {
            double d0 = user.getRandom().nextGaussian() * 0.02;
            double d2 = user.getRandom().nextGaussian() * 0.02;
            double d3 = user.getRandom().nextGaussian() * 0.02;
            double ox = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d0 * 10.0) * 5.0;
            double oz = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d3 * 10.0) * 5.0;
            level.sendParticles(ParticleTypes.LAVA,
                    user.getX() + ox, user.getY(), user.getZ() + oz,
                    1, d0, d2, d3, 0.0);
        }

        // ── 服务端逻辑 ──────────────────────────────────────
        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float magicDamage = baseAttack * 0.53F;

        // AOE 斩击
        AABB box = user.getBoundingBox().inflate(AOE_RADIUS, 0.25, AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().mobAttack(user), baseAttack);
        }

        // 扇形射出熔岩飞刃 (3 列 × 3 行 = 共 9 发)
        int maxCol = 3;
        int maxCount = 3;
        double radBaseRot = Math.toRadians(user.getYRot());
        double radRot = Math.PI * 2 / maxCount;

        for (int j = 0; j < maxCol; j++) {
            for (int i = 0; i < maxCount; i++) {
                double posY = user.getY() + user.getEyeHeight() / 2.0;

                // 位置：玩家前方的扇形
                double px = user.getX() + Math.cos(radBaseRot + radRot * i);
                double pz = user.getZ() + Math.sin(radBaseRot + radRot * i);

                // 方向：玩家面朝方向（1.12.2 setLocationAndAngles + setDriveVector 逻辑）
                Vec3 dir = user.getLookAngle();

                FlareEdgeEntity flare = FlareEdgeEntity.spawn(level,
                        user,
                        new Vec3(px, posY, pz),
                        dir,
                        magicDamage,
                        DRIVE_COLOR,
                        20 + 3 * j + i,   // lifetime
                        90.0F,             // roll
                        true               // multiHit
                );
                flare.setInitialSpeed(0.1F);
                flare.setNextSpeed(1.05F);
                flare.setChangeTime(5 + 2 * j + i);

                // 30% 概率附加熔岩粒子效果
                flare.setParticleEnabled(level.random.nextInt(10) < 3);
                if (flare.isParticleEnabled()) {
                    flare.setParticleStyle("LAVA");
                }
            }
        }

        return super.doArts(type, user);
    }
}

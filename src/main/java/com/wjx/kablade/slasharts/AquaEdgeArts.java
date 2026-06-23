package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.AquaEdgeEntity;
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
 * 龙一文字线 SA「苍流刃」—— 1.12.2 {@code AquaEdgeEx} 完整移植。
 * <p>
 * 扑灭自身火焰，对周围 5 格敌人造成 AOE 斩击，并向玩家前方扇形射出 3×N 列水流飞刃，
 * 水流飞刃命中时造成溺水伤害 + 灭火。
 */
public final class AquaEdgeArts extends SlashArts {

    private static final float AOE_RADIUS = 5.0F;
    private static final int DRIVE_COLOR = 0x0000FF; // 255

    public AquaEdgeArts(Function<LivingEntity, ResourceLocation> state) {
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

        // 游泳声
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_SWIM, SoundSource.PLAYERS, 1.0F, 1.5F);

        // 100 个水花粒子
        for (int i = 0; i < 100; i++) {
            double d0 = user.getRandom().nextGaussian() * 0.02;
            double d2 = user.getRandom().nextGaussian() * 0.02;
            double d3 = user.getRandom().nextGaussian() * 0.02;
            double ox = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d0 * 10.0) * 5.0;
            double oz = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d3 * 10.0) * 5.0;
            level.sendParticles(ParticleTypes.SPLASH,
                    user.getX() + ox, user.getY(), user.getZ() + oz,
                    1, d0, d2, d3, 0.0);
        }

        // 扑灭自身火焰
        if (user.isOnFire()) {
            user.clearFire();
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.7F,
                    1.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
        }

        // ── 服务端逻辑 ──────────────────────────────────────
        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float magicDamage = baseAttack * 0.27F;

        // AOE 斩击
        AABB box = user.getBoundingBox().inflate(AOE_RADIUS, 0.25, AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().mobAttack(user), baseAttack);
        }

        // 扇形射出水流飞刃
        int maxCol = 3;
        int maxCount = 3;  // rank=0 时，1.12.2 默认 3
        int halfCount = (int) Math.floor((double) maxCount / 2);

        for (int j = 0; j < maxCol; j++) {
            for (int i = 0; i < maxCount; i++) {
                double posY = user.getY() + user.getEyeHeight() / 2.0;

                // 1.12.2 定位: player.rotationYaw + 15 * (floor(maxCount/2)+1 - i)
                float yawOffset = (halfCount + 1 - i) * 15.0F;
                float spawnYaw = user.getYRot() + yawOffset;
                float yawRad = spawnYaw * ((float) Math.PI / 180.0F);

                Vec3 dir = user.getLookAngle();

                // 位置从玩家出发+偏航方向推 1 格
                double px = user.getX() - Math.sin(yawRad);
                double pz = user.getZ() + Math.cos(yawRad);

                AquaEdgeEntity aqua = AquaEdgeEntity.spawn(level,
                        user,
                        new Vec3(px, posY, pz),
                        dir,
                        magicDamage,
                        DRIVE_COLOR,
                        30 + 5 * j + i,  // lifetime
                        90.0F,            // roll
                        true              // multiHit
                );
                aqua.setInitialSpeed(0.1F);
                aqua.setNextSpeed(1.05F);
                aqua.setChangeTime(5 + 2 * j + i);
                aqua.setParticleEnabled(true);
                aqua.setParticleStyle("WATER_SPLASH");
                aqua.setSoundName("minecraft:entity.player.swim");
            }
        }

        return super.doArts(type, user);
    }
}

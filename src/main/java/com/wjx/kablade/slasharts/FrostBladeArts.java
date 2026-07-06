package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.FrostBladeEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 寒霜灵刃 —— 脉冲太刀 17 式与轩辕・脉冲太刀的专属 SA。
 * <p>
 * 依照崩坏 3 武器演示重制：六把冰蓝灵刃在使用者身侧贴地依次显形，
 * 带长尾高速刺向同一锁定目标；每次命中都会产生晶化爆闪，最后一剑更大、更亮。
 */
public final class FrostBladeArts extends SlashArts {

    private static final int SWORD_COUNT = 6;
    private static final int SWORD_COLOR = 0x20DDF4;
    private static final float ATTACK_FACTOR = 1.0F;
    private static final float DAMAGE_MULTIPLIER = 2.0F;
    private static final double SEEK_RANGE = 24.0D;

    /** 每把剑相对使用者的（右、前、上）偏移，形成参考画面中的贴地扇形队列。 */
    private static final double[][] SPAWN_OFFSETS = {
            {-0.95D, -0.65D, 0.18D},
            { 0.85D, -0.30D, 0.28D},
            {-1.55D,  0.10D, 0.16D},
            { 1.45D,  0.45D, 0.24D},
            {-0.55D,  0.85D, 0.34D},
            { 0.55D,  1.10D, 0.45D}
    };

    /** 前五剑保持紧凑节奏，终结剑稍作停顿，强化最后一下的重量。 */
    private static final int[] WAVE_DELAYS = {0, 2, 4, 6, 8, 11};

    public FrostBladeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR) * DAMAGE_MULTIPLIER;

        LivingEntity target = findTarget(level, user, blade);
        Vec3 fallbackDirection = target == null
                ? user.getLookAngle().normalize()
                : target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D)
                        .subtract(user.getEyePosition()).normalize();

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.45F);

        for (int i = 0; i < SWORD_COUNT; i++) {
            final int wave = i;
            final LivingEntity lockedTarget = target;
            SaFx.schedule(level, WAVE_DELAYS[i], () -> {
                if (!user.isAlive() || (lockedTarget != null && !lockedTarget.isAlive())) {
                    return;
                }
                spawnWave(level, user, lockedTarget, fallbackDirection, damage, wave);
            });
        }

        return super.doArts(type, user);
    }

    private static void spawnWave(ServerLevel level, LivingEntity user, LivingEntity target,
                                  Vec3 fallbackDirection, float damage, int wave) {
        // 出剑阵列只服从玩家朝向；锁定目标仅负责飞出后的追踪，不能反过来旋转玩家身边的阵列。
        Vec3 forward = SaFx.flatLook(user);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double[] offset = SPAWN_OFFSETS[wave];
        Vec3 spawn = user.position()
                .add(right.scale(offset[0]))
                .add(forward.scale(offset[1]))
                .add(0.0D, offset[2], 0.0D);

        boolean finisher = wave == SWORD_COUNT - 1;
        FrostBladeEntity.spawn(level, user, target, spawn, fallbackDirection,
                damage, SWORD_COLOR, finisher);

        level.playSound(null, spawn.x, spawn.y, spawn.z,
                SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS,
                finisher ? 0.78F : 0.48F, finisher ? 1.25F : 1.55F + wave * 0.025F);
    }

    private static LivingEntity findTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity living
                && SaTargeting.canDamage(user, living)
                && living.distanceToSqr(user) <= SEEK_RANGE * SEEK_RANGE) {
            return living;
        }

        Entity watched = SATool.getEntityToWatch(user);
        if (watched instanceof LivingEntity living
                && SaTargeting.canDamage(user, living)
                && living.distanceToSqr(user) <= SEEK_RANGE * SEEK_RANGE) {
            return living;
        }

        return level.getEntitiesOfClass(LivingEntity.class,
                        user.getBoundingBox().inflate(12.0D),
                        e -> SaTargeting.canDamage(user, e))
                .stream()
                .min(java.util.Comparator.comparingDouble(user::distanceToSqr))
                .orElse(null);
    }
}

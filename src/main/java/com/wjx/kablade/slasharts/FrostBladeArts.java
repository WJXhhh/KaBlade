package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 寒霜灵刃 —— 脉冲太刀17式专属 SA。
 * 从 1.12.2 {@code HonKaiFrostBlade} 移植而来。
 * <p>
 * 在玩家周围召唤 6 把寒霜召唤剑，自动追踪附近敌人（近的优先）；
 * 被锁定的目标施加缓慢 III（复刻 1.12.2 召唤剑命中后的霜冻减速）。附近无敌人时朝视线方向直射。
 */
public final class FrostBladeArts extends SlashArts {

    private static final int SWORD_COUNT = 6;
    /** 召唤剑颜色（1.12.2 原值 65518 = 0x00FFEE，青色）。 */
    private static final int SWORD_COLOR = 0x00FFEE;
    /** 攻击力补正系数（1.12.2 amplifierCalc(attack, 1)），对数补正。 */
    private static final float ATTACK_FACTOR = 1.0F;
    /** 寒霜减速：缓慢 III（1.12.2 SLOWNESS 60t、amplifier 2）。 */
    private static final int FROST_SLOW_DURATION = 60;
    private static final int FROST_SLOW_AMPLIFIER = 2;
    /** 召唤剑的追踪锁定范围。 */
    private static final double SEEK_RANGE = 12.0;

    public FrostBladeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        float damage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        // 锁定附近敌人，近的优先（复刻 1.12.2 召唤剑的自动追踪）。
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(SEEK_RANGE),
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        enemies.sort(Comparator.comparingDouble(user::distanceToSqr));

        Vec3 pos = user.position();
        Vec3 look = user.getLookAngle();
        for (int i = 0; i < SWORD_COUNT; i++) {
            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(
                    SlashBlade.RegistryEvents.SummonedSword, level);
            sword.setShooter(user);
            sword.setDamage(damage);
            sword.setColor(SWORD_COLOR);
            sword.setNoClip(true);

            // 在玩家周围随机位置生成（偏移 1:0 忠实复刻 1.12.2）
            double ox = (level.random.nextBoolean() ? 1 : 0) * level.random.nextDouble();
            double oy = level.random.nextDouble() / 2.0;
            double oz = (level.random.nextBoolean() ? 1 : 0) * level.random.nextDouble();
            Vec3 spawn = new Vec3(pos.x + ox, pos.y + user.getEyeHeight() + oy, pos.z + oz);
            sword.setPos(spawn.x, spawn.y, spawn.z);

            Vec3 dir;
            if (!enemies.isEmpty()) {
                // 多把剑按距离轮流分配目标；被锁定者吃缓慢 III（仅命中目标，贴近 1.12.2）。
                LivingEntity target = enemies.get(i % enemies.size());
                sword.setHitEntity(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        FROST_SLOW_DURATION, FROST_SLOW_AMPLIFIER));
                dir = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                        .subtract(spawn).normalize();
            } else {
                dir = look;
            }
            sword.shoot(dir.x, dir.y, dir.z, 1.5F, 0.0F);
            level.addFreshEntity(sword);
        }

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);

        return super.doArts(type, user);
    }
}

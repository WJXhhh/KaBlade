package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
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
 * 罪业之火 —— 复合刀·朱雀专属 SA。
 * 从 1.12.2 {@code HonKaiFireOfSin} 移植而来。
 * <p>
 * 在玩家眼部高度生成一道大范围（4 格）的 EntityDrive（90 tick 寿命），
 * 同时点燃周围 4 格内的所有生物 4 秒，并喷发熔岩滴粒子。
 */
public final class FireOfSinArts extends SlashArts {

    /** 基础伤害（1.12.2 基础值 10；额外伤害 = amplifierCalc(attack, 3)，对数补正）。 */
    private static final float BASE_DAMAGE = 1.65F;
    private static final float ATTACK_FACTOR = 0.5F;
    /** Drive 寿命（tick）。 */
    private static final int DRIVE_LIFETIME = 90;
    private static final float DRIVE_SPEED = 1.5F;
    /** 点燃时间（秒）。 */
    private static final int FIRE_SECONDS = 1;
    /** 点燃范围（格）。 */
    private static final double IGNITE_RANGE = 4.0;
    /** 火焰刀光颜色（橙红）。 */
    private static final int DRIVE_COLOR = 0xFF6A00;

    public FireOfSinArts(Function<LivingEntity, ResourceLocation> state) {
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

        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        double damage = BASE_DAMAGE + extraDamage;

        // 生成火焰 Drive
        spawnFireDrive(level, user, damage);

        // 点燃周围生物
        igniteNearby(level, user);

        // 熔岩滴粒子
        spawnLavaParticles(level, user);

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 0.8F);

        return super.doArts(type, user);
    }

    /** 在玩家眼部高度生成一道大范围火焰 Drive。 */
    private static void spawnFireDrive(ServerLevel level, LivingEntity user, double damage) {
        EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
        drive.setShooter(user);
        drive.setDamage(damage);
        drive.setColor(DRIVE_COLOR);
        drive.setLifetime(DRIVE_LIFETIME);
        drive.setPos(user.getX(), user.getY() + user.getEyeHeight(), user.getZ());
        Vec3 look = user.getLookAngle();
        drive.shoot(look.x, look.y, look.z, DRIVE_SPEED, 0.0F);
        level.addFreshEntity(drive);
    }

    /** 点燃周围 4 格内的所有非友方生物 4 秒。 */
    private static void igniteNearby(ServerLevel level, LivingEntity user) {
        Vec3 pos = user.position();
        AABB box = new AABB(
                pos.x - IGNITE_RANGE, pos.y - 1.0, pos.z - IGNITE_RANGE,
                pos.x + IGNITE_RANGE, pos.y + 2.0, pos.z + IGNITE_RANGE);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));

        for (LivingEntity target : targets) {
            target.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    /** 20 个熔岩滴粒子，随机散布在玩家周围。 */
    private static void spawnLavaParticles(ServerLevel level, LivingEntity user) {
        for (int i = 0; i < 20; i++) {
            double ox = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble() * 2.0;
            double oy = level.random.nextDouble() * user.getBbHeight() / 2.0;
            double oz = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble() * 2.0;
            level.sendParticles(ParticleTypes.DRIPPING_LAVA,
                    user.getX() + ox, user.getY() + oy, user.getZ() + oz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}

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
 * 龙一文字线 SA「苍流刃」。
 * <p>
 * 从 1.12.2 {@code AquaEdgeEx} 简化移植：
 * 扑灭使用者身上的火焰，对周围敌人造成斩击伤害，并向四周射出蓝色水流飞斩。
 */
public final class AquaEdgeArts extends SlashArts {

    private static final float BASE_DAMAGE = 9.0F;
    private static final float ATTACK_FACTOR = 4.5F;
    private static final float AOE_RADIUS = 5.0F;
    private static final int DRIVE_COUNT = 10;
    private static final int DRIVE_COLOR = 0x00BFFF;

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
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_SWIM, SoundSource.PLAYERS, 1.0F, 1.5F);

        // 扑灭使用者火焰
        if (user.isOnFire()) {
            user.clearFire();
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.7F, 1.6F);
        }

        // AOE 伤害
        AABB box = user.getBoundingBox().inflate(AOE_RADIUS, 0.25, AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().mobAttack(user), damage);
        }

        // 径向水流飞斩
        Vec3 center = new Vec3(user.getX(), user.getY() + user.getEyeHeight() / 2.0, user.getZ());
        for (int i = 0; i < DRIVE_COUNT; i++) {
            float yaw = (float) (i * (2.0 * Math.PI) / DRIVE_COUNT);
            Vec3 dir = new Vec3(-Math.sin(yaw), 0.05, Math.cos(yaw));
            EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
            drive.setPos(center.x, center.y, center.z);
            drive.setShooter(user);
            drive.setDamage(damage * 0.4);
            drive.setColor(DRIVE_COLOR);
            drive.setLifetime(25);
            drive.shoot(dir.x, dir.y, dir.z, 0.85F, 0.1F);
            level.addFreshEntity(drive);
        }

        // 水花粒子
        for (int i = 0; i < 50; i++) {
            double ox = (level.random.nextDouble() - 0.5) * user.getBbWidth() * 4.0;
            double oz = (level.random.nextDouble() - 0.5) * user.getBbWidth() * 4.0;
            level.sendParticles(ParticleTypes.SPLASH,
                    user.getX() + ox, user.getY(), user.getZ() + oz,
                    1, 0.0, 0.05, 0.0, 0.0);
        }

        return super.doArts(type, user);
    }
}

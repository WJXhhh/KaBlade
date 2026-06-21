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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 龙一文字线 SA「超越斩」。
 * <p>
 * 从 1.12.2 {@code OverSlash} 简化移植：
 * 以玩家为中心对周围 5 格敌人造成一次斩击伤害，并向四周射出 6 道紫色飞斩。
 */
public final class OverSlashArts extends SlashArts {

    private static final float BASE_DAMAGE = 8.0F;
    private static final float ATTACK_FACTOR = 4.0F;
    private static final float AOE_RADIUS = 5.0F;
    private static final int DRIVE_COUNT = 6;
    private static final int DRIVE_COLOR = 0x9932CC;

    public OverSlashArts(Function<LivingEntity, ResourceLocation> state) {
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
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.8F);

        // AOE 伤害
        AABB box = user.getBoundingBox().inflate(AOE_RADIUS, 0.25, AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().mobAttack(user), damage);
        }

        //  radial drives
        Vec3 center = new Vec3(user.getX(), user.getY() + user.getEyeHeight() / 2.0, user.getZ());
        for (int i = 0; i < DRIVE_COUNT; i++) {
            float yaw = (float) (i * (2.0 * Math.PI) / DRIVE_COUNT);
            Vec3 dir = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
            drive.setPos(center.x, center.y, center.z);
            drive.setShooter(user);
            drive.setDamage(damage * 0.5);
            drive.setColor(DRIVE_COLOR);
            drive.setLifetime(15);
            drive.shoot(dir.x, dir.y, dir.z, 0.6F, 0.0F);
            level.addFreshEntity(drive);
        }

        spawnParticles(level, user);
        return super.doArts(type, user);
    }

    private static void spawnParticles(ServerLevel level, LivingEntity user) {
        for (int i = 0; i < 20; i++) {
            double ox = (level.random.nextDouble() - 0.5) * user.getBbWidth() * 2.0;
            double oz = (level.random.nextDouble() - 0.5) * user.getBbWidth() * 2.0;
            level.sendParticles(ParticleTypes.WITCH,
                    user.getX() + ox, user.getY() + user.getBbHeight() / 2.0, user.getZ() + oz,
                    1, 0.0, 0.02, 0.0, 0.0);
        }
    }
}

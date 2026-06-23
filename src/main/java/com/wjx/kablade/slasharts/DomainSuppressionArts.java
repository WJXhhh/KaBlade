package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ConfinementForceFieldEntity;
import com.wjx.kablade.entity.OriginFreeSwordEntity;
import com.wjx.kablade.specialeffect.Oripursuit;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

/**
 * 领域压杀 —— 源能刃「碎钢」专属 SA。
 * 从 1.12.2 {@code SaDomainSuppression} 移植而来。
 * <p>
 * 射线锁定前方 10 格内的最近敌人，在目标周围召唤多圈召唤剑围杀，
 * 最后落下闪电轰击。召唤剑颜色为青色（0x00FFFF）。
 * <ul>
 *   <li>内圈（r=2）：6 把高伤害召唤剑</li>
 *   <li>外圈（r=6）：6 把中伤害召唤剑</li>
 *   <li>地面圈（r=6）：10 把自由落体召唤剑</li>
 *   <li>终结：6 道闪电 + 大爆炸粒子</li>
 * </ul>
 */
public final class DomainSuppressionArts extends SlashArts {

    private static final double RAY_DISTANCE = 10.0;
    private static final int SWORD_COLOR = 0x00FFFF;
    private static final int SOFT_COLOR = 0xC8FBFF;
    private static final int DOMAIN_FIELD_LIFETIME = 34;

    private static final float INNER_DAMAGE_BASE = 10.0F;
    private static final float OUTER_DAMAGE_BASE = 4.0F;
    private static final float GROUND_DAMAGE_BASE = 3.0F;
    private static final int FREE_SWORD_DELAY = 7;
    private static final int FREE_SWORD_LIFETIME = 24;
    private static final Vector3f CYAN = rgb(SWORD_COLOR);
    private static final Vector3f PALE_CYAN = rgb(SOFT_COLOR);

    public DomainSuppressionArts(Function<LivingEntity, ResourceLocation> state) {
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

        // 射线锁定
        LivingEntity target = raycastTarget(level, user);
        if (target == null) {
            return super.doArts(type, user);
        }
        if (user instanceof Player player) {
            Oripursuit.lockTarget(player, target);
        } else {
            blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setTargetEntityId(target));
        }

        float extraDamage = MathFunc.amplifierCalc(bladeAttack, 3.0F);

        // 目标上浮
        target.setDeltaMovement(target.getDeltaMovement().add(0, 0.5, 0));
        target.hurtMarked = true;

        final double tx = target.getX();
        final double tz = target.getZ();
        final double groundY = target.getY() + 0.02;
        final double midY = target.getY() + target.getEyeHeight();
        final double topY = target.getY() + target.getEyeHeight() + 1.0;
        final float outerDamage = OUTER_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, 2.0F);
        final float freeDamage = GROUND_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, 2.0F);

        ConfinementForceFieldEntity field = new ConfinementForceFieldEntity(level, user);
        field.setPos(tx, groundY, tz);
        field.setLifetimeTicks(DOMAIN_FIELD_LIFETIME);
        level.addFreshEntity(field);

        spawnDomainClouds(level, tx, target.getY(), tz);
        spawnDomainPulse(level, tx, groundY, tz, 6.0, 28, 0.0F);
        spawnVerticalCurtain(level, tx, target.getY() + 0.35, tz, 6.0, 10, 2.8, 0.0F);
        level.sendParticles(ParticleTypes.FLASH, tx, target.getY() + 1.35, tz, 1, 0, 0, 0, 0);
        level.playSound(null, tx, target.getY(), tz, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 0.7F);

        SaFx.schedule(level, 3, () -> {
            if (!target.isAlive()) {
                return;
            }
            spawnSwordCircle(level, user, tx, topY, tz, 2.0, 6, INNER_DAMAGE_BASE + extraDamage, 0);
            spawnDomainPulse(level, tx, midY, tz, 2.2, 16, 30.0F);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, tx, midY, tz, 14, 0.4, 0.7, 0.4, 0.03);
            level.playSound(null, tx, midY, tz, SoundEvents.ALLAY_HURT, SoundSource.PLAYERS, 0.85F, 0.55F);
        });

        SaFx.schedule(level, 8, () -> {
            if (!target.isAlive()) {
                return;
            }
            spawnSwordCircle(level, user, tx, topY, tz, 6.0, 6, outerDamage, 30);
            spawnDomainPulse(level, tx, topY, tz, 6.1, 22, -18.0F);
            spawnVerticalCurtain(level, tx, target.getY() + 0.45, tz, 6.0, 6, 2.5, 30.0F);
            level.playSound(null, tx, midY, tz, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8F, 1.35F);
        });

        SaFx.schedule(level, 12, () -> {
            if (!target.isAlive()) {
                return;
            }
            spawnFreeSwordCircle(level, user, tx, midY, tz, 6.0, 10, freeDamage, 0);
            spawnVerticalCurtain(level, tx, target.getY() + 0.2, tz, 6.0, 10, 3.2, 18.0F);
            level.sendParticles(ParticleTypes.END_ROD, tx, midY, tz, 18, 0.65, 1.0, 0.65, 0.02);
            level.playSound(null, tx, midY, tz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.75F, 1.2F);
        });

        SaFx.schedule(level, 18, () -> {
            if (!target.isAlive()) {
                return;
            }
            spawnCollapseSpark(level, tx, target.getY() + 0.15, tz, 6.2, 16);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, tx, target.getY() + 1.1, tz, 24, 0.8, 0.9, 0.8, 0.06);
            level.playSound(null, tx, target.getY(), tz, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.75F, 0.65F);
        });

        SaFx.schedule(level, 24, () -> {
            if (!target.isAlive()) {
                return;
            }
            finishDomain(level, tx, target.getY(), tz);
        });

        return super.doArts(type, user);
    }

    /**
     * 在目标周围生成一圈召唤剑。
     */
    private void spawnSwordCircle(ServerLevel level, LivingEntity user,
                                  double cx, double cy, double cz,
                                  double radius, int count, float damage, float angleOffset) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(angleOffset + (360.0 / count) * i);
            double px = cx + Math.cos(angle) * radius;
            double py = cy;
            double pz = cz + Math.sin(angle) * radius;

            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(
                    SlashBlade.RegistryEvents.SummonedSword, level);
            sword.setShooter(user);
            sword.setDamage(damage);
            sword.setPos(px, py, pz);
            sword.setColor(SWORD_COLOR);
            // 朝向目标中心
            double dx = cx - px;
            double dy = cy - py;
            double dz = cz - pz;
            sword.shoot(dx, dy, dz, 0.8F, 0);
            level.addFreshEntity(sword);
        }
    }

    /**
     * 复刻 1.12.2 的 EntitySummonSwordFree：从外圈延迟启动，垂直向上射出并在结束时爆裂。
     */
    private void spawnFreeSwordCircle(ServerLevel level, LivingEntity user,
                                      double cx, double cy, double cz,
                                      double radius, int count, float damage, float angleOffset) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(angleOffset + (360.0 / count) * i);
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            Vec3 dir = new Vec3(0.0, 1.0, 0.0);
            OriginFreeSwordEntity.spawn(level, user, px, cy, pz, dir, damage, SWORD_COLOR,
                    FREE_SWORD_DELAY, FREE_SWORD_LIFETIME, (float) (i * 36.0));
        }
    }

    private void spawnDomainClouds(ServerLevel level, double x, double y, double z) {
        for (int ring = 0; ring < 4; ring++) {
            double r = 1.0 + ring * 0.5;
            float speed = 0.5F - ring * 0.1F;
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18.0);
                double px = x + Math.cos(angle) * r;
                double pz = z + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.CLOUD, px, y + 0.1, pz,
                        1, Math.cos(angle) * speed, 0, Math.sin(angle) * speed, 0);
            }
        }

        for (int ring = 0; ring < 4; ring++) {
            double r = 1.0 + ring * 0.5;
            float speed = 0.5F - ring * 0.1F;
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18.0 + 90);
                double px = x + Math.cos(angle) * r;
                double pz = z + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.CLOUD, px, y + 2.0, pz,
                        1, Math.cos(angle) * speed, 0, Math.sin(angle) * speed, 0);
            }
        }
    }

    private void spawnDomainPulse(ServerLevel level, double x, double y, double z,
                                  double radius, int count, float angleOffset) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(angleOffset + (360.0 / count) * i);
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            level.sendParticles(new DustColorTransitionOptions(PALE_CYAN, CYAN, 1.2F),
                    px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, px, y + 0.02, pz,
                    1, Math.cos(angle) * 0.05, 0.0, Math.sin(angle) * 0.05, 0.0);
        }
    }

    private void spawnVerticalCurtain(ServerLevel level, double x, double y, double z,
                                      double radius, int columns, double height, float angleOffset) {
        for (int i = 0; i < columns; i++) {
            double angle = Math.toRadians(angleOffset + (360.0 / columns) * i);
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            for (int j = 0; j < 5; j++) {
                double py = y + height * (j / 4.0);
                level.sendParticles(new DustParticleOptions(CYAN, 0.9F),
                        px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private void spawnCollapseSpark(ServerLevel level, double x, double y, double z,
                                    double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians((360.0 / count) * i);
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double dx = (x - px) * 0.1;
            double dz = (z - pz) * 0.1;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, y + 0.2, pz,
                    2, dx, 0.02, dz, 0.0);
        }
    }

    private void finishDomain(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FLASH, x, y + 1.1, z, 1, 0, 0, 0, 0);
            net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(x, y, z);
                level.addFreshEntity(bolt);
            }
        }
        level.playSound(null, x, y, z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static Vector3f rgb(int color) {
        return new Vector3f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
    }

    /**
     * 锁定前方的 LivingEntity。
     * <p>
     * 与 1.12.2 多数 SA 的锁定策略一致：先做精确射线（瞄得准时命中准星所指的敌人），
     * 锁空时回退到 {@link SATool#getEntityToWatch}——取身前最近的可攻击实体，
     * 不要求准星正对，避免准星稍偏就完全锁不到导致 SA 看似“放不出来”。
     */
    private LivingEntity raycastTarget(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(2.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, e -> e != user && e.isAlive() && e.isPickable());

        // 1) 精确射线优先：碰撞箱略放宽 0.3 增加容差。
        LivingEntity closest = null;
        double closestDist = RAY_DISTANCE;
        for (LivingEntity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(candidate.getPickRadius() + 0.3);
            var hit = bb.clip(eye, end);
            if (bb.contains(eye)) {
                return candidate;
            } else if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < closestDist) {
                    closest = candidate;
                    closestDist = dist;
                }
            }
        }
        if (closest != null) {
            return closest;
        }

        // 2) 回退：身前最近的可攻击实体（与 1.12.2 getEntityToWatch 一致）。
        if (SATool.getEntityToWatch(user) instanceof LivingEntity watched) {
            return watched;
        }
        return null;
    }
}

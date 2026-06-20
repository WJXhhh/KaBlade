package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.OriginFreeSwordEntity;
import com.wjx.kablade.specialeffect.Oripursuit;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
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

    private static final float INNER_DAMAGE_BASE = 10.0F;
    private static final float OUTER_DAMAGE_BASE = 4.0F;
    private static final float GROUND_DAMAGE_BASE = 3.0F;
    private static final int FREE_SWORD_DELAY = 7;
    private static final int FREE_SWORD_LIFETIME = 24;

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

        double tx = target.getX();
        double ty = target.getY() + target.getEyeHeight() + 1.0;
        double tz = target.getZ();

        // 内圈：6 把高伤害召唤剑（r=2）
        spawnSwordCircle(level, user, tx, ty, tz, 2.0, 6,
                INNER_DAMAGE_BASE + extraDamage, 0);

        // 外圈：6 把中伤害召唤剑（r=6）
        spawnSwordCircle(level, user, tx, ty, tz, 6.0, 6,
                OUTER_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, 2.0F), 30);

        // 源能自由剑：10 把自由剑在目标周围成环，延迟后垂直上射。
        double groundY = target.getY() + target.getEyeHeight();
        spawnFreeSwordCircle(level, user, tx, groundY, tz, 6.0, 10,
                GROUND_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, 2.0F), 0);

        // 地面云粒子
        for (int ring = 0; ring < 4; ring++) {
            double r = 1.0 + ring * 0.5;
            float speed = 0.5F - ring * 0.1F;
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18.0);
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.CLOUD, px, target.getY() + 0.1, pz,
                        1, Math.cos(angle) * speed, 0, Math.sin(angle) * speed, 0);
            }
        }

        // 高空云粒子
        for (int ring = 0; ring < 4; ring++) {
            double r = 1.0 + ring * 0.5;
            float speed = 0.5F - ring * 0.1F;
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18.0 + 90);
                double px = tx + Math.cos(angle) * r;
                double pz = tz + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.CLOUD, px, target.getY() + 2.0, pz,
                        1, Math.cos(angle) * speed, 0, Math.sin(angle) * speed, 0);
            }
        }

        // 闪电终结
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tx, target.getY(), tz,
                    1, 0, 0, 0, 0);
            net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(tx, target.getY(), tz);
                level.addFreshEntity(bolt);
            }
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);

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

    /**
     * 射线锁定前方最近的 LivingEntity。
     */
    private LivingEntity raycastTarget(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, e -> e != user && e.isAlive() && e.isPickable());

        LivingEntity closest = null;
        double closestDist = RAY_DISTANCE;

        for (LivingEntity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hit = bb.clip(eye, end);
            if (bb.contains(eye)) {
                closest = candidate;
                closestDist = 0;
                break;
            } else if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < closestDist) {
                    closest = candidate;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }
}

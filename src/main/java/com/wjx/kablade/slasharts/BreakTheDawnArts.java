package com.wjx.kablade.slasharts;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 弧光破晓 —— 弧光刃「流芒」专属 SA。
 * <p>
 * 从 1.12.2 的 {@code SaBreakTheDawn} 移植而来。
 * 爆开 5 道淡金色幻影刃 + 召唤 5 把环绕玩家的幻影剑自动索敌，
 * 同时给周围敌对生物施加发光效果，自身获得夜视。
 * <p>
 * 强度：
 * <ul>
 *   <li>幻影刃 ×5：伤害 = 3 + 刀攻击力 × 0.5，向前飞出</li>
 *   <li>幻影剑 ×5：伤害 = 5 + 刀攻击力 × 0.5，环绕玩家自动索敌射击</li>
 *   <li>发光范围 10 格，持续 120s</li>
 *   <li>夜视持续 80s</li>
 * </ul>
 */
public final class BreakTheDawnArts extends SlashArts {

    /** 幻影刃/幻影剑数量（与 1.12.2 一致）。 */
    private static final int COUNT = 5;
    /** 幻影刃伤害基准值。 */
    private static final float BASE_DRIVE_DAMAGE = 3.0F;
    /** 幻影剑伤害基准值。 */
    private static final float BASE_SWORD_DAMAGE = 5.0F;
    /** 伤害：刀攻击力倍率。 */
    private static final float ATTACK_RATIO = 0.5F;
    /** 幻影刃速度。 */
    private static final float DRIVE_SPEED = 1.5F;
    /** 发光范围半径（格）。 */
    private static final double GLOW_RADIUS = 10.0;
    /** 发光时长（tick，120s）。 */
    private static final int GLOW_DURATION = 2400;
    /** 夜视时长（tick，80s）。 */
    private static final int NIGHT_VISION_DURATION = 1600;
    /** 召唤剑颜色（淡金色）。 */
    private static final int SWORD_COLOR = 0xfff8ca;

    public BreakTheDawnArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }

        // 只有右键蓄力成功才触发，蓄力不足（Fail）不生效
        if (type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        final ServerLevel level = (ServerLevel) user.level();
        final ItemStack stack = user.getMainHandItem();
        final float bladeAttack = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        final float driveDamage = BASE_DRIVE_DAMAGE + bladeAttack * ATTACK_RATIO;
        final float swordDamage = BASE_SWORD_DAMAGE + bladeAttack * ATTACK_RATIO;

        // —— 1. 爆开 5 道幻影刃 ——
        // 与 1.12.2 一致：全部朝视线方向飞出（无扇形偏移）
        final Vec3 look = user.getLookAngle();

        for (int i = 0; i < COUNT; i++) {
            EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
            drive.setPos(
                    user.getX() + look.x * 0.5,
                    user.getY() + user.getEyeHeight() * 0.6,
                    user.getZ() + look.z * 0.5
            );
            drive.setShooter(user);
            drive.setDamage(driveDamage);
            drive.setColor(SWORD_COLOR);
            drive.shoot(look.x, look.y + 0.05, look.z, DRIVE_SPEED, 0.5F);
            level.addFreshEntity(drive);
        }

        // —— 2. 召唤 5 把幻影剑，左右两侧各一组 ——
        // 与 1.12.2 一致：分列玩家左右，全部朝向玩家面向方向
        // faceEntityStandby 会根据 delay 值自动计算骑乘偏移量，
        // 用不同 delay 让左右侧幻影剑分布在对应位置
        for (int i = 0; i < COUNT; i++) {
            // delay 控制 faceEntityStandby 中左右（奇偶）和远近（数值），
            // 递增的 delay 让剑在玩家两侧依次排开，间距更大
            int delay = i * 3;

            EntityBlisteringSwords sword = new EntityBlisteringSwords(
                    SlashBlade.RegistryEvents.BlisteringSwords, level);
            sword.setPos(user.getX(), user.getY() + 1.2, user.getZ());
            sword.setShooter(user);
            sword.setDamage(swordDamage);
            sword.setColor(SWORD_COLOR);
            sword.setDelay(delay);
            level.addFreshEntity(sword);
            sword.startRiding(user, true);
        }

        // —— 3. 给周围敌对生物加发光 ——
        AABB glowBox = user.getBoundingBox().inflate(GLOW_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, glowBox,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 2));
        }

        // —— 3. 自身夜视 ——
        if (user instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 2));
        }

        // —— 4. 烟花粒子特效 ——
        for (int i = 0; i < 40; i++) {
            double px = user.getX() + (level.random.nextDouble() - 0.5) * 3.0;
            double py = user.getY() + level.random.nextDouble();
            double pz = user.getZ() + (level.random.nextDouble() - 0.5) * 3.0;
            level.sendParticles(ParticleTypes.FIREWORK, px, py, pz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        return super.doArts(type, user);
    }
}

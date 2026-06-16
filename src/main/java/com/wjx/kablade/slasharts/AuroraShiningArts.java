package com.wjx.kablade.slasharts;

import com.wjx.kablade.event.AuroraColorCycling;
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

import java.util.List;
import java.util.function.Function;

/**
 * 极光闪耀 —— 极光刃「映天」专属 SA。
 * <p>
 * 从 1.12.2 的 {@code SaAuroraShining} 移植而来。
 * 召唤 10 道极光幻影刃 + 10 把极光幻影剑，颜色随极光色谱随机变化；
 * 周围敌对生物施加发光 + 无视护甲伤害 + 着火，自身获得夜视。
 * <p>
 * 强度：
 * <ul>
 *   <li>幻影刃 ×10：伤害 = 4 + 刀攻击力 × 0.5，向前飞出</li>
 *   <li>幻影剑 ×10：伤害 = 6 + 刀攻击力 × 0.5，环绕玩家自动索敌</li>
 *   <li>无视护甲伤害 8 + 刀攻击力 × 0.4，范围 20 格</li>
 *   <li>发光 120s、着火 5s、夜视 120s</li>
 * </ul>
 */
public final class AuroraShiningArts extends SlashArts {

    /** 幻影刃/幻影剑数量。 */
    private static final int COUNT = 10;
    /** 幻影刃伤害基准值。 */
    private static final float BASE_DRIVE_DAMAGE = 4.0F;
    /** 幻影剑伤害基准值。 */
    private static final float BASE_SWORD_DAMAGE = 6.0F;
    /** 伤害：刀攻击力倍率。 */
    private static final float ATTACK_RATIO = 0.5F;
    /** 无视护甲伤害基准值。 */
    private static final float BYPASS_BASE_DAMAGE = 8.0F;
    /** 无视护甲伤害：刀攻击力倍率。 */
    private static final float BYPASS_ATTACK_RATIO = 0.4F;
    /** 幻影刃速度。 */
    private static final float DRIVE_SPEED = 1.5F;
    /** 极光伤害范围半径（格）。 */
    private static final double AURORA_RADIUS = 20.0;
    /** 发光时长（tick，120s）。 */
    private static final int GLOW_DURATION = 2400;
    /** 夜视时长（tick，120s）。 */
    private static final int NIGHT_VISION_DURATION = 2400;
    /** 着火时长（秒）。 */
    private static final int FIRE_SECONDS = 5;


    public AuroraShiningArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }

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
        final float bypassDamage = BYPASS_BASE_DAMAGE + bladeAttack * BYPASS_ATTACK_RATIO;

        // —— 1. 召唤 10 道极光幻影刃 ——
        for (int i = 0; i < COUNT; i++) {
            EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
            drive.setPos(
                    user.getX() + user.getLookAngle().x * 0.5,
                    user.getY() + user.getEyeHeight() * 0.6,
                    user.getZ() + user.getLookAngle().z * 0.5
            );
            drive.setShooter(user);
            drive.setDamage(driveDamage);
            drive.setColor(AuroraColorCycling.getRandomColor(level.random));
            drive.shoot(user.getLookAngle().x, user.getLookAngle().y + 0.05,
                    user.getLookAngle().z, DRIVE_SPEED, 0.5F);
            level.addFreshEntity(drive);
        }

        // —— 2. 召唤 10 把极光幻影剑 ——
        for (int i = 0; i < COUNT; i++) {
            int delay = i * 3;
            EntityBlisteringSwords sword = new EntityBlisteringSwords(
                    SlashBlade.RegistryEvents.BlisteringSwords, level);
            sword.setPos(user.getX(), user.getY() + 1.2, user.getZ());
            sword.setShooter(user);
            sword.setDamage(swordDamage);
            sword.setColor(AuroraColorCycling.getRandomColor(level.random));
            sword.setDelay(delay);
            level.addFreshEntity(sword);
            sword.startRiding(user, true);
        }

        // —— 3. 极光闪耀：周围敌对生物发光 + 无视护甲伤害 + 着火 ——
        AABB auroraBox = user.getBoundingBox().inflate(AURORA_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, auroraBox,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 2));
            target.hurt(level.damageSources().magic(), bypassDamage);
            target.setSecondsOnFire(FIRE_SECONDS);
        }

        // —— 4. 自身夜视 ——
        if (user instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 2));
        }

        // —— 5. 极光烟花粒子特效 ——
        for (int i = 0; i < 80; i++) {
            double px = user.getX() + (level.random.nextDouble() - 0.5) * 6.0;
            double py = user.getY() + level.random.nextDouble() * 2.0;
            double pz = user.getZ() + (level.random.nextDouble() - 0.5) * 6.0;
            level.sendParticles(ParticleTypes.FIREWORK, px, py, pz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        return super.doArts(type, user);
    }

}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/**
 * 撕裂灵刃 —— 妖刀村正专属 SA。
 * 从 1.12.2 {@code HonkaiLacerateBlade} 移植而来。
 * <p>
 * 在玩家身周随机位置召唤一道放大的暗红 EntityDrive，持续 90 tick 缓慢飘移切割；
 * 同时对正在攻击玩家的敌人施加凋零 II 5 秒。
 */
public final class LacerateBladeArts extends SlashArts {

    /** 基础伤害（1.12.2 基础值 10；额外伤害 = amplifierCalc(attack, 3)，对数补正）。 */
    private static final float BASE_DAMAGE = 10.0F;
    private static final float ATTACK_FACTOR = 3.0F;
    /** Drive 寿命（tick）。 */
    private static final int DRIVE_LIFETIME = 90;
    /** 凋零持续时间（tick）。 */
    private static final int WITHER_DURATION = 100;
    /** 凋零等级（0-based，即凋零 II）。 */
    private static final int WITHER_AMPLIFIER = 1;
    /** 暗红刀光颜色（RGB 0xCC3344，对应 1.12.2 的 0.8/0.2/0.4）。 */
    private static final int DRIVE_COLOR = 0xCC3344;
    /** Drive 放大倍率（1.12.2 SCALE_Y=2）。 */
    private static final float DRIVE_SIZE = 2.0F;

    public LacerateBladeArts(Function<LivingEntity, ResourceLocation> state) {
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

        // 在玩家身周随机位置生成暗红放大刀光
        spawnLacerateDrive(level, user, damage);

        // 对正在攻击玩家的敌人施加凋零 II
        LivingEntity attacker = user.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, WITHER_AMPLIFIER, false, false));
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);

        return super.doArts(type, user);
    }

    /**
     * 在玩家身周随机位置召唤一道放大的暗红 EntityDrive。
     * 复刻 1.12.2 的随机偏移 + 慢速飘移 + 90 tick 寿命。
     */
    private static void spawnLacerateDrive(ServerLevel level, LivingEntity user, double damage) {
        EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
        drive.setShooter(user);
        drive.setDamage(damage);
        drive.setColor(DRIVE_COLOR);
        drive.setBaseSize(DRIVE_SIZE);
        drive.setLifetime(DRIVE_LIFETIME);

        // 随机角度 + 随机偏移（1.12.2：rand(25)*0.1 水平 + rand(13) 垂直 + 眼高/2）
        float angle = level.getRandom().nextFloat() * 360.0F;
        double offX = user.getX() + level.getRandom().nextInt(25) * 0.1;
        double offY = user.getY() + level.getRandom().nextInt(13) + user.getEyeHeight() / 2.0;
        double offZ = user.getZ() + level.getRandom().nextInt(25) * 0.1;
        drive.setPos(offX, offY, offZ);
        drive.setYRot(angle);
        drive.setXRot(0.0F);

        // 慢速飘移（1.12.2 initialSpeed=0.1+0.001f）
        double rad = Math.toRadians(angle);
        double speed = 0.101F;
        drive.setDeltaMovement(-Math.sin(rad) * speed, 0.0, Math.cos(rad) * speed);

        level.addFreshEntity(drive);
    }
}

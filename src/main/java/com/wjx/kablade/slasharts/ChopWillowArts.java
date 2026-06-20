package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 锋刀抚柳 —— 复合刀·柳叶专属 SA。
 * 从 1.12.2 {@code HonKaiChopWillow} 移植并简化而来。
 * <p>
 * 原版实现：SA 给玩家 Y 速度 +1.1 弹起，再通过 entityData 标志位
 * （{@code to_chop_willow}/{@code start_chop_willow}/{@code chop_willow_retry_count}）
 * 在 {@code WorldEvent} 每 tick 轮询，倒计时 10 tick 后对周围 4×2×4 范围结算伤害；
 * 若未命中则每 tick 重试，最多 40 次。
 * <p>
 * 1.20.1 简化：把跨 tick 状态机收敛进 SA 类内部，用 {@link TickTask} 调度，
 * 不再依赖 entityData 标志位或全局事件监听。行为等价：
 * <ol>
 *   <li>玩家弹起（Y 速度 +1.1）</li>
 *   <li>10 tick 后 AABB 扫描周围 4×2×4 范围，对每个生物造成 {@code 4 + min(attack, 4)} 伤害</li>
 *   <li>若未命中且重试次数 &lt; 40，则下一 tick 再结算；否则结束</li>
 * </ol>
 */
public final class ChopWillowArts extends SlashArts {

    /** 弹起 Y 速度（1.12.2 原值）。 */
    private static final double LAUNCH_Y = 1.1;
    /** 倒计时 tick 数（1.12.2 原值 10）。 */
    private static final int DELAY_TICKS = 10;
    /** 范围伤害基础值。 */
    private static final float BASE_DAMAGE = 4.0F;
    /** 攻击力补正系数：extraDamage = amplifierCalc(attack, 4)，对数补正。 */
    private static final float ATTACK_FACTOR = 4.0F;
    /** AABB 范围（格）。 */
    private static final double RANGE_XZ = 4.0;
    private static final double RANGE_Y = 2.0;
    /** 最大重试次数（1.12.2 原值 40）。 */
    private static final int MAX_RETRY = 40;

    public ChopWillowArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        // 弹起
        user.setDeltaMovement(user.getDeltaMovement().x, LAUNCH_Y, user.getDeltaMovement().z);
        user.hurtMarked = true;

        final ServerLevel level = (ServerLevel) user.level();
        final MinecraftServer server = level.getServer();
        if (server == null) {
            return super.doArts(type, user);
        }

        // 10 tick 后开始结算
        server.tell(new TickTask(server.getTickCount() + DELAY_TICKS,
                () -> chopWillowTask(server, level, user, 0)));

        return super.doArts(type, user);
    }

    /**
     * 范围斩击结算任务。
     * 命中敌人则一次结算完毕；未命中则下一 tick 重试，最多 {@link #MAX_RETRY} 次。
     */
    private static void chopWillowTask(MinecraftServer server, ServerLevel level, LivingEntity user, int retry) {
        if (!user.isAlive() || retry > MAX_RETRY) {
            return;
        }

        ItemStack blade = user.getMainHandItem();
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        float damage = BASE_DAMAGE + extraDamage;

        Vec3 pos = user.position();
        AABB box = new AABB(
                pos.x - RANGE_XZ, pos.y - RANGE_Y, pos.z - RANGE_XZ,
                pos.x + RANGE_XZ, pos.y + RANGE_Y, pos.z + RANGE_XZ);

        DamageSource src = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user));

        if (targets.isEmpty() && retry < MAX_RETRY) {
            // 未命中，下一 tick 重试
            server.tell(new TickTask(server.getTickCount() + 1,
                    () -> chopWillowTask(server, level, user, retry + 1)));
            return;
        }

        // 结算伤害
        for (LivingEntity target : targets) {
            target.hurt(src, damage);
        }
        // 命中才扣一次耐久（无论命中几个目标，与 1.12.2 行为对齐）
        if (!targets.isEmpty()) {
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        // 落地爆裂粒子
        for (int i = 0; i < 10; i++) {
            double ox = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble();
            double oz = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble();
            level.sendParticles(ParticleTypes.POOF,
                    pos.x + ox, pos.y + level.random.nextDouble() * user.getBbHeight() / 2.0, pos.z + oz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 霜冻彗星 —— 结晶逆刃刀专属 SA。
 * 从 1.12.2 {@code HonKaiFrostComet} 移植并简化而来。
 * <p>
 * 对周围 5×1×5 范围内的所有生物造成伤害，并在目标脚下放置浮冰、清零其运动向量（定身）。
 */
public final class FrostCometArts extends SlashArts {

    private static final float BASE_DAMAGE = 1.0F;
    /** 攻击力补正系数（1.12.2 amplifierCalc(attack, 1)），对数补正。 */
    private static final float ATTACK_FACTOR = 1.0F;
    private static final double RANGE_XZ = 5.0;
    private static final double RANGE_Y = 1.0;

    public FrostCometArts(Function<LivingEntity, ResourceLocation> state) {
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

        Vec3 pos = user.position();
        AABB box = new AABB(
                pos.x - RANGE_XZ, pos.y - RANGE_Y, pos.z - RANGE_XZ,
                pos.x + RANGE_XZ, pos.y + RANGE_Y, pos.z + RANGE_XZ);

        DamageSource src = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        TargetSelector.AttackablePredicate attackable = new TargetSelector.AttackablePredicate();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user) && attackable.test(e));

        for (LivingEntity target : targets) {
            target.hurt(src, damage);
            // 脚下放浮冰（用 blockPosition 向下取整，负坐标也正确）
            BlockPos feet = target.blockPosition();
            level.setBlock(feet, Blocks.PACKED_ICE.defaultBlockState(), 3);
            level.setBlock(feet.above(), Blocks.PACKED_ICE.defaultBlockState(), 3);
            // 定身
            target.setDeltaMovement(0, 0, 0);
            target.hurtMarked = true;
        }

        if (!targets.isEmpty()) {
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z,
                30, RANGE_XZ / 2, RANGE_Y, RANGE_XZ / 2, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);

        return super.doArts(type, user);
    }
}

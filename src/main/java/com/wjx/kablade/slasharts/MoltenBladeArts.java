package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
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
 * 熔铁之刃 —— 热能切割刃 / 凰剑专属 SA。
 * 从 1.12.2 {@code HonKaiMoltenBlade} 移植并简化而来。
 * <p>
 * 对周围 3×1×3 范围内的所有生物造成伤害并点燃 5 秒。
 */
public final class MoltenBladeArts extends SlashArts {

    private static final float BASE_DAMAGE = 10.0F;
    private static final float ATTACK_FACTOR = 15.0F;
    private static final int FIRE_SECONDS = 5;
    private static final double RANGE_XZ = 3.0;
    private static final double RANGE_Y = 1.0;

    public MoltenBladeArts(Function<LivingEntity, ResourceLocation> state) {
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
        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        float damage = BASE_DAMAGE + extraDamage;

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
            target.setSecondsOnFire(FIRE_SECONDS);
        }

        if (!targets.isEmpty()) {
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.0, pos.z,
                20, RANGE_XZ / 2, RANGE_Y, RANGE_XZ / 2, 0.0);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 1.0F);

        return super.doArts(type, user);
    }
}

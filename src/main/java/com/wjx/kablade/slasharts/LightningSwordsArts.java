package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 龙一文字线 SA「雷剑裁决」。
 * <p>
 * 从 1.12.2 {@code LightningSwordsEx} 简化移植：
 * 锁定/视线目标被定身，并召唤 6 把金色召唤剑从空中落下追击目标。
 */
public final class LightningSwordsArts extends SlashArts {

    private static final float BASE_DAMAGE = 10.0F;
    private static final float ATTACK_FACTOR = 5.0F;
    private static final int SWORD_COUNT = 6;
    private static final int COLOR = 0xFFD700;
    private static final int LIFE_TIME = 40;

    public LightningSwordsArts(Function<LivingEntity, ResourceLocation> state) {
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

        Entity target = SATool.getEntityToWatch(user);
        if (!(target instanceof LivingEntity livingTarget) || !target.isAlive()) {
            return super.doArts(type, user);
        }

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.0F);

        // 定身
        livingTarget.setDeltaMovement(0.0, 0.0, 0.0);
        livingTarget.hasImpulse = true;

        double radius = 2.5;
        Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        for (int i = 0; i < SWORD_COUNT; i++) {
            double angle = i * (2.0 * Math.PI) / SWORD_COUNT;
            double sx = target.getX() + Math.sin(angle) * radius;
            double sz = target.getZ() + Math.cos(angle) * radius;
            double sy = target.getY() + 4.0 + level.random.nextDouble();

            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(
                    SlashBlade.RegistryEvents.SummonedSword, level);
            sword.setPos(sx, sy, sz);
            sword.setShooter(user);
            sword.setDamage(damage);
            sword.setColor(COLOR);
            sword.setHitEntity(livingTarget);

            Vec3 dir = targetCenter.subtract(sx, sy, sz).normalize();
            sword.shoot(dir.x, dir.y, dir.z, 1.25F, 0.0F);
            level.addFreshEntity(sword);

            level.sendParticles(ParticleTypes.WITCH, sx, sy, sz, 2, 0.0, 0.0, 0.0, 0.02);
        }

        return super.doArts(type, user);
    }
}

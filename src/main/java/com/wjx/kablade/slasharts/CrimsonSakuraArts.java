package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.CrimsonSakuraAttackEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleTypes;
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

public final class CrimsonSakuraArts extends SlashArts {

    private static final double RANGE = 6.0D;
    private static final double HIT_INFLATE_XZ = 3.0D;
    private static final double HIT_INFLATE_Y = 1.0D;
    private static final float BASE_DAMAGE = 50.0F;
    private static final float ATTACK_FACTOR = 12.0F;
    private static final float DAMAGE_MULTIPLIER = 1.2F;

    public CrimsonSakuraArts(Function<LivingEntity, ResourceLocation> state) {
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

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * DAMAGE_MULTIPLIER;

        CrimsonSakuraAttackEntity.spawn(level, user);
        damageForwardLine(level, user, damage, blade);
        spawnLavaRing(level, user);
        blade.getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(state -> state.updateComboSeq(user, ComboStateRegistry.VOID_SLASH.getId()));

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.1F, 1.55F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.85F);

        return ComboStateRegistry.VOID_SLASH.getId();
    }

    private static void damageForwardLine(ServerLevel level, LivingEntity user, float damage, ItemStack blade) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(HIT_INFLATE_XZ, HIT_INFLATE_Y, HIT_INFLATE_XZ);
        DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                target -> isAttackable(user, target) && intersectsLookLine(target, eye, end));
        boolean damagedBlade = false;
        for (LivingEntity target : targets) {
            if (user instanceof Player player) {
                player.crit(target);
            }
            target.invulnerableTime = 0;
            target.hurt(source, damage);
            target.setSecondsOnFire(4);
            if (!damagedBlade) {
                blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
                damagedBlade = true;
            }
        }
    }

    private static boolean intersectsLookLine(LivingEntity target, Vec3 eye, Vec3 end) {
        AABB box = target.getBoundingBox().inflate(target.getPickRadius());
        return box.contains(eye) || box.clip(eye, end).isPresent();
    }

    private static boolean isAttackable(LivingEntity user, LivingEntity target) {
        if (target == user || !target.isAlive() || target.isAlliedTo(user) || target instanceof Player) {
            return false;
        }
        return new TargetSelector.AttackablePredicate().test(target);
    }

    private static void spawnLavaRing(ServerLevel level, LivingEntity user) {
        double y = user.getY() + 1.75D;
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            level.sendParticles(ParticleTypes.LAVA,
                    user.getX() + Math.sin(angle) * 2.5D,
                    y,
                    user.getZ() + Math.cos(angle) * 2.5D,
                    1, 0.0D, -0.1D, 0.0D, 0.0D);
        }
    }
}

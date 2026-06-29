package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ZaizanEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/** Zaizan, the signature slash art for Nue. */
public final class ZaizanArts extends SlashArts {

    private static final float BASE_DAMAGE = 20.0F;
    private static final double AOE_RADIUS = 5.0;
    private static final int STRENGTH_DURATION = 140;
    private static final int STRENGTH_AMPLIFIER = 2;
    private static final int DELAYED_STRENGTH_TICKS = 12;
    private static final int VISUAL_LIFETIME = 32;
    private static final float VISUAL_SCALE = 1.12F;
    private static final double VISUAL_FORWARD = 1.55;
    private static final double VISUAL_UP = 1.16;
    private static final double LUNGE_SPEED = 0.92;

    public ZaizanArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        Vec3 look = SaFx.flatLook(user);

        lungeForward(user, look);
        spawnVisual(level, user, look);
        damageArea(level, user);
        SaFx.schedule(level, DELAYED_STRENGTH_TICKS,
                () -> user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        STRENGTH_DURATION, STRENGTH_AMPLIFIER, false, true)));

        return super.doArts(type, user);
    }

    private static void lungeForward(LivingEntity user, Vec3 look) {
        Vec3 motion = user.getDeltaMovement();
        user.setDeltaMovement(look.x * LUNGE_SPEED, motion.y, look.z * LUNGE_SPEED);
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    private static void spawnVisual(ServerLevel level, LivingEntity user, Vec3 look) {
        Vec3 origin = user.position().add(look.scale(VISUAL_FORWARD)).add(0.0, VISUAL_UP, 0.0);
        ZaizanEntity.spawn(level, user, VISUAL_FORWARD, VISUAL_UP, VISUAL_LIFETIME, VISUAL_SCALE);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.06F, 1.52F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 0.92F, 1.34F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.52F, 1.72F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.72F, 0.64F);
    }

    private static void damageArea(ServerLevel level, LivingEntity user) {
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = (float) MathFunc.amplifierCalc(bladeAttack, 20.0F);

        TargetSelector.AttackablePredicate attackable = new TargetSelector.AttackablePredicate();
        AABB box = user.getBoundingBox()
                .inflate(AOE_RADIUS, 1.0, AOE_RADIUS)
                .move(user.getDeltaMovement());
        List<Entity> entities = level.getEntities(user, box,
                e -> e instanceof LivingEntity && e != user && e.isAlive() && attackable.test((LivingEntity) e));

        for (Entity entity : entities) {
            LivingEntity target = (LivingEntity) entity;
            if (target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        100, STRENGTH_AMPLIFIER, false, true));
            } else {
                target.hurt(level.damageSources().mobAttack(user), BASE_DAMAGE + extraDamage);
            }
        }
    }
}

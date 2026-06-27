package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ShockImpactEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * Shock Impact - the Dawn Breaker Slash Art.
 * <p>
 * Keeps the 1.12.2 damage shape, but presents the release as a Honkai-style
 * cyan blade trail pulled out by the sword edge.
 */
public final class ShockImpactArts extends SlashArts {

    private static final float BASE_DAMAGE = 22.0F;
    private static final double AOE_RADIUS = 3.0;
    private static final int STRENGTH_DURATION = 100;
    private static final int STRENGTH_AMPLIFIER = 5;
    private static final int VISUAL_LIFETIME = 34;
    private static final float VISUAL_SCALE = 1.08F;
    private static final double LUNGE_SPEED = 1.15;
    // 刀光始终跟随玩家：在剑尖前方一点处生成，整段生命周期里贴着玩家移动，
    // 渲染器用 partialTick 取玩家插值位置摆放，避免一颤一颤。
    private static final double VISUAL_FORWARD = 1.1;
    private static final double VISUAL_UP = 1.08;

    public ShockImpactArts(Function<LivingEntity, ResourceLocation> state) {
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
        spawnImpactVisual(level, user, look);

        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = (float) MathFunc.amplifierCalc(bladeAttack, 10.0F);

        AABB box = user.getBoundingBox()
                .inflate(AOE_RADIUS, 1.0, AOE_RADIUS)
                .move(user.getDeltaMovement());
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive());
        for (LivingEntity target : enemies) {
            if (target instanceof Player) {
                continue;
            }
            target.hurt(level.damageSources().mobAttack(user), BASE_DAMAGE + extraDamage);
        }

        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                STRENGTH_DURATION, STRENGTH_AMPLIFIER, false, true));

        return super.doArts(type, user);
    }

    private static void lungeForward(LivingEntity user, Vec3 look) {
        Vec3 motion = user.getDeltaMovement();
        user.setDeltaMovement(look.x * LUNGE_SPEED, motion.y, look.z * LUNGE_SPEED);
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    private static void spawnImpactVisual(ServerLevel level, LivingEntity user, Vec3 look) {
        Vec3 origin = user.position().add(look.scale(VISUAL_FORWARD)).add(0.0, VISUAL_UP, 0.0);
        ShockImpactEntity.spawn(level, user, VISUAL_FORWARD, VISUAL_UP, VISUAL_LIFETIME, VISUAL_SCALE);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.1F, 1.72F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.78F, 1.52F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.86F, 1.26F);
    }
}

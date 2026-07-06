package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.FengxuanDimensionEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Fengxuan, ported from the 1.12.2 AL_Fengxuan slash art.
 */
public final class FengxuanArts extends SlashArts {

    private static final double RANGE = 5.0D;
    private static final float DAMAGE_RATIO = 3.0F;
    private static final int COLOR = 0xFFFFFF;

    public FengxuanArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 look = user.getLookAngle();
        Vec3 pos = resolveCutPosition(level, user, eye, look);

        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = bladeAttack / 3.4F + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO);

        FengxuanDimensionEntity.spawn(level, user, pos, damage, COLOR, 50);

        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z,
                32, 1.2D, 1.2D, 1.2D, 0.18D);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.35F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);

        return super.doArts(type, user);
    }

    private static Vec3 resolveCutPosition(ServerLevel level, LivingEntity user, Vec3 eye, Vec3 look) {
        Vec3 fallback = eye.add(look.scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(
                eye,
                fallback,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                user));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(user.position()) > 1.0D) {
            return hit.getLocation();
        }
        return fallback;
    }
}

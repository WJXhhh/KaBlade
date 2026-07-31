package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ThunderboltCallEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import com.wjx.kablade.util.SaTarget;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.function.Function;

/** 唤霆霓 / Thunderbolt Call. */
public final class ThunderboltCallArts extends SlashArts {

    private static final double TARGET_RANGE = 12.0D;
    private static final double VIRTUAL_TARGET_DISTANCE = 5.0D;
    private static final float BASE_DAMAGE = 50.0F;
    private static final float ATTACK_FACTOR = 12.0F;
    private static final float DAMAGE_MULTIPLIER = 5.04F;

    public ThunderboltCallArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        if (ThunderboltCallEntity.isCasting(user)) {
            return super.doArts(type, user);
        }

        ItemStack blade = user.getMainHandItem();
        SaTarget target = resolveTarget(level, user, blade);
        Vec3 launchDirection = normalizedLook(user);
        Vec3 targetAnchor = target == null
                ? user.getEyePosition().add(launchDirection.scale(VIRTUAL_TARGET_DISTANCE))
                : target.anchor();

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float totalDamage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR))
                * DAMAGE_MULTIPLIER;

        if (ThunderboltCallEntity.spawn(level, user,
                target == null ? null : target.hitEntity(), targetAnchor,
                launchDirection, totalDamage) == null) {
            return super.doArts(type, user);
        }

        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.82F, 1.82F);
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.78F, 0.72F);
        return super.doArts(type, user);
    }

    private static SaTarget resolveTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level)).orElse(null);
        return SaTargeting.findTarget(user, locked, TARGET_RANGE).orElse(null);
    }

    private static Vec3 normalizedLook(LivingEntity user) {
        Vec3 look = user.getLookAngle();
        return look.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
    }
}

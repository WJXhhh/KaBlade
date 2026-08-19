package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.TunaEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** Lethal Thrust, One Salty Tuna's 1.12.2 SA 456. */
public final class LethalThrustArts extends SlashArts {

    private static final double LOCK_RANGE = 100.0D;
    private static final float ATTACK_FACTOR = 5.0F;

    public LethalThrustArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        SaTarget target = resolveTarget(level, user, blade);
        Vec3 spawn = target == null
                ? user.position().add(user.getDeltaMovement())
                : target.anchor();

        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = MathFunc.amplifierCalc(baseAttack, ATTACK_FACTOR);

        TunaEntity.spawn(level, user, spawn.x, spawn.y, spawn.z, extraDamage);
        level.playSound(null, spawn.x, spawn.y, spawn.z,
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.9F, 0.85F);

        return super.doArts(type, user);
    }

    private static SaTarget resolveTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        return SaTargeting.findTarget(user, locked, LOCK_RANGE).orElse(null);
    }
}

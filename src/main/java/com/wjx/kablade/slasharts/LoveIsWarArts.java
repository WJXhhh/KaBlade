package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.RainUmbrellaEntity;
import com.wjx.kablade.util.SATool;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** Love is War, Pledge of Rain's 1.12.2 SA 457. */
public final class LoveIsWarArts extends SlashArts {

    private static final double LOCK_RANGE = 100.0D;

    public LoveIsWarArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        LivingEntity target = resolveTarget(level, user, blade);
        Vec3 spawn = target == null
                ? user.position().add(user.getDeltaMovement())
                : target.position();

        RainUmbrellaEntity.spawn(level, user, spawn.x, spawn.y, spawn.z);
        level.playSound(null, spawn.x, spawn.y, spawn.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 1.55F);
        return super.doArts(type, user);
    }

    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity living && isValidTarget(user, living)) {
            return living;
        }

        Entity watched = SATool.getEntityToWatch(user);
        if (watched instanceof LivingEntity living && isValidTarget(user, living)) {
            return living;
        }
        return null;
    }

    private static boolean isValidTarget(LivingEntity user, LivingEntity target) {
        return SaTargeting.canDamage(user, target)
                && target.distanceToSqr(user) <= LOCK_RANGE * LOCK_RANGE;
    }
}

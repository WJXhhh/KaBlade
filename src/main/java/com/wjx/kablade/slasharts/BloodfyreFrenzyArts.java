package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.BloodfyreFrenzyEntity;
import com.wjx.kablade.specialeffect.FuelTheRuin;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/** Ruinous Sakura's two-stage spinning slash and blood-flame finisher. */
public final class BloodfyreFrenzyArts extends SlashArts {

    private static final float BASE_DAMAGE = 54.0F;
    private static final float ATTACK_FACTOR = 12.0F;
    private static final float DAMAGE_MULTIPLIER = 2.5F;

    public BloodfyreFrenzyArts(Function<LivingEntity, ResourceLocation> state) {
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
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR))
                * DAMAGE_MULTIPLIER;

        BloodfyreFrenzyEntity.spawn(level, user, damage);
        if (user instanceof net.minecraft.world.entity.player.Player player) {
            FuelTheRuin.trigger(player);
        }
        blade.hurtAndBreak(3, user, entity -> entity.broadcastBreakEvent(user.getUsedItemHand()));

        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.82F, 1.32F);
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.74F, 0.72F);
        return super.doArts(type, user);
    }
}

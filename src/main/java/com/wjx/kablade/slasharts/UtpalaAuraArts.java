package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.UtpalaAuraEntity;
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

/** Utpala Aura, Frozen Naraka's staged ice vortex slash art. */
public final class UtpalaAuraArts extends SlashArts {

    private static final float BASE_DAMAGE = 26.0F;
    private static final float ATTACK_FACTOR = 10.0F;

    public UtpalaAuraArts(Function<LivingEntity, ResourceLocation> state) {
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
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * 2.0F;

        UtpalaAuraEntity.spawn(level, user, damage);
        blade.hurtAndBreak(2, user, entity -> entity.broadcastBreakEvent(user.getUsedItemHand()));

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.95F, 0.74F);
        return super.doArts(type, user);
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.DeathGazeBeamEntity;
import com.wjx.kablade.init.ModComboStates;
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

/**
 * 灼热重斩 (Death Gaze) —— 天父大剑专属 SA。
 * <p>
 * 施法者以重剑重击之势，向前方直线轰击出一道极高温度与密度的紫色激光死光与崩坏能粒子束，
 * 持续贯穿沿途目标并造成连续爆破灼烧与击退。
 */
public final class DeathGazeArts extends SlashArts {

    private static final float BASE_DAMAGE = 16.0F;
    private static final float ATTACK_FACTOR = 4.5F;

    public DeathGazeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return ModComboStates.VALKYRIE_IMPACT.getId();
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(12.0F);
        float totalDamage = BASE_DAMAGE + (float) MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        // Spawn DeathGazeBeamEntity
        DeathGazeBeamEntity.spawn(level, user, totalDamage);

        // Heavy charging and energy release sounds
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.3F, 1.4F);
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.8F);
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);

        return ModComboStates.VALKYRIE_IMPACT.getId();
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.NuclearShockEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Nuclear Shock (核能震动) Slash Art.
 */
public final class NuclearShockArts extends SlashArts {

    private static final float BASE_DAMAGE = 17.0F;
    private static final float ATTACK_FACTOR = 6.0F;
    private static final double IMPACT_DISTANCE = 1.65D;

    public NuclearShockArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return ModComboStates.FUSION_NUCLEAR_SHOCK.getId();
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float totalDamage = BASE_DAMAGE + (float) MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        // Calculate impact position in front of the caster
        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1e-4) {
            horizontal = new Vec3(1.0, 0.0, 0.0);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 impactPos = user.position().add(horizontal.scale(IMPACT_DISTANCE));

        // Spawn timeline entity
        NuclearShockEntity.spawn(level, user, impactPos, totalDamage);

        // Play initial sweep sound
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.85F);

        return ModComboStates.FUSION_NUCLEAR_SHOCK.getId();
    }
}

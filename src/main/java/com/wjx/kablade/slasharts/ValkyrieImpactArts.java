package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ValkyrieImpactEntity;
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
 * Valkyrie Impact (女武神冲击) Slash Art.
 * Super Heavy Greatsword Charge weapon skill:
 * High windup, violent downward slam triggering instant ground impact burst and forward advancing fissure wave.
 */
public final class ValkyrieImpactArts extends SlashArts {

    private static final float BASE_DAMAGE = 13.5F;
    private static final float ATTACK_FACTOR = 4.875F;
    private static final double IMPACT_DISTANCE = 1.25D;

    public ValkyrieImpactArts(Function<LivingEntity, ResourceLocation> state) {
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
                .orElse(5.0F);
        float totalDamage = BASE_DAMAGE + (float) MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        // Calculate impact position in front of caster along horizontal look direction
        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1e-4) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 impactPos = user.position().add(horizontal.scale(IMPACT_DISTANCE));

        // Spawn timeline entity
        ValkyrieImpactEntity.spawn(level, user, impactPos, user.getYRot(), totalDamage);

        // Play initial high-windup energy sound
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.3F, 0.75F);
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);

        return ModComboStates.VALKYRIE_IMPACT.getId();
    }
}

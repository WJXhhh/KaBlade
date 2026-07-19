package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ConceptualMetaphorEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import com.wjx.kablade.util.SaTargeting;
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

/** Conceptual Metaphor, Domain of Unity's purple-white conceptual blade field. */
public final class ConceptualMetaphorArts extends SlashArts {

    private static final float BASE_DAMAGE = 20.0F;
    private static final float ATTACK_FACTOR = 5.0F;
    private static final float DAMAGE_MULTIPLIER = 2.6F;
    private static final double LOCK_RANGE = 100.0D;
    private static final double SIGHT_RANGE = 8.0D;

    public ConceptualMetaphorArts(Function<LivingEntity, ResourceLocation> state) {
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
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * DAMAGE_MULTIPLIER;

        Vec3 attackPosition = resolveAttackPosition(level, user);
        ConceptualMetaphorEntity.spawn(level, user, damage, attackPosition);
        blade.hurtAndBreak(3, user, entity -> entity.broadcastBreakEvent(user.getUsedItemHand()));

        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.82F, 1.78F);
        return super.doArts(type, user);
    }

    private static Vec3 resolveAttackPosition(ServerLevel level, LivingEntity user) {
        Entity locked = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity target
                && SaTargeting.canDamage(user, target)
                && target.distanceToSqr(user) <= LOCK_RANGE * LOCK_RANGE) {
            return target.position();
        }

        LivingEntity inSight = SATool.getEntityInSight(user, SIGHT_RANGE);
        if (inSight != null) {
            return inSight.position();
        }

        return user.position();
    }
}

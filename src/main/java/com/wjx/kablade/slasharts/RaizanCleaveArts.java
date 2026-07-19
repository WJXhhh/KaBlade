package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.RaizanCleaveEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.function.Function;

/** Domain of Sanction's two-stage floating-weapon slash art. */
public final class RaizanCleaveArts extends SlashArts {

    private static final double TARGET_RANGE = 8.0D;
    private static final double VIRTUAL_TARGET_DISTANCE = 4.0D;
    private static final float BASE_DAMAGE = 50.0F;
    private static final float ATTACK_FACTOR = 12.0F;
    private static final float THUNDER_EDGE_MULTIPLIER = 1.4F;
    private static final float RAIZAN_MULTIPLIER = 4.0F;

    public RaizanCleaveArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        if (RaizanCleaveEntity.isCasting(user)) {
            return super.doArts(type, user);
        }

        ItemStack blade = user.getMainHandItem();
        LivingEntity target = resolveTarget(level, user, blade);
        Vec3 origin = user.position();
        Vec3 flatForward = target == null
                ? flatLook(user)
                : flatten(target.position().subtract(origin), flatLook(user));
        Vec3 targetAnchor = target == null
                ? origin.add(flatForward.scale(VIRTUAL_TARGET_DISTANCE))
                        .add(0.0D, user.getBbHeight() * 0.52D, 0.0D)
                : target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
        float yaw = (float) (Mth.atan2(-flatForward.x, flatForward.z) * Mth.RAD_TO_DEG);

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float totalDamage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR))
                * THUNDER_EDGE_MULTIPLIER * RAIZAN_MULTIPLIER;

        if (RaizanCleaveEntity.spawn(level, user, origin, targetAnchor, yaw, totalDamage) == null) {
            return super.doArts(type, user);
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.82F, 1.72F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 0.72F, 1.34F);
        return super.doArts(type, user);
    }

    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level)).orElse(null);
        if (locked instanceof LivingEntity living && validTarget(user, living)) {
            return living;
        }

        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(TARGET_RANGE));
        AABB rayArea = user.getBoundingBox().expandTowards(look.scale(TARGET_RANGE)).inflate(1.0D);
        return level.getEntitiesOfClass(LivingEntity.class, rayArea,
                        target -> validTarget(user, target)
                                && target.getBoundingBox().inflate(target.getPickRadius() + 0.25D)
                                .clip(eye, end).isPresent())
                .stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(user)))
                .orElse(null);
    }

    private static boolean validTarget(LivingEntity user, LivingEntity target) {
        return target.distanceToSqr(user) <= TARGET_RANGE * TARGET_RANGE
                && target.isPickable()
                && SaTargeting.canDamageAttackable(user, target);
    }

    private static Vec3 flatLook(LivingEntity user) {
        return flatten(user.getLookAngle(), new Vec3(0.0D, 0.0D, 1.0D));
    }

    private static Vec3 flatten(Vec3 direction, Vec3 fallback) {
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        return flat.lengthSqr() < 1.0E-6D ? fallback : flat.normalize();
    }
}

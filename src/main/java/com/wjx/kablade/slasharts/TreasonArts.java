package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.TreasonMissileEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTarget;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** 背叛者斩击：六枚持续追踪的能量飞弹。 */
public final class TreasonArts extends SlashArts {
    private static final float DAMAGE_MULTIPLIER = 1.5F;

    public TreasonArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (!user.level().isClientSide() && type != ArtsType.Fail) {
            var state = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE);
            Entity locked = state.map(value -> value.getTargetEntity(user.level())).orElse(null);
            SaTarget target = SaTargeting.findTarget(user, locked, 32.0D).orElse(null);
            float damage = MathFunc.amplifierCalc(
                    state.map(ISlashBladeState::getBaseAttackModifier).orElse(4.0F), 1.0F) * DAMAGE_MULTIPLIER;
            Vec3 forward = SaFx.flatLook(user);
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            for (int i = 0; i < 6; i++) {
                double side = i % 2 == 0 ? -1.0D : 1.0D;
                int row = i / 2;
                Vec3 position = user.getEyePosition().add(forward.scale(0.6D))
                        .add(right.scale(side * (0.55D + row * 0.3D)))
                        .add(0, row * 0.35D, 0);
                Vec3 direction = user.getLookAngle().add(right.scale(side * 0.35D))
                        .add(0, row * 0.12D, 0).normalize();
                TreasonMissileEntity.spawn(user, target, position, direction, damage);
            }
            user.level().playSound(null, user.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        return super.doArts(type, user);
    }
}

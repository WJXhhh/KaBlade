package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.entity.ExSlashDriveEntity;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 御气 —— 白牙「御气」专属 SA（复刻 1.12.2 {@code AL_Yuqi}）。
 * 朝玩家面向发射一道白色气劲飞刃，随后重置连段。
 */
public final class YuqiArts extends SlashArts {

    private static final float DAMAGE_BASE = 1.0F;
    private static final float DAMAGE_RATIO = 10.0F;
    private static final float INITIAL_SPEED = 0.008F;
    private static final int LIFETIME = 80;
    private static final int WHITE = 0xFFFFFF;

    public YuqiArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        final ServerLevel level = (ServerLevel) user.level();

        final float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier).orElse(4.0F);
        final float damage = DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO);

        final Vec3 eye = user.getEyePosition(1.0F);
        final Vec3 look = user.getLookAngle();

        ExSlashDriveEntity.spawn(level, user, eye, look, damage, WHITE, LIFETIME, 0.0F, false)
                .setInitialSpeed(INITIAL_SPEED);

        return super.doArts(type, user);
    }
}

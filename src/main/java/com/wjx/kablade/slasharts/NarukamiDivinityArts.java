package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.NarukamiDivinityEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import com.wjx.kablade.util.SaTarget;
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

/** 鸣雷神 / Narukami Divinity, exclusive Slash Art of Narukami. */
public final class NarukamiDivinityArts extends SlashArts {

    private static final double TARGET_RANGE = 14.0D;
    private static final double VIRTUAL_TARGET_DISTANCE = 5.5D;
    private static final float BASE_DAMAGE = 54.0F;
    private static final float ATTACK_FACTOR = 14.0F;
    private static final float DAMAGE_MULTIPLIER = 8.40F;

    public NarukamiDivinityArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        if (NarukamiDivinityEntity.isCasting(user)) {
            return super.doArts(type, user);
        }

        ItemStack blade = user.getMainHandItem();
        SaTarget target = resolveTarget(level, user, blade);
        Vec3 direction = horizontalLook(user);
        Vec3 targetAnchor = target == null
                ? user.position().add(0.0D, 1.25D, 0.0D)
                .add(direction.scale(VIRTUAL_TARGET_DISTANCE))
                : target.anchor();

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float totalDamage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR))
                * DAMAGE_MULTIPLIER;

        if (NarukamiDivinityEntity.spawn(level, user,
                target == null ? null : target.hitEntity(), targetAnchor,
                direction, totalDamage) == null) {
            return super.doArts(type, user);
        }

        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.88F, 1.54F);
        level.playSound(null, user.getX(), user.getY() + 1.0D, user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.78F, 0.62F);
        return super.doArts(type, user);
    }

    private static SaTarget resolveTarget(ServerLevel level, LivingEntity user, ItemStack blade) {
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level)).orElse(null);
        return SaTargeting.findTarget(user, locked, TARGET_RANGE).orElse(null);
    }

    private static Vec3 horizontalLook(LivingEntity user) {
        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-8D) {
            float yaw = user.getYRot() * ((float) Math.PI / 180.0F);
            horizontal = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        return horizontal.normalize();
    }
}

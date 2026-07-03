package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.VorpalBlackHoleEntity;
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
 * Vorpal Hole - the "Anti-Force Field Katana Type 11" Slash Art.
 * <p>
 * The singularity pulls enemies inward, performs one 80% attack cut, then
 * releases several 20% attack energy pulses.
 */
public final class VorpalHoleArts extends SlashArts {

    private static final float DAMAGE_MULTIPLIER = 1.5F;
    private static final float OPENING_DAMAGE_RATIO = 0.8F;
    private static final float PULSE_DAMAGE_RATIO = 0.2F;
    private static final int LIFETIME = 56;

    public VorpalHoleArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        float bladeAttack = bladeAttack(user);
        Vec3 look = SaFx.flatLook(user);
        Vec3 origin = user.position().add(look.scale(2.35)).add(0.0, 1.35, 0.0);
        float openingDamage = bladeAttack * OPENING_DAMAGE_RATIO * DAMAGE_MULTIPLIER;
        float pulseDamage = bladeAttack * PULSE_DAMAGE_RATIO * DAMAGE_MULTIPLIER;

        VorpalBlackHoleEntity.spawn(level, user, origin, LIFETIME,
                openingDamage,
                pulseDamage);
        openingFx(level, origin);
        spawnCutLines(level, user, origin, look, openingDamage);

        return super.doArts(type, user);
    }

    private static float bladeAttack(LivingEntity user) {
        ItemStack stack = user.getMainHandItem();
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
    }

    private static void openingFx(ServerLevel level, Vec3 origin) {
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.15F, 0.72F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.95F, 0.42F);
    }

    private static void spawnCutLines(ServerLevel level, LivingEntity user, Vec3 origin,
                                      Vec3 look, float openingDamage) {
        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        Vec3 base = origin.add(0.0, -0.15, 0.0);
        for (int i = 0; i < 3; i++) {
            final int index = i;
            SaFx.schedule(level, 14 + i * 5, () -> {
                Vec3 dir = side.scale(index == 1 ? -1.0 : 1.0)
                        .add(look.scale(0.18)).normalize();
                Vec3 pos = base.add(look.scale(0.35 * index))
                        .add(0.0, (index - 1) * 0.18, 0.0);
                SaFx.drive(level, user, pos, dir, 0.72F, openingDamage * 0.28F,
                        0xFF1E24, 1.65F + index * 0.18F, 11.0F,
                        index == 1 ? -24.0F : 18.0F);
            });
        }
    }
}

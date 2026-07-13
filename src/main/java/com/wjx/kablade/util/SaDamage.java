package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Damage helpers for Kablade SA/SE multi-hit timing. */
public final class SaDamage {

    /** Dedicated datapack damage type tagged with {@code minecraft:bypasses_armor}. */
    public static final ResourceKey<DamageType> SLASH_ART = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Main.MODID, "slash_art"));

    private SaDamage() {
    }

    public static boolean hurtNoIFrame(LivingEntity target, DamageSource source, float amount) {
        clearInvulnerability(target);
        boolean hurt = target.hurt(source, amount);
        target.invulnerableTime = 0;
        return hurt;
    }

    /**
     * Apply Kablade's direct slash-art damage. The owner remains the indirect attacker for
     * kill credit, while {@code directEntity} identifies a persistent area/effect entity.
     * Damage from an ownerless delayed entity is intentionally cancelled.
     */
    public static boolean hurtSlashArtNoIFrame(LivingEntity target, ServerLevel level,
                                                Entity directEntity, LivingEntity owner, float amount) {
        if (owner == null) {
            return false;
        }
        DamageSource source = new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SLASH_ART),
                directEntity, owner);
        return hurtNoIFrame(target, source, amount);
    }

    /** Apply direct, owner-originated slash-art damage. */
    public static boolean hurtSlashArtNoIFrame(LivingEntity target, ServerLevel level,
                                                LivingEntity owner, float amount) {
        return hurtSlashArtNoIFrame(target, level, owner, owner, amount);
    }

    public static void clearInvulnerability(LivingEntity target) {
        target.hurtTime = 0;
        target.invulnerableTime = 0;
    }
}
